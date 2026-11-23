package com.vorki.app.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.LruCache;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pictures chosen from the gallery. Each one is copied (and made smaller) into the app's own
 * storage, so a widget keeps working even if the original photo is moved or deleted.
 */
public final class BgImages {

    private BgImages() {
    }

    public static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(6);

    private static File dir(Context c) {
        File d = new File(c.getApplicationContext().getFilesDir(), "pics");
        if (!d.exists()) {
            //noinspection ResultOfMethodCallIgnored
            d.mkdirs();
        }
        return d;
    }

    /** Copies one picture into the app. Returns its file name, or null if it failed. */
    public static String importOne(Context c, Uri uri, int maxPx) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bmp;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                bmp = BitmapFactory.decodeStream(in, null, opts);
            }
            if (bmp == null) {
                return null;
            }
            int rotation = 0;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                if (in != null) {
                    int o = new ExifInterface(in).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    if (o == ExifInterface.ORIENTATION_ROTATE_90) {
                        rotation = 90;
                    } else if (o == ExifInterface.ORIENTATION_ROTATE_180) {
                        rotation = 180;
                    } else if (o == ExifInterface.ORIENTATION_ROTATE_270) {
                        rotation = 270;
                    }
                }
            } catch (Exception ignored) {
            }
            float scale = Math.min(1f, maxPx / (float) Math.max(bmp.getWidth(), bmp.getHeight()));
            if (rotation != 0 || scale < 1f) {
                Matrix m = new Matrix();
                m.postScale(scale, scale);
                m.postRotate(rotation);
                Bitmap out = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                if (out != bmp) {
                    bmp.recycle();
                    bmp = out;
                }
            }
            String name = UUID.randomUUID().toString().substring(0, 12) + ".jpg";
            try (FileOutputStream os = new FileOutputStream(new File(dir(c), name))) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 88, os);
            }
            bmp.recycle();
            return name;
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    /** Loads a saved picture (cached). Returns null if it is missing. */
    public static Bitmap load(Context c, String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        Bitmap cached = CACHE.get(name);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }
        try {
            Bitmap bmp = BitmapFactory.decodeFile(new File(dir(c), name).getAbsolutePath());
            if (bmp != null) {
                CACHE.put(name, bmp);
            }
            return bmp;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    /** Deletes pictures that no widget uses any more (ignores very recent ones). */
    public static void cleanup(Context c) {
        try {
            SharedPreferences prefs = c.getApplicationContext()
                    .getSharedPreferences("vorki_widgets", Context.MODE_PRIVATE);
            StringBuilder all = new StringBuilder();
            for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
                all.append(e.getValue()).append('\n');
            }
            String used = all.toString();
            File[] files = dir(c).listFiles();
            if (files == null) {
                return;
            }
            long now = System.currentTimeMillis();
            for (File f : files) {
                if (!used.contains(f.getName()) && now - f.lastModified() > 2 * 60 * 1000L) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                    CACHE.remove(f.getName());
                }
            }
        } catch (Exception ignored) {
        }
    }
}
