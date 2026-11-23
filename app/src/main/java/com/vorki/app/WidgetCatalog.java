package com.vorki.app;

import com.vorki.app.apps.AppsActivity;
import com.vorki.app.calendar.CalendarActivity;
import com.vorki.app.clock.ClockActivity;
import com.vorki.app.countdown.CountdownActivity;
import com.vorki.app.events.EventsActivity;
import com.vorki.app.fun.FunActivity;
import com.vorki.app.notes.NotesActivity;
import com.vorki.app.photo.PhotoActivity;
import com.vorki.app.quote.QuoteActivity;
import com.vorki.app.transparent.TransparentActivity;

/**
 * The list of widgets shown on the main screen, in groups.
 * To add a new widget later: create its package (provider, settings screen), register both in
 * AndroidManifest.xml, add an info file in res/xml and add ONE line here.
 */
public final class WidgetCatalog {

    private WidgetCatalog() {
    }

    public static final class Entry {
        public final int titleRes;
        public final int subtitleRes;
        public final int iconRes;
        public final Class<?> screen;

        Entry(int titleRes, int subtitleRes, int iconRes, Class<?> screen) {
            this.titleRes = titleRes;
            this.subtitleRes = subtitleRes;
            this.iconRes = iconRes;
            this.screen = screen;
        }
    }

    public static final class Category {
        public final int titleRes;
        public final Entry[] entries;

        Category(int titleRes, Entry... entries) {
            this.titleRes = titleRes;
            this.entries = entries;
        }
    }

    public static final Category[] ALL = {
            new Category(R.string.cat_time,
                    new Entry(R.string.clock_name, R.string.clock_sub, R.drawable.ic_clock, ClockActivity.class),
                    new Entry(R.string.calendar_name, R.string.calendar_sub, R.drawable.ic_calendar, CalendarActivity.class)),
            new Category(R.string.cat_counters,
                    new Entry(R.string.counters_name, R.string.counters_sub, R.drawable.ic_hourglass, CountdownActivity.class),
                    new Entry(R.string.events_name, R.string.events_sub, R.drawable.ic_events, EventsActivity.class)),
            new Category(R.string.cat_notes,
                    new Entry(R.string.notes_name, R.string.notes_sub, R.drawable.ic_note, NotesActivity.class),
                    new Entry(R.string.quote_name, R.string.quote_sub, R.drawable.ic_quote, QuoteActivity.class)),
            new Category(R.string.cat_photos,
                    new Entry(R.string.photo_name, R.string.photo_sub, R.drawable.ic_photo, PhotoActivity.class)),
            new Category(R.string.cat_shortcuts,
                    new Entry(R.string.transparent_name, R.string.transparent_sub, R.drawable.ic_transparent, TransparentActivity.class),
                    new Entry(R.string.apps_name, R.string.apps_sub, R.drawable.ic_apps, AppsActivity.class)),
            new Category(R.string.cat_fun,
                    new Entry(R.string.fun_name, R.string.fun_sub, R.drawable.ic_dice, FunActivity.class)),
    };
}
