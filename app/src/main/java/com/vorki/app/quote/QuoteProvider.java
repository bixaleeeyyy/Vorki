package com.vorki.app.quote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.vorki.app.R;
import com.vorki.app.core.FontStyles;
import com.vorki.app.core.RvUtil;
import com.vorki.app.core.Settings;
import com.vorki.app.core.VorkiProvider;
import com.vorki.app.core.WidgetBg;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class QuoteProvider extends VorkiProvider {

    @Override
    protected void update(Context c, AppWidgetManager m, int id) {
        m.updateAppWidget(id, render(c, id, Settings.loadForWidget(c, id), false));
    }

    /** Built-in quotes ("text | author") and/or the user's own lines. */
    private static List<String> pool(Context c, Settings s) {
        int mode = s.getInt("qmode", 0); // 0 built-in, 1 mine, 2 both
        List<String> list = new ArrayList<>();
        if (mode != 1) {
            for (String q : c.getResources().getStringArray(R.array.quotes)) {
                list.add(q);
            }
        }
        if (mode != 0) {
            for (String line : s.getString("own", "").split("\n")) {
                if (!line.trim().isEmpty()) {
                    list.add(line.trim());
                }
            }
        }
        if (list.isEmpty()) {
            list.add(c.getString(R.string.quote_fallback));
        }
        return list;
    }

    public static RemoteViews render(Context c, int widgetId, Settings s, boolean preview) {
        int text = s.getInt("text", 0xFFFFFFFF);
        int accent = s.getInt("accent", 0xFFFFB020);
        int scale = s.getInt("scale", 100);
        int font = FontStyles.index(s);

        List<String> pool = pool(c, s);
        int index = (int) Math.floorMod(LocalDate.now().toEpochDay() + widgetId, (long) pool.size());
        String line = pool.get(index);
        String quote = line;
        String author = "";
        int bar = line.lastIndexOf('|');
        if (bar > 0) {
            quote = line.substring(0, bar).trim();
            author = line.substring(bar + 1).trim();
        }

        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_quote);
        WidgetBg.apply(c, rv, s, R.id.q_root, 0xFF0B0D17, 75);
        rv.setTextColor(R.id.q_mark, accent);
        FontStyles.text(rv, R.id.q_text, quote, font);
        rv.setTextColor(R.id.q_text, text);
        RvUtil.textSize(rv, R.id.q_text, 18, scale);
        if (author.isEmpty() || !s.getBool("showAuthor", true)) {
            rv.setViewVisibility(R.id.q_author, View.GONE);
        } else {
            rv.setViewVisibility(R.id.q_author, View.VISIBLE);
            FontStyles.text(rv, R.id.q_author, "\u2014 " + author, font);
            rv.setTextColor(R.id.q_author, RvUtil.faded(text));
            RvUtil.textSize(rv, R.id.q_author, 12, scale);
        }
        if (!preview) {
            Intent edit = new Intent(c, QuoteActivity.class);
            edit.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            edit.setData(Uri.parse("vorki://quote/" + widgetId));
            rv.setOnClickPendingIntent(R.id.q_root, PendingIntent.getActivity(c, widgetId, edit,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        return rv;
    }
}
