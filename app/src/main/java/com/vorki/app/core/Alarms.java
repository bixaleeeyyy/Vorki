package com.vorki.app.core;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/** Inexact alarms (no special permission needed) used to refresh widgets at the right time. */
public final class Alarms {

    private Alarms() {
    }

    private static PendingIntent pending(Context c, Class<?> receiver, String action,
                                         int requestCode, int widgetId) {
        Intent intent = new Intent(c, receiver);
        intent.setAction(action);
        if (widgetId > 0) {
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        }
        return PendingIntent.getBroadcast(c, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void setAt(Context c, Class<?> receiver, String action, int requestCode,
                             int widgetId, long atMillis) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis,
                pending(c, receiver, action, requestCode, widgetId));
    }

    public static void cancel(Context c, Class<?> receiver, String action, int requestCode,
                              int widgetId) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        am.cancel(pending(c, receiver, action, requestCode, widgetId));
    }

    /** A couple of seconds after the next midnight. */
    public static long nextMidnight(long now) {
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(now);
        next.add(Calendar.DAY_OF_YEAR, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 2);
        next.set(Calendar.MILLISECOND, 0);
        return next.getTimeInMillis();
    }
}
