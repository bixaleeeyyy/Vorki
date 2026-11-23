package com.vorki.app.notes;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
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

public class NotesProvider extends VorkiProvider {

    static final String ACTION_TOGGLE = "com.vorki.app.action.TOGGLE_TODO";
    static final int MAX_ITEMS = 8;

    private static final int[] ROWS = {R.id.td_row0, R.id.td_row1, R.id.td_row2, R.id.td_row3,
            R.id.td_row4, R.id.td_row5, R.id.td_row6, R.id.td_row7};
    private static final int[] BOXES = {R.id.td_box0, R.id.td_box1, R.id.td_box2, R.id.td_box3,
            R.id.td_box4, R.id.td_box5, R.id.td_box6, R.id.td_box7};
    private static final int[] TEXTS = {R.id.td_text0, R.id.td_text1, R.id.td_text2, R.id.td_text3,
            R.id.td_text4, R.id.td_text5, R.id.td_text6, R.id.td_text7};

    // Presets: paper colour, ink colour, pattern (0 none, 1 lines, 2 grid, 3 dots)
    public static final int[] PAPER = {0xFFFFF3A3, 0xFFBDF2D5, 0xFFFFC8DD, 0xFFBDE0FE,
            0xFFDCC8FF, 0xFF1E2233, 0xFFFFFDF5, 0xFFF4F7FB};
    public static final int[] INK = {0xFF3B3200, 0xFF0F3D2B, 0xFF5C1230, 0xFF0B2F52,
            0xFF2D1A5C, 0xFFFFFFFF, 0xFF262626, 0xFF1F2A44};
    public static final int[] PATTERN = {0, 0, 0, 0, 3, 0, 1, 2};

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
        if (ACTION_TOGGLE.equals(intent.getAction())) {
            int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
            int index = intent.getIntExtra("index", -1);
            if (id > 0 && index >= 0) {
                Settings s = Settings.loadForWidget(c, id);
                String[] lines = s.getString("todo", "").split("\n", -1);
                if (index < lines.length) {
                    lines[index] = isDone(lines[index]) ? clean(lines[index]) : "x " + lines[index];
                    s.put("todo", String.join("\n", lines));
                    s.save(c, id);
                    AppWidgetManager m = AppWidgetManager.getInstance(c);
                    m.updateAppWidget(id, render(c, id, s, false));
                }
            }
            return;
        }
        super.onReceive(c, intent);
    }

    public static boolean isDone(String line) {
        return line.startsWith("x ");
    }

    public static String clean(String line) {
        return isDone(line) ? line.substring(2) : line;
    }

    public static RemoteViews render(Context c, int widgetId, Settings s, boolean preview) {
        boolean todo = s.getInt("ntype", 0) == 1;
        int preset = Math.max(0, Math.min(PAPER.length - 1, s.getInt("preset", 0)));
        int ink = INK[preset];
        int scale = s.getInt("scale", 100);
        int font = FontStyles.index(s);

        RemoteViews rv = new RemoteViews(c.getPackageName(), todo ? R.layout.widget_todo : R.layout.widget_note);
        boolean picture = WidgetBg.applyPicture(c, rv, s);
        RvUtil.tintBackground(rv, R.id.nt_root, R.drawable.widget_bg, PAPER[preset]);
        if (picture) {
            ink = 0xFFFFFFFF;
        }
        int[] patterns = {0, R.drawable.pattern_lines, R.drawable.pattern_grid, R.drawable.pattern_dots};
        if (PATTERN[preset] != 0 && !picture) {
            rv.setViewVisibility(R.id.note_pattern, View.VISIBLE);
            rv.setInt(R.id.note_pattern, "setBackgroundResource", patterns[PATTERN[preset]]);
        } else {
            rv.setViewVisibility(R.id.note_pattern, View.GONE);
        }

        String title = s.getString("title", "").trim();
        int titleId = todo ? R.id.td_title : R.id.nt_title;
        if (title.isEmpty()) {
            rv.setViewVisibility(titleId, View.GONE);
        } else {
            rv.setViewVisibility(titleId, View.VISIBLE);
            FontStyles.text(rv, titleId, title, font);
            rv.setTextColor(titleId, ink);
            RvUtil.textSize(rv, titleId, 14, scale);
        }

        if (!todo) {
            String body = s.getString("note", "");
            FontStyles.text(rv, R.id.nt_text, body.isEmpty() ? c.getString(R.string.note_placeholder) : body, font);
            rv.setTextColor(R.id.nt_text, body.isEmpty() ? RvUtil.faded(ink) : ink);
            RvUtil.textSize(rv, R.id.nt_text, 16, scale);
            if (!preview) {
                rv.setOnClickPendingIntent(R.id.nt_root, editIntent(c, widgetId));
            }
            return rv;
        }

        String[] lines = s.getString("todo", "").split("\n");
        int shown = 0;
        int extra = 0;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) {
                continue;
            }
            if (shown >= MAX_ITEMS) {
                extra++;
                continue;
            }
            boolean done = isDone(lines[i]);
            rv.setViewVisibility(ROWS[shown], View.VISIBLE);
            rv.setImageViewResource(BOXES[shown], done ? R.drawable.ic_check_on : R.drawable.ic_check_off);
            rv.setInt(BOXES[shown], "setColorFilter", done ? RvUtil.faded(ink) : ink);
            FontStyles.text(rv, TEXTS[shown], clean(lines[i]), font, done);
            rv.setTextColor(TEXTS[shown], done ? RvUtil.faded(ink) : ink);
            RvUtil.textSize(rv, TEXTS[shown], 15, scale);
            if (!preview) {
                Intent toggle = new Intent(c, NotesProvider.class).setAction(ACTION_TOGGLE);
                toggle.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                toggle.putExtra("index", i);
                toggle.setData(Uri.parse("vorki://todo/" + widgetId + "/" + i));
                rv.setOnClickPendingIntent(ROWS[shown], PendingIntent.getBroadcast(c, 0, toggle,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            }
            shown++;
        }
        for (int i = shown; i < MAX_ITEMS; i++) {
            rv.setViewVisibility(ROWS[i], View.GONE);
        }
        if (extra > 0) {
            rv.setViewVisibility(R.id.td_more, View.VISIBLE);
            rv.setTextViewText(R.id.td_more, c.getString(R.string.todo_more, extra));
            rv.setTextColor(R.id.td_more, RvUtil.faded(ink));
        } else {
            rv.setViewVisibility(R.id.td_more, View.GONE);
        }
        if (!preview) {
            rv.setOnClickPendingIntent(R.id.td_title, editIntent(c, widgetId));
            rv.setOnClickPendingIntent(R.id.td_more, editIntent(c, widgetId));
        }
        return rv;
    }

    private static PendingIntent editIntent(Context c, int widgetId) {
        Intent edit = new Intent(c, NotesActivity.class);
        edit.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        edit.setData(Uri.parse("vorki://note/" + widgetId));
        return PendingIntent.getActivity(c, widgetId, edit,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

}
