package com.vorki.app.fun;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.vorki.app.R;
import com.vorki.app.core.FontStyles;
import com.vorki.app.core.RvUtil;
import com.vorki.app.core.Settings;
import com.vorki.app.core.VorkiProvider;
import com.vorki.app.core.WidgetBg;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FunProvider extends VorkiProvider {

    static final String ACTION_ROLL = "com.vorki.app.action.FUN_ROLL";
    private static final String RUNTIME = "vorki_runtime";

    public static final int DICE = 0;
    public static final int COIN = 1;
    public static final int BALL = 2;
    public static final int RPS = 3;
    public static final int NUMBER = 4;
    public static final int PICKER = 5;

    private static final Random RANDOM = new Random();

    @Override
    protected void update(Context c, AppWidgetManager m, int id) {
        m.updateAppWidget(id, render(c, id, Settings.loadForWidget(c, id), false));
    }

    @Override
    protected boolean wantsTimedRefresh() {
        return false;
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        if (ACTION_ROLL.equals(intent.getAction())) {
            int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
            if (id > 0) {
                Settings s = Settings.loadForWidget(c, id);
                String result = roll(c, s);
                c.getSharedPreferences(RUNTIME, Context.MODE_PRIVATE).edit()
                        .putString("fun_" + id, result).apply();
                AppWidgetManager.getInstance(c).updateAppWidget(id, render(c, id, s, false));
            }
            return;
        }
        super.onReceive(c, intent);
    }

    @Override
    public void onDeleted(Context c, int[] ids) {
        super.onDeleted(c, ids);
        SharedPreferences.Editor e = c.getSharedPreferences(RUNTIME, Context.MODE_PRIVATE).edit();
        for (int id : ids) {
            e.remove("fun_" + id);
        }
        e.apply();
    }

    /** A new result as "big|small". */
    private static String roll(Context c, Settings s) {
        switch (s.getInt("ftype", DICE)) {
            case COIN: {
                boolean heads = RANDOM.nextBoolean();
                return (heads ? "\uD83E\uDE99" : "\uD83E\uDE99") + "|"
                        + c.getString(heads ? R.string.coin_heads : R.string.coin_tails);
            }
            case BALL: {
                String[] answers = c.getResources().getStringArray(R.array.ball_answers);
                return "\uD83C\uDFB1|" + answers[RANDOM.nextInt(answers.length)];
            }
            case RPS: {
                String[] icons = {"\u270A", "\u270B", "\u270C"};
                int[] names = {R.string.rps_rock, R.string.rps_paper, R.string.rps_scissors};
                int k = RANDOM.nextInt(3);
                return icons[k] + "|" + c.getString(names[k]);
            }
            case NUMBER: {
                int a = s.getInt("rmin", 1);
                int b = s.getInt("rmax", 100);
                int lo = Math.min(a, b);
                int hi = Math.max(a, b);
                return (lo + RANDOM.nextInt(hi - lo + 1)) + "|" + lo + " - " + hi;
            }
            case PICKER: {
                List<String> items = new ArrayList<>();
                for (String line : s.getString("plist", "").split("\n")) {
                    if (!line.trim().isEmpty()) {
                        items.add(line.trim());
                    }
                }
                if (items.isEmpty()) {
                    return "?|" + c.getString(R.string.picker_empty);
                }
                return items.get(RANDOM.nextInt(items.size())) + "|";
            }
            default: {
                int n = Math.max(1, Math.min(3, s.getInt("dn", 1)));
                int sum = 0;
                StringBuilder parts = new StringBuilder();
                for (int i = 0; i < n; i++) {
                    int v = 1 + RANDOM.nextInt(6);
                    sum += v;
                    if (i > 0) {
                        parts.append(" + ");
                    }
                    parts.append(v);
                }
                return sum + "|" + (n > 1 ? parts.toString() : "");
            }
        }
    }

    private static int titleOf(int type) {
        int[] titles = {R.string.fun_dice, R.string.fun_coin, R.string.fun_ball,
                R.string.fun_rps, R.string.fun_number, R.string.fun_picker};
        return titles[Math.max(0, Math.min(5, type))];
    }

    public static RemoteViews render(Context c, int widgetId, Settings s, boolean preview) {
        int type = s.getInt("ftype", DICE);
        int text = s.getInt("text", 0xFFFFFFFF);
        int accent = s.getInt("accent", 0xFF22D3EE);
        int scale = s.getInt("scale", 100);
        int font = FontStyles.index(s);

        String stored = c.getSharedPreferences(RUNTIME, Context.MODE_PRIVATE).getString("fun_" + widgetId, null);
        if (stored == null || preview) {
            stored = placeholder(type);
        }
        int bar = stored.indexOf('|');
        String big = bar < 0 ? stored : stored.substring(0, bar);
        String sub = bar < 0 ? "" : stored.substring(bar + 1);

        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_fun);
        WidgetBg.apply(c, rv, s, R.id.fn_root, 0xFF0B0D17, 75);
        FontStyles.text(rv, R.id.fn_title, c.getString(titleOf(type)), font);
        rv.setTextColor(R.id.fn_title, RvUtil.faded(text));
        FontStyles.text(rv, R.id.fn_big, big, font);
        rv.setTextColor(R.id.fn_big, accent);
        RvUtil.textSize(rv, R.id.fn_big, big.length() > 6 ? 26 : 48, scale);
        FontStyles.text(rv, R.id.fn_sub, sub, font);
        rv.setTextColor(R.id.fn_sub, text);
        rv.setViewVisibility(R.id.fn_sub, sub.isEmpty() ? View.GONE : View.VISIBLE);
        rv.setTextColor(R.id.fn_hint, RvUtil.faded(text));
        rv.setTextViewText(R.id.fn_hint, c.getString(R.string.fun_tap_hint));

        if (!preview) {
            Intent tap = new Intent(c, FunProvider.class).setAction(ACTION_ROLL);
            tap.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            tap.setData(Uri.parse("vorki://fun/" + widgetId));
            rv.setOnClickPendingIntent(R.id.fn_root, PendingIntent.getBroadcast(c, 0, tap,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        return rv;
    }

    private static String placeholder(int type) {
        switch (type) {
            case COIN:
                return "\uD83E\uDE99|";
            case BALL:
                return "\uD83C\uDFB1|";
            case RPS:
                return "\u270A\u270B\u270C|";
            case NUMBER:
                return "42|";
            case PICKER:
                return "?|";
            default:
                return "6|";
        }
    }
}
