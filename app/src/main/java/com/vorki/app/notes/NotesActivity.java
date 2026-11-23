package com.vorki.app.notes;

import android.appwidget.AppWidgetProvider;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

import com.vorki.app.R;
import com.vorki.app.core.BaseWidgetActivity;
import com.vorki.app.core.Settings;
import com.vorki.app.core.Ui;
import com.vorki.app.core.WidgetBg;

public class NotesActivity extends BaseWidgetActivity {

    @Override
    protected int titleRes() {
        return R.string.notes_name;
    }

    @Override
    protected int hintRes() {
        return R.string.notes_hint;
    }

    @Override
    protected Class<? extends AppWidgetProvider> providerClass() {
        return NotesProvider.class;
    }

    @Override
    protected int[] previewSizeDp() {
        return new int[]{210, 210};
    }

    @Override
    protected RemoteViews render(int id, Settings s, boolean preview) {
        return NotesProvider.render(this, id, s, preview);
    }

    @Override
    protected void buildOptions() {
        boolean todo = settings.getInt("ntype", 0) == 1;
        addSegmented(R.string.note_type, "ntype",
                new int[]{R.string.type_sticky, R.string.type_todo}, 0, pos -> rebuildOptions());
        addTextField(R.string.note_title, "title", "", 40);
        if (todo) {
            addTextField(R.string.todo_items, "todo", "", 600, 5);
            addNote(R.string.todo_note);
        } else {
            addTextField(R.string.note_text, "note", "", 600, 5);
        }

        addSection(R.string.note_paper);
        addPresets();
        addFontRow();
        addSlider(R.string.text_size, "scale", 70, 150, 10, 100, "%d%%");
        addSection(R.string.background);
        addPictureRows(R.string.background_picture);
    }

    private void addPresets() {
        int[] names = {R.string.paper_yellow, R.string.paper_mint, R.string.paper_pink,
                R.string.paper_sky, R.string.paper_lavender, R.string.paper_dark,
                R.string.paper_lined, R.string.paper_grid};
        addDialogChoice(R.string.paper_style, "preset", names, 0, null);
        // A small strip of the paper colours, just for looks.
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = Ui.dp(this, 10);
        for (int i = 0; i < NotesProvider.PAPER.length; i++) {
            final int index = i;
            View chip = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.RECTANGLE);
            d.setCornerRadius(Ui.dp(this, 8));
            d.setColor(NotesProvider.PAPER[i]);
            d.setStroke(Ui.dp(this, 1), 0x55888888);
            chip.setBackground(d);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 36), 1f);
            lp.setMarginEnd(Ui.dp(this, 6));
            chip.setOnClickListener(v -> {
                settings.put("preset", index);
                rebuildOptions();
            });
            strip.addView(chip, lp);
        }
        options.addView(strip, sp);
    }

}
