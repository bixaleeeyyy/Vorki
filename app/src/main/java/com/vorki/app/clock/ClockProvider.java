package com.vorki.app.clock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;
import android.view.View;
import android.widget.RemoteViews;

import com.vorki.app.R;
import com.vorki.app.core.FontStyles;
import com.vorki.app.core.RvUtil;
import com.vorki.app.core.Settings;
import com.vorki.app.core.VorkiProvider;
import com.vorki.app.core.WidgetBg;


public class ClockProvider extends VorkiProvider {

    /** [style][font] */
    private static final int[][] DIGITAL = {
            {R.layout.widget_clock_d0_f0, R.layout.widget_clock_d0_f1, R.layout.widget_clock_d0_f2, R.layout.widget_clock_d0_f3, R.layout.widget_clock_d0_f4, R.layout.widget_clock_d0_f5, R.layout.widget_clock_d0_f6, R.layout.widget_clock_d0_f7},
            {R.layout.widget_clock_d1_f0, R.layout.widget_clock_d1_f1, R.layout.widget_clock_d1_f2, R.layout.widget_clock_d1_f3, R.layout.widget_clock_d1_f4, R.layout.widget_clock_d1_f5, R.layout.widget_clock_d1_f6, R.layout.widget_clock_d1_f7},
            {R.layout.widget_clock_d2_f0, R.layout.widget_clock_d2_f1, R.layout.widget_clock_d2_f2, R.layout.widget_clock_d2_f3, R.layout.widget_clock_d2_f4, R.layout.widget_clock_d2_f5, R.layout.widget_clock_d2_f6, R.layout.widget_clock_d2_f7},
            {R.layout.widget_clock_d3_f0, R.layout.widget_clock_d3_f1, R.layout.widget_clock_d3_f2, R.layout.widget_clock_d3_f3, R.layout.widget_clock_d3_f4, R.layout.widget_clock_d3_f5, R.layout.widget_clock_d3_f6, R.layout.widget_clock_d3_f7},
            {R.layout.widget_clock_d4_f0, R.layout.widget_clock_d4_f1, R.layout.widget_clock_d4_f2, R.layout.widget_clock_d4_f3, R.layout.widget_clock_d4_f4, R.layout.widget_clock_d4_f5, R.layout.widget_clock_d4_f6, R.layout.widget_clock_d4_f7},
            {R.layout.widget_clock_d5_f0, R.layout.widget_clock_d5_f1, R.layout.widget_clock_d5_f2, R.layout.widget_clock_d5_f3, R.layout.widget_clock_d5_f4, R.layout.widget_clock_d5_f5, R.layout.widget_clock_d5_f6, R.layout.widget_clock_d5_f7},
            {R.layout.widget_clock_d6_f0, R.layout.widget_clock_d6_f1, R.layout.widget_clock_d6_f2, R.layout.widget_clock_d6_f3, R.layout.widget_clock_d6_f4, R.layout.widget_clock_d6_f5, R.layout.widget_clock_d6_f6, R.layout.widget_clock_d6_f7},
            {R.layout.widget_clock_d7_f0, R.layout.widget_clock_d7_f1, R.layout.widget_clock_d7_f2, R.layout.widget_clock_d7_f3, R.layout.widget_clock_d7_f4, R.layout.widget_clock_d7_f5, R.layout.widget_clock_d7_f6, R.layout.widget_clock_d7_f7}
    };

    private static final int[] ANALOG_LIGHT = {
            R.layout.widget_clock_a0_light, R.layout.widget_clock_a1_light,
            R.layout.widget_clock_a2_light, R.layout.widget_clock_a3_light
    };
    private static final int[] ANALOG_DARK = {
            R.layout.widget_clock_a0_dark, R.layout.widget_clock_a1_dark,
            R.layout.widget_clock_a2_dark, R.layout.widget_clock_a3_dark
    };

    public static final int STYLE_PHOTO = 7;
    public static final int FIRST_ANALOG = 8;

    @Override
    protected void update(Context c, AppWidgetManager m, int id) {
        m.updateAppWidget(id, render(c, id, Settings.loadForWidget(c, id), false));
    }

    @Override
    protected boolean wantsTimedRefresh() {
        return false;
    }

    /** Hours mode: 0 = follow the phone, 1 = always 12 hour, 2 = always 24 hour. */
    private static void hours(RemoteViews rv, int id, String f12, String f24, int mode) {
        if (mode == 1) {
            rv.setCharSequence(id, "setFormat24Hour", f12);
        } else if (mode == 2) {
            rv.setCharSequence(id, "setFormat12Hour", f24);
        }
    }

    private static void paint(RemoteViews rv, int id, int color, float sp, int scale) {
        rv.setTextColor(id, color);
        RvUtil.textSize(rv, id, sp, scale);
    }

