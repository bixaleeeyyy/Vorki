package com.vorki.app.calendar;

import android.appwidget.AppWidgetProvider;
import android.view.View;
import android.widget.RemoteViews;

import com.vorki.app.R;
import com.vorki.app.core.BaseWidgetActivity;
import com.vorki.app.core.Palette;
import com.vorki.app.core.Settings;

public class CalendarActivity extends BaseWidgetActivity {

    private View weekRow;

    @Override
    protected int titleRes() {
        return R.string.calendar_name;
    }

    @Override
    protected int hintRes() {
        return R.string.calendar_hint;
    }

    @Override
    protected Class<? extends AppWidgetProvider> providerClass() {
        return CalendarProvider.class;
    }

    @Override
    protected int[] previewSizeDp() {
        return new int[]{230, 215};
    }

    @Override
    protected RemoteViews render(int id, Settings s, boolean preview) {
        return CalendarRenderer.render(this, id, s, preview);
    }

    @Override
    protected void buildOptions() {
        addSegmented(R.string.cal_style, "style",
                new int[]{R.string.style_date, R.string.style_full, R.string.style_month},
                CalendarRenderer.STYLE_MONTH,
                position -> weekRow.setVisibility(
                        position == CalendarRenderer.STYLE_MONTH ? View.VISIBLE : View.GONE));

        addSection(R.string.colours);
        addSwatches(R.string.text_colour, "text", Palette.TEXT, 0xFFFFFFFF);
        addSwatches(R.string.highlight_colour, "accent", Palette.ACCENT, 0xFF7C5CFF);
        addFontRow();
        addSection(R.string.background);
        addSwatches(R.string.background_colour, "bg", Palette.BACKGROUND, 0xFF0B0D17);
        addSlider(R.string.background_opacity, "opacity", 0, 100, 5, 75, "%d%%");
        addPictureRows(R.string.background_picture);

        addSection(R.string.size_and_layout);
        addSlider(R.string.text_size, "scale", 70, 150, 10, 100, "%d%%");
        weekRow = addSegmented(R.string.week_starts, "week",
                new int[]{R.string.sunday, R.string.monday}, 0, null);
        weekRow.setVisibility(settings.getInt("style", CalendarRenderer.STYLE_MONTH)
                == CalendarRenderer.STYLE_MONTH ? View.VISIBLE : View.GONE);
    }
}
