package com.vorki.app.countdown;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.vorki.app.R;
import com.vorki.app.core.Alarms;
import com.vorki.app.core.FontStyles;
import com.vorki.app.core.WidgetBg;
import com.vorki.app.core.RvUtil;
import com.vorki.app.core.Settings;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.Locale;

public final class CountdownRenderer {

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    public static final int TYPE_COUNTDOWN = 0;
    public static final int TYPE_SINCE = 1;
    public static final int TYPE_PROGRESS = 2;

    private static final int[] SEG_IDS = {R.id.seg0, R.id.seg1, R.id.seg2, R.id.seg3, R.id.seg4, R.id.seg5, R.id.seg6, R.id.seg7, R.id.seg8, R.id.seg9, R.id.seg10, R.id.seg11, R.id.seg12, R.id.seg13, R.id.seg14, R.id.seg15, R.id.seg16, R.id.seg17, R.id.seg18, R.id.seg19};

    private CountdownRenderer() {
    }

    /** What the widget says right now, and when it needs to be redrawn next. */
    public static final class Result {
        public String big;
        public String unit;
        public boolean finished;
        public long nextUpdate;
        public int percent = -1; // only for year / month / week progress
        public String dateText;
    }

    // ---- the target date ----------------------------------------------------------------

    /** A new countdown starts with a date 30 days from now (so it does not move every day). */
    public static boolean ensureDefaults(Settings s) {
        boolean changed = false;
        if (!s.has("sY")) {
            setSince(s, LocalDate.now());
            changed = true;
        }
        if (s.has("tY")) {
            return changed;
        }
        LocalDate d = LocalDate.now().plusDays(30);
        s.put("tY", d.getYear());
        s.put("tM", d.getMonthValue());
        s.put("tD", d.getDayOfMonth());
        s.put("tH", 0);
        s.put("tMin", 0);
        return true;
    }

    public static void setSince(Settings s, LocalDate d) {
        s.put("sY", d.getYear());
        s.put("sM", d.getMonthValue());
        s.put("sD", d.getDayOfMonth());
    }

    public static LocalDate sinceOf(Settings s) {
        try {
            return LocalDate.of(s.getInt("sY", 2026), s.getInt("sM", 1), s.getInt("sD", 1));
        } catch (RuntimeException e) {
            return LocalDate.now();
        }
    }

    public static String sinceLabel(Settings s) {
        return sinceOf(s).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
    }

    public static LocalDate dateOf(Settings s) {
        if (!s.has("tY")) {
            return LocalDate.now().plusDays(30);
        }
        try {
            return LocalDate.of(s.getInt("tY", 2026), s.getInt("tM", 1), s.getInt("tD", 1));
        } catch (RuntimeException e) {
            return LocalDate.now().plusDays(30);
        }
    }

    public static LocalTime timeOf(Settings s) {
        try {
            return LocalTime.of(s.getInt("tH", 0), s.getInt("tMin", 0));
        } catch (RuntimeException e) {
            return LocalTime.MIDNIGHT;
        }
    }

