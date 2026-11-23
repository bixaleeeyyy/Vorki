package com.vorki.app.clock;

import android.appwidget.AppWidgetProvider;
import android.widget.RemoteViews;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vorki.app.R;
import com.vorki.app.core.BaseWidgetActivity;
import com.vorki.app.core.Palette;
import com.vorki.app.core.Settings;

public class ClockActivity extends BaseWidgetActivity {

    private static final String[] ZONES = {
            "Pacific/Honolulu", "America/Anchorage", "America/Los_Angeles", "America/Denver",
            "America/Chicago", "America/New_York", "America/Sao_Paulo", "Atlantic/Reykjavik",
            "Europe/London", "Europe/Paris", "Europe/Berlin", "Europe/Moscow", "Africa/Cairo",
            "Africa/Lagos", "Africa/Nairobi", "Asia/Dubai", "Asia/Karachi", "Asia/Kolkata",
            "Asia/Dhaka", "Asia/Bangkok", "Asia/Singapore", "Asia/Shanghai", "Asia/Tokyo",
            "Asia/Seoul", "Australia/Sydney", "Pacific/Auckland"
    };

    @Override
    protected int titleRes() {
        return R.string.clock_name;
    }

    @Override
    protected int hintRes() {
        return R.string.clock_hint;
    }

    @Override
    protected Class<? extends AppWidgetProvider> providerClass() {
        return ClockProvider.class;
    }

    @Override
    protected int[] previewSizeDp() {
        return new int[]{230, 160};
    }

    @Override
    protected RemoteViews render(int id, Settings s, boolean preview) {
        return ClockProvider.render(this, id, s, preview);
    }

    @Override
    protected void buildOptions() {
        int style = settings.getInt("cstyle", 0);
        addDialogChoice(R.string.clock_style, "cstyle", new int[]{
                R.string.clock_d0, R.string.clock_d1, R.string.clock_d2, R.string.clock_d3,
                R.string.clock_d4, R.string.clock_d5, R.string.clock_d6, R.string.clock_d7,
                R.string.clock_a0, R.string.clock_a1, R.string.clock_a2, R.string.clock_a3},
                0, pos -> rebuildOptions());

        if (style == 7) {
            addSegmented(R.string.clock_position, "pos",
                    new int[]{R.string.pos_top, R.string.pos_middle, R.string.pos_bottom, R.string.pos_left},
                    1, null);
            addNote(R.string.clock_photo_note);
        }
        if (style == 5) {
            String[] names = new String[ZONES.length];
            for (int i = 0; i < ZONES.length; i++) {
                names[i] = ZONES[i].substring(ZONES[i].indexOf('/') + 1).replace('_', ' ');
            }
            addActionRow(R.string.clock_second_zone,
                    settings.getString("tz", "Europe/London").replaceFirst(".*/", "").replace('_', ' '), v ->
                    new MaterialAlertDialogBuilder(this).setTitle(R.string.clock_second_zone)
                            .setItems(names, (d, which) -> {
                                settings.put("tz", ZONES[which]);
                                rebuildOptions();
                            }).show());
        }
        if (style < 8) {
            addSection(R.string.colours);
            addSwatches(R.string.text_colour, "text", Palette.TEXT, 0xFFFFFFFF);
            if (style == 2 || style == 3 || style == 4) {
                addSwatches(R.string.highlight_colour, "accent", Palette.ACCENT, 0xFF7C5CFF);
            }
            addFontRow();
            addSlider(R.string.text_size, "scale", 70, 150, 10, 100, "%d%%");
            addSegmented(R.string.clock_hours, "hmode",
                    new int[]{R.string.hours_phone, R.string.hours_12, R.string.hours_24}, 0, null);
        } else {
            addSection(R.string.look);
            addSegmented(R.string.clock_face, "face",
                    new int[]{R.string.face_light, R.string.face_dark}, 0, null);
        }
        addBackgroundRows(true, style == 7 ? R.string.picture_clock : R.string.background_picture);
    }
}