    public static RemoteViews render(Context c, int widgetId, Settings s, boolean preview) {
        int style = Math.max(0, Math.min(11, s.getInt("cstyle", 0)));
        int font = FontStyles.index(s);
        int mode = s.getInt("hmode", 0);
        int text = s.getInt("text", 0xFFFFFFFF);
        int scale = s.getInt("scale", 100);
        int pos = s.getInt("pos", 1);

        RemoteViews rv;
        if (style >= FIRST_ANALOG) {
            boolean dark = s.getInt("face", 0) == 1;
            rv = new RemoteViews(c.getPackageName(),
                    (dark ? ANALOG_DARK : ANALOG_LIGHT)[style - FIRST_ANALOG]);
            if (style == FIRST_ANALOG + 3) {
                rv.setTextColor(R.id.tc_date, dark ? 0xFF12142B : 0xFFFFFFFF);
            }
        } else {
            rv = new RemoteViews(c.getPackageName(), DIGITAL[style][font]);
            switch (style) {
                case 0:
                    paint(rv, R.id.tc_time, text, 60, scale);
                    hours(rv, R.id.tc_time, "h:mm", "HH:mm", mode);
                    break;
                case 1:
                    paint(rv, R.id.tc_time, text, 54, scale);
                    paint(rv, R.id.tc_date, RvUtil.faded(text), 15, scale);
                    hours(rv, R.id.tc_time, "h:mm", "HH:mm", mode);
                    break;
                case 2:
                    paint(rv, R.id.tc_h, text, 64, scale);
                    paint(rv, R.id.tc_m, s.getInt("accent", 0xFF7C5CFF), 64, scale);
                    hours(rv, R.id.tc_h, "hh", "HH", mode);
                    break;
                case 3:
                    paint(rv, R.id.tc_time, text, 56, scale);
                    paint(rv, R.id.tc_ampm, s.getInt("accent", 0xFF7C5CFF), 18, scale);
                    hours(rv, R.id.tc_time, "h:mm", "HH:mm", mode);
                    hours(rv, R.id.tc_ampm, "a", "'  '", mode);
                    break;
                case 4:
                    paint(rv, R.id.tc_day, s.getInt("accent", 0xFF7C5CFF), 56, scale);
                    paint(rv, R.id.tc_dayname, text, 16, scale);
                    paint(rv, R.id.tc_monthyear, RvUtil.faded(text), 13, scale);
                    paint(rv, R.id.tc_time, text, 24, scale);
                    hours(rv, R.id.tc_time, "h:mm", "HH:mm", mode);
                    break;
                case 5: {
                    paint(rv, R.id.tc_time, text, 32, scale);
                    paint(rv, R.id.tc_time2, text, 32, scale);
                    paint(rv, R.id.lb_local, RvUtil.faded(text), 12, scale);
                    paint(rv, R.id.lb_other, RvUtil.faded(text), 12, scale);
                    hours(rv, R.id.tc_time, "h:mm", "HH:mm", mode);
                    hours(rv, R.id.tc_time2, "h:mm", "HH:mm", mode);
                    String tz = s.getString("tz", "Europe/London");
                    rv.setString(R.id.tc_time2, "setTimeZone", tz);
                    String city = tz.contains("/") ? tz.substring(tz.indexOf('/') + 1) : tz;
                    rv.setTextViewText(R.id.lb_other, city.replace('_', ' '));
                    rv.setTextViewText(R.id.lb_local, c.getString(R.string.clock_local));
                    break;
                }
                case 6:
                    paint(rv, R.id.tc_line, text, 24, scale);
                    hours(rv, R.id.tc_line, "h:mm  \u00B7  EEE d MMM", "HH:mm  \u00B7  EEE d MMM", mode);
                    break;
                default: {
                    int[] boxes = {R.id.pos_t, R.id.pos_c, R.id.pos_b, R.id.pos_l};
                    for (int i = 0; i < boxes.length; i++) {
                        rv.setViewVisibility(boxes[i], i == Math.max(0, Math.min(3, pos)) ? View.VISIBLE : View.GONE);
                    }
                    int[] times = {R.id.tc_time_t, R.id.tc_time_c, R.id.tc_time_b, R.id.tc_time_l};
                    int[] dates = {R.id.tc_date_t, R.id.tc_date_c, R.id.tc_date_b, R.id.tc_date_l};
                    for (int i = 0; i < 4; i++) {
                        paint(rv, times[i], text, 54, scale);
                        paint(rv, dates[i], text, 15, scale);
                        hours(rv, times[i], "h:mm", "HH:mm", mode);
                    }
                    break;
                }
            }
        }

        if (style == STYLE_PHOTO) {
            WidgetBg.apply(c, rv, s, R.id.clk_root, 0xFF0B0D17, 100);
        } else {
            WidgetBg.apply(c, rv, s, R.id.clk_root, 0xFF0B0D17, 75);
        }
        if (!preview) {
            Intent open = new Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            rv.setOnClickPendingIntent(R.id.clk_root, PendingIntent.getActivity(c, widgetId, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        return rv;
    }

}
