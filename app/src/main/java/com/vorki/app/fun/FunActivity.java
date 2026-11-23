package com.vorki.app.fun;

import android.appwidget.AppWidgetProvider;
import android.widget.RemoteViews;

import com.vorki.app.R;
import com.vorki.app.core.BaseWidgetActivity;
import com.vorki.app.core.Palette;
import com.vorki.app.core.Settings;

public class FunActivity extends BaseWidgetActivity {

    @Override
    protected int titleRes() {
        return R.string.fun_name;
    }

    @Override
    protected int hintRes() {
        return R.string.fun_hint;
    }

    @Override
    protected Class<? extends AppWidgetProvider> providerClass() {
        return FunProvider.class;
    }

    @Override
    protected int[] previewSizeDp() {
        return new int[]{190, 170};
    }

    @Override
    protected RemoteViews render(int id, Settings s, boolean preview) {
        return FunProvider.render(this, id, s, preview);
    }

    @Override
    protected void buildOptions() {
        int type = settings.getInt("ftype", FunProvider.DICE);
        addDialogChoice(R.string.fun_type, "ftype", new int[]{
                R.string.fun_dice, R.string.fun_coin, R.string.fun_ball,
                R.string.fun_rps, R.string.fun_number, R.string.fun_picker}, 0, pos -> rebuildOptions());

        if (type == FunProvider.DICE) {
            addSegmented(R.string.dice_count, "dn",
                    new int[]{R.string.one_die, R.string.two_dice, R.string.three_dice}, 0, null);
        } else if (type == FunProvider.NUMBER) {
            addSlider(R.string.number_min, "rmin", 0, 100, 1, 1, "%d");
            addSlider(R.string.number_max, "rmax", 1, 1000, 1, 100, "%d");
        } else if (type == FunProvider.PICKER) {
            addTextField(R.string.picker_list, "plist", "", 400, 5);
            addNote(R.string.picker_note);
        }

        addSection(R.string.colours);
        addSwatches(R.string.text_colour, "text", Palette.TEXT, 0xFFFFFFFF);
        addSwatches(R.string.number_colour, "accent", Palette.ACCENT, 0xFF22D3EE);
        addFontRow();
        addSlider(R.string.text_size, "scale", 70, 150, 10, 100, "%d%%");
        addBackgroundRows(true, R.string.background_picture);
    }
}
