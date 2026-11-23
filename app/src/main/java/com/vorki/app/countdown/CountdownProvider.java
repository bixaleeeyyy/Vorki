package com.vorki.app.countdown;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.vorki.app.MainActivity;
import com.vorki.app.R;
import com.vorki.app.core.Alarms;
import com.vorki.app.core.Settings;
import com.vorki.app.core.VorkiProvider;

public class CountdownProvider extends VorkiProvider {

    static final String ACTION_DONE = "com.vorki.app.COUNTDOWN_DONE";
    private static final String CHANNEL = "countdown_alerts";
    private static final String RUNTIME_PREFS = "vorki_runtime";

    @Override
    protected void update(Context c, AppWidgetManager m, int id) {
        Settings s = Settings.loadForWidget(c, id);
        if (CountdownRenderer.ensureDefaults(s)) {
            s.save(c, id);
        }
        m.updateAppWidget(id, CountdownRenderer.render(c, id, s, false));
        scheduleDone(c, id, s);
    }

    @Override
    protected long nextRefreshAt(Context c, int[] ids) {
        long now = System.currentTimeMillis();
        long soonest = Alarms.nextMidnight(now);
        for (int id : ids) {
            soonest = Math.min(soonest,
                    CountdownRenderer.compute(c, Settings.loadForWidget(c, id), now).nextUpdate);
        }
        return soonest;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_DONE.equals(intent.getAction())) {
            int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
            if (id > 0) {
                notifyFinished(context, id);
            }
            AppWidgetManager m = AppWidgetManager.getInstance(context);
            onUpdate(context, m, m.getAppWidgetIds(new ComponentName(context, CountdownProvider.class)));
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onDeleted(Context context, int[] ids) {
        super.onDeleted(context, ids);
        for (int id : ids) {
            Alarms.cancel(context, CountdownProvider.class, ACTION_DONE, 1000 + id, id);
        }
    }

    /** The "it's finished" alert for countdowns that want one. */
    static void scheduleDone(Context context, int id, Settings s) {
        long target = CountdownRenderer.targetMillis(s);
        boolean wanted = s.getInt("ctype", 0) == CountdownRenderer.TYPE_COUNTDOWN
                && s.getBool("notify", false) && target > System.currentTimeMillis();
        if (wanted) {
            Alarms.setAt(context, CountdownProvider.class, ACTION_DONE, 1000 + id, id, target);
        } else {
            Alarms.cancel(context, CountdownProvider.class, ACTION_DONE, 1000 + id, id);
        }
    }

    private static void notifyFinished(Context context, int id) {
        Settings s = Settings.load(context, id);
        if (!s.getBool("notify", false)) {
            return;
        }
        long target = CountdownRenderer.targetMillis(s);
        SharedPreferences runtime = context.getSharedPreferences(RUNTIME_PREFS, Context.MODE_PRIVATE);
        if (runtime.getLong("done_" + id, 0L) == target) {
            return; // already told the user
        }
        runtime.edit().putLong("done_" + id, target).apply();

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }
        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT));

        String title = s.getString("title", context.getString(R.string.cd_default_title)).trim();
        if (title.isEmpty()) {
            title = context.getString(R.string.cd_default_title);
        }
        PendingIntent open = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_hourglass)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.cd_notification_text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build();
        nm.notify(2000 + id, notification);
    }
}
