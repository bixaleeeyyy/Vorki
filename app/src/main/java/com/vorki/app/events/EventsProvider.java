package com.vorki.app.events;

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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class EventsProvider extends VorkiProvider {

    public static final int MAX = 6;

    private static final int[] ROWS = {R.id.ev_row0, R.id.ev_row1, R.id.ev_row2, R.id.ev_row3, R.id.ev_row4, R.id.ev_row5};
    private static final int[] NAMES = {R.id.ev_name0, R.id.ev_name1, R.id.ev_name2, R.id.ev_name3, R.id.ev_name4, R.id.ev_name5};
    private static final int[] WHEN = {R.id.ev_when0, R.id.ev_when1, R.id.ev_when2, R.id.ev_when3, R.id.ev_when4, R.id.ev_when5};

    private static final class Item {
        String name;
        LocalDate date;
    }

    @Override
    protected void update(Context c, AppWidgetManager m, int id) {
        m.updateAppWidget(id, render(c, id, Settings.loadForWidget(c, id), false));
    }

    public static LocalDate dateOf(Settings s, int i) {
        if (s.getInt("e" + i + "y", 0) <= 0) {
            return null;
        }
        try {
            return LocalDate.of(s.getInt("e" + i + "y", 2026), s.getInt("e" + i + "m", 1), s.getInt("e" + i + "d", 1));
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static void setEvent(Settings s, int i, String name, LocalDate d) {
        s.put("e" + i + "n", name);
        s.put("e" + i + "y", d == null ? 0 : d.getYear());
        s.put("e" + i + "m", d == null ? 0 : d.getMonthValue());
        s.put("e" + i + "d", d == null ? 0 : d.getDayOfMonth());
    }

    public static RemoteViews render(Context c, int widgetId, Settings s, boolean preview) {
        int text = s.getInt("text", 0xFFFFFFFF);
        int accent = s.getInt("accent", 0xFFFF5C8A);
        int scale = s.getInt("scale", 100);
        int font = FontStyles.index(s);
        boolean hidePast = s.getBool("hidePast", true);
        LocalDate today = LocalDate.now();

        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_events);
        WidgetBg.apply(c, rv, s, R.id.ev_root, 0xFF0B0D17, 75);

        String title = s.getString("title", "").trim();
        if (title.isEmpty()) {
            title = c.getString(R.string.events_default_title);
        }
        FontStyles.text(rv, R.id.ev_title, title, font);
        rv.setTextColor(R.id.ev_title, RvUtil.faded(text));
        RvUtil.textSize(rv, R.id.ev_title, 13, scale);

        List<Item> items = new ArrayList<>();
        for (int i = 0; i < MAX; i++) {
            LocalDate d = dateOf(s, i);
            String name = s.getString("e" + i + "n", "").trim();
            if (d == null || name.isEmpty() || (hidePast && d.isBefore(today))) {
                continue;
            }
            Item it = new Item();
            it.name = name;
            it.date = d;
            items.add(it);
        }
        items.sort((a, b) -> a.date.compareTo(b.date));

        for (int i = 0; i < MAX; i++) {
            if (i >= items.size()) {
                rv.setViewVisibility(ROWS[i], View.GONE);
                continue;
            }
            Item it = items.get(i);
            int days = (int) ChronoUnit.DAYS.between(today, it.date);
            String when;
            if (days == 0) {
                when = c.getString(R.string.ev_today);
            } else if (days > 0) {
                when = c.getResources().getQuantityString(R.plurals.ev_in_days, days, days);
            } else {
                when = c.getResources().getQuantityString(R.plurals.ev_days_ago, -days, -days);
            }
            rv.setViewVisibility(ROWS[i], View.VISIBLE);
            FontStyles.text(rv, NAMES[i], it.name, font);
            rv.setTextColor(NAMES[i], text);
            RvUtil.textSize(rv, NAMES[i], 15, scale);
            FontStyles.text(rv, WHEN[i], when, font);
            rv.setTextColor(WHEN[i], days < 0 ? RvUtil.faded(text) : accent);
            RvUtil.textSize(rv, WHEN[i], 14, scale);
        }
        if (items.isEmpty()) {
            rv.setViewVisibility(ROWS[0], View.VISIBLE);
            FontStyles.text(rv, NAMES[0], c.getString(R.string.events_empty), font);
            rv.setTextColor(NAMES[0], RvUtil.faded(text));
            rv.setTextViewText(WHEN[0], "");
        }
        if (!preview) {
            Intent edit = new Intent(c, EventsActivity.class);
            edit.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            edit.setData(Uri.parse("vorki://events/" + widgetId));
            rv.setOnClickPendingIntent(R.id.ev_root, PendingIntent.getActivity(c, widgetId, edit,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        return rv;
    }
}
