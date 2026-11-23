package com.vorki.app.transparent;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.vorki.app.R;
import com.vorki.app.core.RvUtil;
import com.vorki.app.core.Settings;
import com.vorki.app.core.WidgetBg;

public final class TransparentRenderer {

    private TransparentRenderer() {
    }

    public static RemoteViews render(Context context, int widgetId, Settings s, boolean preview) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_transparent);

        boolean outline = s.getBool("outline", true);
        int color = s.getInt("outlineColor", 0xB3FFFFFF);
        if (outline) {
            RvUtil.tintBackground(rv, R.id.tp_root, R.drawable.outline_bg, color);
            rv.setTextColor(R.id.tp_hint, color);
            rv.setViewVisibility(R.id.tp_hint, View.VISIBLE);
        } else {
            RvUtil.clearBackground(rv, R.id.tp_root);
            rv.setViewVisibility(R.id.tp_hint, View.GONE);
        }
        // A picture makes the widget visible on purpose.
        WidgetBg.applyPicture(context, rv, s);

        if (!preview) {
            // Every tap opens an invisible helper screen that counts the taps (see TapActivity).
            Intent tap = new Intent(context, TapActivity.class);
            tap.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            tap.setData(Uri.parse("vorki://tap/" + widgetId));
            PendingIntent pending = PendingIntent.getActivity(context, widgetId, tap,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.tp_root, pending);
        }
        return rv;
    }
}
