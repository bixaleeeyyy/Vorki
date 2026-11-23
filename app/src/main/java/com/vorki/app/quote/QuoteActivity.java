package com.vorki.app.quote;

import android.appwidget.AppWidgetProvider;
import android.widget.RemoteViews;

import com.vorki.app.R;
import com.vorki.app.core.BaseWidgetActivity;
import com.vorki.app.core.Palette;
import com.vorki.app.core.Settings;

public class QuoteActivity extends BaseWidgetActivity {

    @Override
    protected int titleRes() {
        return R.string.quote_name;
    }

    @Override
    protected int hintRes() {
        return R.string.quote_hint;
    }

    @Override
    protected Class<? extends AppWidgetProvider> providerClass() {
        return QuoteProvider.class;
    }

    @Override
    protected int[] previewSizeDp() {
        return new int[]{230, 170};
    }

    @Override
    protected RemoteViews render(int id, Settings s, boolean preview) {
        return QuoteProvider.render(this, id, s, preview);
    }

    @Override
    protected void buildOptions() {
        int mode = settings.getInt("qmode", 0);
        addSegmented(R.string.quote_source, "qmode",
                new int[]{R.string.quote_builtin, R.string.quote_mine, R.string.quote_both}, 0,
                pos -> rebuildOptions());
        if (mode != 0) {
            addTextField(R.string.quote_own, "own", "", 1200, 5);
            addNote(R.string.quote_own_note);
        }
        addSwitch(R.string.quote_show_author, "showAuthor", true, null);

        addSection(R.string.colours);
        addSwatches(R.string.text_colour, "text", Palette.TEXT, 0xFFFFFFFF);
        addSwatches(R.string.highlight_colour, "accent", Palette.ACCENT, 0xFFFFB020);
        addFontRow();
        addSlider(R.string.text_size, "scale", 70, 150, 10, 100, "%d%%");
        addBackgroundRows(true, R.string.background_picture);
    }
}
