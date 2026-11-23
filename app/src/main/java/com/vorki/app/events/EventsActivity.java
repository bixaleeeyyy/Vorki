package com.vorki.app.events;

import android.appwidget.AppWidgetProvider;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vorki.app.R;
import com.vorki.app.core.BaseWidgetActivity;
import com.vorki.app.core.Palette;
import com.vorki.app.core.Settings;
import com.vorki.app.core.Ui;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class EventsActivity extends BaseWidgetActivity {

    @Override
    protected int titleRes() {
        return R.string.events_name;
    }

    @Override
    protected int hintRes() {
        return R.string.events_hint;
    }

    @Override
    protected Class<? extends AppWidgetProvider> providerClass() {
        return EventsProvider.class;
    }

    @Override
    protected int[] previewSizeDp() {
        return new int[]{230, 200};
    }

    @Override
    protected RemoteViews render(int id, Settings s, boolean preview) {
        return EventsProvider.render(this, id, s, preview);
    }

    @Override
    protected void buildOptions() {
        addTextField(R.string.events_title_hint, "title", "", 30);

        addSection(R.string.events_list);
        for (int i = 0; i < EventsProvider.MAX; i++) {
            final int index = i;
            String name = settings.getString("e" + i + "n", "").trim();
            LocalDate d = EventsProvider.dateOf(settings, i);
            String value = (name.isEmpty() || d == null)
                    ? getString(R.string.event_empty_slot)
                    : name + " \u00B7 " + d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
            addActionRow(R.string.event_slot, value, v -> editEvent(index));
        }
        addSwitch(R.string.events_hide_past, "hidePast", true, null);

        addSection(R.string.colours);
        addSwatches(R.string.text_colour, "text", Palette.TEXT, 0xFFFFFFFF);
        addSwatches(R.string.highlight_colour, "accent", Palette.ACCENT, 0xFFFF5C8A);
        addFontRow();
        addSlider(R.string.text_size, "scale", 70, 150, 10, 100, "%d%%");
        addBackgroundRows(true, R.string.background_picture);
    }

    private void editEvent(int index) {
        LinearLayout box = new LinearLayout(this);
        box.setPadding(Ui.dp(this, 24), Ui.dp(this, 8), Ui.dp(this, 24), 0);
        EditText input = new EditText(this);
        input.setHint(R.string.event_name_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setSingleLine(true);
        input.setText(settings.getString("e" + index + "n", ""));
        box.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.event_slot)
                .setView(box)
                .setPositiveButton(R.string.pick_date, (dlg, w) -> pickDate(index, input.getText().toString().trim()))
                .setNeutralButton(R.string.clear, (dlg, w) -> {
                    EventsProvider.setEvent(settings, index, "", null);
                    rebuildOptions();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void pickDate(int index, String name) {
        LocalDate start = EventsProvider.dateOf(settings, index);
        if (start == null) {
            start = LocalDate.now();
        }
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.pick_date)
                .setSelection(start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                .build();
        picker.addOnPositiveButtonClickListener(sel -> {
            LocalDate d = Instant.ofEpochMilli(sel).atZone(ZoneOffset.UTC).toLocalDate();
            EventsProvider.setEvent(settings, index, name, d);
            rebuildOptions();
        });
        picker.show(getSupportFragmentManager(), "event_date");
    }
}
