package com.vorki.app.apps;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.RemoteViews;

import com.vorki.app.R;
import com.vorki.app.core.FontStyles;
import com.vorki.app.core.RvUtil;
import com.vorki.app.core.Settings;
import com.vorki.app.core.VorkiProvider;
import com.vorki.app.core.WidgetBg;

public class AppsProvider extends VorkiProvider {

    public static final int MAX = 6;

    private static final int[] SLOTS = {R.id.ap_slot0, R.id.ap_slot1, R.id.ap_slot2, R.id.ap_slot3, R.id.ap_slot4, R.id.ap_slot5};
    private static final int[] ICONS = {R.id.ap_icon0, R.id.ap_icon1, R.id.ap_icon2, R.id.ap_icon3, R.id.ap_icon4, R.id.ap_icon5};
    private static final int[] LABELS = {R.id.ap_label0, R.id.ap_label1, R.id.ap_label2, R.id.ap_label3, R.id.ap_label4, R.id.ap_label5};

    @Override
    protected void update(Context c, AppWidgetManager m, int id) {
        m.updateAppWidget(id, render(c, id, Settings.loadForWidget(c, id), false));
    }

    @Override
    protected boolean wantsTimedRefresh() {
        return false;
    }

    private static Bitmap iconOf(Context c, ComponentName cn) {
        try {
            Drawable d = c.getPackageManager().getActivityIcon(cn);
            int size = Math.round(96 * c.getResources().getDisplayMetrics().density / 2f);
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            d.setBounds(0, 0, size, size);
            d.draw(canvas);
            return bmp;
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return null;
        }
    }

    public static RemoteViews render(Context c, int widgetId, Settings s, boolean preview) {
        int text = s.getInt("text", 0xFFFFFFFF);
        int scale = s.getInt("scale", 100);
        int font = FontStyles.index(s);
        boolean labels = s.getBool("labels", true);

        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_apps);
        WidgetBg.apply(c, rv, s, R.id.ap_root, 0xFF0B0D17, 75);

        int shown = 0;
        for (int i = 0; i < MAX; i++) {
            String comp = s.getString("a" + i, "");
            ComponentName cn = comp.isEmpty() ? null : ComponentName.unflattenFromString(comp);
            Bitmap icon = cn == null ? null : iconOf(c, cn);
            if (icon == null) {
                rv.setViewVisibility(SLOTS[i], View.GONE);
                continue;
            }
            shown++;
            rv.setViewVisibility(SLOTS[i], View.VISIBLE);
            rv.setImageViewBitmap(ICONS[i], icon);
            if (labels) {
                rv.setViewVisibility(LABELS[i], View.VISIBLE);
                FontStyles.text(rv, LABELS[i], s.getString("a" + i + "l", ""), font);
                rv.setTextColor(LABELS[i], text);
                RvUtil.textSize(rv, LABELS[i], 11, scale);
            } else {
                rv.setViewVisibility(LABELS[i], View.GONE);
            }
            if (!preview) {
                Intent launch = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(cn)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                rv.setOnClickPendingIntent(SLOTS[i], PendingIntent.getActivity(c, widgetId * 10 + i, launch,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            }
        }
        if (shown == 0) {
            rv.setViewVisibility(SLOTS[0], View.VISIBLE);
            rv.setImageViewResource(ICONS[0], R.drawable.ic_apps);
            rv.setInt(ICONS[0], "setColorFilter", text);
            rv.setViewVisibility(LABELS[0], View.VISIBLE);
            rv.setTextViewText(LABELS[0], c.getString(R.string.apps_empty));
            rv.setTextColor(LABELS[0], text);
            if (!preview) {
                Intent edit = new Intent(c, AppsActivity.class);
                edit.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                edit.setData(android.net.Uri.parse("vorki://apps/" + widgetId));
                rv.setOnClickPendingIntent(SLOTS[0], PendingIntent.getActivity(c, widgetId, edit,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            }
        }
        return rv;
    }
}
