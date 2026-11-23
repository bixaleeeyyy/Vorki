package com.vorki.app.photo;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.vorki.app.R;
import com.vorki.app.core.BgImages;
import com.vorki.app.core.RvUtil;
import com.vorki.app.core.Settings;
import com.vorki.app.core.Ui;
import com.vorki.app.core.VorkiProvider;

public class PhotoProvider extends VorkiProvider {

    static final String ACTION_NEXT = "com.vorki.app.action.NEXT_PHOTO";
    private static final String RUNTIME = "vorki_runtime";

    private static final long[] INTERVALS = {
            60L * 60 * 1000, 6L * 60 * 60 * 1000, 24L * 60 * 60 * 1000, 0L
    };

    @Override
    protected void update(Context c, AppWidgetManager m, int id) {
        m.updateAppWidget(id, render(c, id, Settings.loadForWidget(c, id), false));
    }

    @Override
    protected long nextRefreshAt(Context c, int[] ids) {
        long soonest = 0;
        long now = System.currentTimeMillis();
        for (int id : ids) {
            Settings s = Settings.loadForWidget(c, id);
            long every = INTERVALS[Math.max(0, Math.min(3, s.getInt("pint", 1)))];
            if (every > 0 && names(s).length > 1) {
                long next = (now / every + 1) * every + 1000;
                soonest = soonest == 0 ? next : Math.min(soonest, next);
            }
        }
        return soonest;
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        if (ACTION_NEXT.equals(intent.getAction())) {
            int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
            if (id > 0) {
                SharedPreferences p = c.getSharedPreferences(RUNTIME, Context.MODE_PRIVATE);
                p.edit().putInt("photo_" + id, p.getInt("photo_" + id, 0) + 1).apply();
                AppWidgetManager m = AppWidgetManager.getInstance(c);
                m.updateAppWidget(id, render(c, id, Settings.loadForWidget(c, id), false));
            }
            return;
        }
        super.onReceive(c, intent);
    }

    @Override
    public void onDeleted(Context c, int[] ids) {
        super.onDeleted(c, ids);
        SharedPreferences.Editor e = c.getSharedPreferences(RUNTIME, Context.MODE_PRIVATE).edit();
        for (int id : ids) {
            e.remove("photo_" + id);
        }
        e.apply();
    }

    public static String[] names(Settings s) {
        String raw = s.getString("pics", "");
        return raw.isEmpty() ? new String[0] : raw.split(",");
    }

    public static RemoteViews render(Context c, int widgetId, Settings s, boolean preview) {
        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_photo);
        String[] names = names(s);
        int shape = s.getInt("pshape", 0);
        int border = s.getInt("pborder", 0);
        int shapeRes = shape == 1 ? R.drawable.square_bg : (shape == 2 ? R.drawable.today_bg : R.drawable.widget_bg);

        rv.setInt(R.id.ph_image, "setBackgroundResource", shapeRes);
        if (border > 0) {
            RvUtil.tintBackground(rv, R.id.ph_root, shapeRes, s.getInt("pbcolor", 0xFFFFFFFF));
            int px = Ui.dp(c, border);
            rv.setViewPadding(R.id.ph_root, px, px, px, px);
        } else {
            RvUtil.clearBackground(rv, R.id.ph_root);
            rv.setViewPadding(R.id.ph_root, 0, 0, 0, 0);
        }

        Bitmap bmp = null;
        if (names.length > 0) {
            int offset = c.getSharedPreferences(RUNTIME, Context.MODE_PRIVATE).getInt("photo_" + widgetId, 0);
            long every = INTERVALS[Math.max(0, Math.min(3, s.getInt("pint", 1)))];
            long base = every > 0 && names.length > 1 ? System.currentTimeMillis() / every : 0;
            int idx = (int) Math.floorMod(base + offset, (long) names.length);
            bmp = BgImages.load(c, names[idx]);
        }
        if (bmp != null) {
            rv.setImageViewBitmap(R.id.ph_image, bmp);
            rv.setViewVisibility(R.id.ph_hint, View.GONE);
        } else {
            rv.setImageViewResource(R.id.ph_image, 0);
            rv.setViewVisibility(R.id.ph_hint, View.VISIBLE);
            rv.setInt(R.id.ph_image, "setBackgroundColor", 0x22888888);
        }

        if (!preview) {
            PendingIntent pending;
            if (names.length > 1) {
                Intent next = new Intent(c, PhotoProvider.class).setAction(ACTION_NEXT);
                next.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                next.setData(Uri.parse("vorki://photo/" + widgetId));
                pending = PendingIntent.getBroadcast(c, 0, next,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            } else {
                Intent edit = new Intent(c, PhotoActivity.class);
                edit.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                edit.setData(Uri.parse("vorki://photo-edit/" + widgetId));
                pending = PendingIntent.getActivity(c, widgetId, edit,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            }
            rv.setOnClickPendingIntent(R.id.ph_root, pending);
        }
        return rv;
    }
}
