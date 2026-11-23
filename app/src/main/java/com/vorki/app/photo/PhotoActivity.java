package com.vorki.app.photo;

import android.appwidget.AppWidgetProvider;
import android.widget.RemoteViews;

import com.vorki.app.R;
import com.vorki.app.core.BaseWidgetActivity;
import com.vorki.app.core.Palette;
import com.vorki.app.core.Settings;

public class PhotoActivity extends BaseWidgetActivity {

    @Override
    protected int titleRes() {
        return R.string.photo_name;
    }

    @Override
    protected int hintRes() {
        return R.string.photo_hint_screen;
    }

    @Override
    protected Class<? extends AppWidgetProvider> providerClass() {
        return PhotoProvider.class;
    }

    @Override
    protected int[] previewSizeDp() {
        return new int[]{200, 200};
    }

    @Override
    protected RemoteViews render(int id, Settings s, boolean preview) {
        return PhotoProvider.render(this, id, s, preview);
    }

    @Override
    protected void buildOptions() {
        int count = PhotoProvider.names(settings).length;
        addSection(R.string.photo_pictures);
        addActionRow(R.string.photo_add, getResources().getQuantityString(R.plurals.photo_count, count, count), v ->
                pickImages(true, 1000, picked -> {
                    StringBuilder sb = new StringBuilder(settings.getString("pics", ""));
                    int have = PhotoProvider.names(settings).length;
                    for (String n : picked) {
                        if (have >= 12) {
                            break;
                        }
                        if (sb.length() > 0) {
                            sb.append(',');
                        }
                        sb.append(n);
                        have++;
                    }
                    settings.put("pics", sb.toString());
                    rebuildOptions();
                }));
        if (count > 0) {
            addActionRow(R.string.photo_clear, getString(R.string.photo_clear_sub), v -> {
                settings.put("pics", "");
                rebuildOptions();
            });
        }
        addNote(R.string.photo_note);
        if (count > 1) {
            addDialogChoice(R.string.photo_change_every, "pint", new int[]{
                    R.string.every_hour, R.string.every_6_hours, R.string.every_day, R.string.tap_only}, 1, null);
        }

        addSection(R.string.look);
        addSegmented(R.string.photo_shape, "pshape",
                new int[]{R.string.shape_rounded, R.string.shape_square, R.string.shape_oval}, 0, null);
        addSlider(R.string.photo_border, "pborder", 0, 8, 1, 0, "%d dp");
        addSwatches(R.string.photo_border_colour, "pbcolor", Palette.COLORS, 0xFFFFFFFF);
    }
}
