package com.vorki.app.core;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Base of every widget provider. A new widget only has to say how to draw itself.
 * Refreshing (at midnight, after a time change, after a reboot) is handled here.
 */
public abstract class VorkiProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.vorki.app.action.REFRESH";

    /** Draws and pushes one widget. */
    protected abstract void update(Context c, AppWidgetManager m, int id);

    /** When this kind of widget should be redrawn next (milliseconds). Default: next midnight. */
    protected long nextRefreshAt(Context c, int[] ids) {
        return Alarms.nextMidnight(System.currentTimeMillis());
    }

    /** Return false for widgets that never need a timed refresh (for example the clock). */
    protected boolean wantsTimedRefresh() {
        return true;
    }

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) {
            try {
                update(c, m, id);
            } catch (RuntimeException ignored) {
                // One broken widget must not stop the others.
            }
        }
        schedule(c, m);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (ACTION_REFRESH.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            onUpdate(c, m, m.getAppWidgetIds(new ComponentName(c, getClass())));
            return;
        }
        super.onReceive(c, intent);
    }

    @Override
    public void onDeleted(Context c, int[] ids) {
        Settings.delete(c, ids);
        BgImages.IO.execute(() -> BgImages.cleanup(c.getApplicationContext()));
    }

    @Override
    public void onDisabled(Context c) {
        Alarms.cancel(c, getClass(), ACTION_REFRESH, 1, 0);
    }

    private void schedule(Context c, AppWidgetManager m) {
        if (!wantsTimedRefresh()) {
            return;
        }
        int[] ids = m.getAppWidgetIds(new ComponentName(c, getClass()));
        if (ids.length == 0) {
            return;
        }
        long at = nextRefreshAt(c, ids);
        if (at > 0) {
            Alarms.setAt(c, getClass(), ACTION_REFRESH, 1, 0, at);
        }
    }

    /** Asks a provider to redraw all of its widgets (used after a tap changes something). */
    public static void requestRefresh(Context c, Class<?> provider) {
        c.sendBroadcast(new Intent(c, provider).setAction(ACTION_REFRESH));
    }
}
