package com.vorki.app.calendar;

import android.appwidget.AppWidgetManager;
import android.content.Context;

import com.vorki.app.core.Settings;
import com.vorki.app.core.VorkiProvider;

public class CalendarProvider extends VorkiProvider {

    @Override
    protected void update(Context c, AppWidgetManager m, int id) {
        m.updateAppWidget(id, CalendarRenderer.render(c, id, Settings.loadForWidget(c, id), false));
    }
}
