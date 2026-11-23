package com.vorki.app.calendar;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.vorki.app.R;
import com.vorki.app.core.FontStyles;
import com.vorki.app.core.RvUtil;
import com.vorki.app.core.Settings;
import com.vorki.app.core.WidgetBg;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class CalendarRenderer {

    public static final int STYLE_DATE = 0;
    public static final int STYLE_FULL = 1;
    public static final int STYLE_MONTH = 2;

    private static final int[] WEEKDAY_IDS = {R.id.wd0, R.id.wd1, R.id.wd2, R.id.wd3, R.id.wd4, R.id.wd5, R.id.wd6};

    private static final int[] DAY_IDS = {
            R.id.d0,R.id.d1,R.id.d2,R.id.d3,R.id.d4,R.id.d5,R.id.d6,
            R.id.d7,R.id.d8,R.id.d9,R.id.d10,R.id.d11,R.id.d12,R.id.d13,
            R.id.d14,R.id.d15,R.id.d16,R.id.d17,R.id.d18,R.id.d19,R.id.d20,
            R.id.d21,R.id.d22,R.id.d23,R.id.d24,R.id.d25,R.id.d26,R.id.d27,
            R.id.d28,R.id.d29,R.id.d30,R.id.d31,R.id.d32,R.id.d33,R.id.d34,
            R.id.d35,R.id.d36,R.id.d37,R.id.d38,R.id.d39,R.id.d40,R.id.d41
    };

    private CalendarRenderer() {
    }

    public static RemoteViews render(Context context, int widgetId, Settings s, boolean preview) {
        int style = s.getInt("style", STYLE_MONTH);
        int textColor = s.getInt("text", 0xFFFFFFFF);
        int accent = s.getInt("accent", 0xFF7C5CFF);
        int scale = s.getInt("scale", 100);
        int font = FontStyles.index(s);
        int weekStart = s.getInt("week", 0) == 1 ? Calendar.MONDAY : Calendar.SUNDAY;

        int layout = style == STYLE_DATE ? R.layout.widget_calendar_date
                : (style == STYLE_FULL ? R.layout.widget_calendar_full : R.layout.widget_calendar_month);
        RemoteViews rv = new RemoteViews(context.getPackageName(), layout);

        WidgetBg.apply(context, rv, s, R.id.cal_root, 0xFF0B0D17, 75);

        Locale locale = Locale.getDefault();
        Calendar today = Calendar.getInstance();

        FontStyles.text(rv, R.id.tv_day_num, String.valueOf(today.get(Calendar.DAY_OF_MONTH)), font);
        rv.setTextColor(R.id.tv_day_num, accent);

        if (style == STYLE_DATE) {
            RvUtil.textSize(rv, R.id.tv_day_num, 72, scale);
        } else {
            FontStyles.text(rv, R.id.tv_weekday,
                    new SimpleDateFormat("EEEE", locale).format(today.getTime()), font);
            FontStyles.text(rv, R.id.tv_month_year,
                    new SimpleDateFormat("MMMM yyyy", locale).format(today.getTime()), font);
            rv.setTextColor(R.id.tv_weekday, textColor);
            rv.setTextColor(R.id.tv_month_year, RvUtil.faded(textColor));
            if (style == STYLE_FULL) {
                RvUtil.textSize(rv, R.id.tv_day_num, 64, scale);
                RvUtil.textSize(rv, R.id.tv_weekday, 18, scale);
                RvUtil.textSize(rv, R.id.tv_month_year, 14, scale);
            } else {
                RvUtil.textSize(rv, R.id.tv_day_num, 40, scale);
                RvUtil.textSize(rv, R.id.tv_weekday, 16, scale);
                RvUtil.textSize(rv, R.id.tv_month_year, 13, scale);
                fillMonth(rv, today, weekStart, textColor, accent, scale, font, locale);
            }
        }

        if (!preview) {
            // Tap = open the phone's calendar app (if there is one)
            Intent open = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pending = PendingIntent.getActivity(context, widgetId, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.cal_root, pending);
        }
        return rv;
    }

    private static void fillMonth(RemoteViews rv, Calendar today, int weekStart, int textColor,
                                  int accent, int scale, int font, Locale locale) {
        // Weekday letters
        SimpleDateFormat narrow = new SimpleDateFormat("EEEEE", locale);
        Calendar tmp = Calendar.getInstance();
        for (int k = 0; k < 7; k++) {
            int dayOfWeek = ((weekStart - 1 + k) % 7) + 1; // 1 = Sunday ... 7 = Saturday
            tmp.set(Calendar.DAY_OF_WEEK, dayOfWeek);
            FontStyles.text(rv, WEEKDAY_IDS[k], narrow.format(tmp.getTime()), font);
            rv.setTextColor(WEEKDAY_IDS[k], RvUtil.faded(textColor));
            RvUtil.textSize(rv, WEEKDAY_IDS[k], 11, scale);
        }

        Calendar first = (Calendar) today.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int offset = (first.get(Calendar.DAY_OF_WEEK) - weekStart + 7) % 7;
        int daysInMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH);
        int todayNumber = today.get(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < DAY_IDS.length; i++) {
            int viewId = DAY_IDS[i];
            int day = i - offset + 1;
            boolean valid = day >= 1 && day <= daysInMonth;
            FontStyles.text(rv, viewId, valid ? String.valueOf(day) : "", font);
            RvUtil.textSize(rv, viewId, 12, scale);
            if (valid && day == todayNumber) {
                rv.setTextColor(viewId, RvUtil.onColor(accent));
                RvUtil.tintBackground(rv, viewId, R.drawable.today_bg, accent);
            } else {
                rv.setTextColor(viewId, textColor);
                RvUtil.clearBackground(rv, viewId);
            }
        }
    }
}