    public static long targetMillis(Settings s) {
        return LocalDateTime.of(dateOf(s), timeOf(s))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static String dateLabel(Settings s) {
        return dateOf(s).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
    }

    public static String timeLabel(Settings s) {
        return timeOf(s).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
    }

    // ---- the text -----------------------------------------------------------------------

    public static Result compute(Context context, Settings s, long now) {
        int type = s.getInt("ctype", TYPE_COUNTDOWN);
        if (type == TYPE_SINCE) {
            return computeSince(context, s, now);
        }
        if (type == TYPE_PROGRESS) {
            return computeProgress(context, s, now);
        }
        return computeCountdown(context, s, now);
    }

    private static Result computeSince(Context context, Settings s, long now) {
        LocalDate today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();
        int days = (int) Math.max(0, ChronoUnit.DAYS.between(sinceOf(s), today));
        Result r = new Result();
        r.big = String.valueOf(days);
        r.unit = context.getResources().getQuantityString(R.plurals.days_since, days);
        r.dateText = sinceLabel(s);
        r.nextUpdate = Alarms.nextMidnight(now);
        return r;
    }

    private static Result computeProgress(Context context, Settings s, long now) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime nowTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone);
        LocalDate today = nowTime.toLocalDate();
        int scope = s.getInt("scope", 0);
        LocalDate start;
        LocalDate end; // exclusive
        String label;
        if (scope == 1) {
            start = today.withDayOfMonth(1);
            end = start.plusMonths(1);
            label = context.getString(R.string.progress_of_month,
                    today.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()));
        } else if (scope == 2) {
            start = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
            end = start.plusDays(7);
            label = context.getString(R.string.progress_of_week);
        } else {
            start = today.withDayOfYear(1);
            end = start.plusYears(1);
            label = context.getString(R.string.progress_of_year, today.getYear());
        }
        long from = start.atStartOfDay(zone).toInstant().toEpochMilli();
        long to = end.atStartOfDay(zone).toInstant().toEpochMilli();
        int percent = (int) Math.max(0, Math.min(100, (now - from) * 100 / Math.max(1, to - from)));
        Result r = new Result();
        r.percent = percent;
        r.big = percent + "%";
        r.unit = label;
        r.dateText = "";
        r.nextUpdate = Math.min(Alarms.nextMidnight(now), now + 60 * MINUTE);
        return r;
    }

    private static Result computeCountdown(Context context, Settings s, long now) {
        Resources res = context.getResources();
        ZoneId zone = ZoneId.systemDefault();
        long target = targetMillis(s);
        long remaining = target - now;
        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        LocalDate targetDate = Instant.ofEpochMilli(target).atZone(zone).toLocalDate();

        Result r = new Result();
        if (remaining > 0) {
            if (remaining >= DAY) {
                int days = (int) Math.max(1, ChronoUnit.DAYS.between(today, targetDate));
                r.big = String.valueOf(days);
                r.unit = res.getQuantityString(R.plurals.days_left, days);
                r.nextUpdate = Math.min(Alarms.nextMidnight(now), target - DAY + 2000);
            } else if (remaining >= HOUR) {
                int hours = (int) (remaining / HOUR);
                r.big = String.valueOf(hours);
                r.unit = res.getQuantityString(R.plurals.hours_left, hours);
                r.nextUpdate = Math.min(now + 15 * MINUTE, target + 2000);
            } else {
                int minutes = (int) Math.max(1, (remaining + MINUTE - 1) / MINUTE);
                r.big = String.valueOf(minutes);
                r.unit = res.getQuantityString(R.plurals.minutes_left, minutes);
                r.nextUpdate = Math.min(now + 5 * MINUTE, target + 2000);
            }
        } else {
            int since = (int) ChronoUnit.DAYS.between(targetDate, today);
            r.finished = true;
            r.big = context.getString(R.string.cd_done);
            r.unit = since <= 0
                    ? context.getString(R.string.cd_finished_today)
                    : res.getQuantityString(R.plurals.finished_days_ago, since, since);
            r.nextUpdate = Alarms.nextMidnight(now);
        }
        if (r.nextUpdate <= now) {
            r.nextUpdate = now + MINUTE;
        }
        return r;
    }

    // ---- drawing ------------------------------------------------------------------------

    public static RemoteViews render(Context context, int widgetId, Settings s, boolean preview) {
        Result r = compute(context, s, System.currentTimeMillis());
        int type = s.getInt("ctype", TYPE_COUNTDOWN);
        boolean hideWhenFinished = s.getInt("finish", 0) == 1;

        // Tapping the widget opens its own settings.
        PendingIntent pending = null;
        if (!preview) {
            Intent edit = new Intent(context, CountdownActivity.class);
            edit.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            edit.setData(Uri.parse("vorki://countdown/" + widgetId));
            pending = PendingIntent.getActivity(context, widgetId, edit,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        if (type == TYPE_COUNTDOWN && r.finished && hideWhenFinished) {
            RemoteViews empty = new RemoteViews(context.getPackageName(), R.layout.widget_empty);
            if (pending != null) {
                empty.setOnClickPendingIntent(R.id.empty_root, pending);
            }
            return empty;
        }

        int textColor = s.getInt("text", 0xFFFFFFFF);
        int accent = s.getInt("accent", 0xFFFF5C8A);
        int scale = s.getInt("scale", 100);
        int font = FontStyles.index(s);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_countdown);
        WidgetBg.apply(context, rv, s, R.id.cd_root, 0xFF0B0D17, 75);

        String defTitle = context.getString(type == TYPE_SINCE ? R.string.since_default_title
                : (type == TYPE_PROGRESS ? R.string.progress_default_title : R.string.cd_default_title));
        String title = s.getString("title", "").trim();
        if (title.isEmpty()) {
            title = defTitle;
        }
        rv.setViewVisibility(R.id.cd_title, View.VISIBLE);
        FontStyles.text(rv, R.id.cd_title, title, font);
        rv.setTextColor(R.id.cd_title, RvUtil.faded(textColor));

        FontStyles.text(rv, R.id.cd_big, r.big, font);
        rv.setTextColor(R.id.cd_big, accent);
        FontStyles.text(rv, R.id.cd_unit, r.unit, font);
        rv.setTextColor(R.id.cd_unit, textColor);

        String dateText = r.dateText != null ? r.dateText : dateLabel(s);
        if (dateText.isEmpty()) {
            rv.setViewVisibility(R.id.cd_date, View.GONE);
        } else {
            rv.setViewVisibility(R.id.cd_date, View.VISIBLE);
            FontStyles.text(rv, R.id.cd_date, dateText, font);
            rv.setTextColor(R.id.cd_date, RvUtil.faded(textColor));
        }

        if (r.percent >= 0) {
            rv.setViewVisibility(R.id.cd_bar, View.VISIBLE);
            int filled = Math.round(r.percent * SEG_IDS.length / 100f);
            for (int i = 0; i < SEG_IDS.length; i++) {
                RvUtil.tintBackground(rv, SEG_IDS[i], R.drawable.seg_bg,
                        i < filled ? accent : RvUtil.withOpacity(textColor, 25));
            }
        } else {
            rv.setViewVisibility(R.id.cd_bar, View.GONE);
        }

        RvUtil.textSize(rv, R.id.cd_title, 14, scale);
        RvUtil.textSize(rv, R.id.cd_big, r.finished ? 44 : 56, scale);
        RvUtil.textSize(rv, R.id.cd_unit, 15, scale);
        RvUtil.textSize(rv, R.id.cd_date, 12, scale);

        if (pending != null) {
            rv.setOnClickPendingIntent(R.id.cd_root, pending);
        }
        return rv;
    }
}
