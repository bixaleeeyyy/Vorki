package com.vorki.app.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.RemoteViews;

import com.vorki.app.R;

/** Paints a widget's background: a colour with opacity, or a picture from the gallery. */
public final class WidgetBg {

    private WidgetBg() {
    }

    public static final String K_BG = "bg";
    public static final String K_OPACITY = "opacity";
    public static final String K_PIC = "pic";
    public static final String K_DIM = "dim";

    public static void apply(Context c, RemoteViews rv, Settings s, int rootId,
                             int defBg, int defOpacity) {
        RvUtil.tintBackground(rv, rootId, R.drawable.widget_bg,
                RvUtil.withOpacity(s.getInt(K_BG, defBg), s.getInt(K_OPACITY, defOpacity)));
        applyPicture(c, rv, s);
    }

    /** Only the picture part (the picture, dimmed). Returns true if a picture is shown. */
    public static boolean applyPicture(Context c, RemoteViews rv, Settings s) {
        Bitmap bmp = BgImages.load(c, s.getString(K_PIC, ""));
        if (bmp != null) {
            rv.setViewVisibility(R.id.bg_image, View.VISIBLE);
            rv.setImageViewBitmap(R.id.bg_image, bmp);
            int dim = Math.max(0, Math.min(80, s.getInt(K_DIM, 30)));
            rv.setInt(R.id.bg_image, "setColorFilter", (dim * 255 / 100) << 24);
            return true;
        }
        rv.setViewVisibility(R.id.bg_image, View.GONE);
        return false;
    }

    public static boolean hasPicture(Settings s) {
        return !s.getString(K_PIC, "").isEmpty();
    }
}
