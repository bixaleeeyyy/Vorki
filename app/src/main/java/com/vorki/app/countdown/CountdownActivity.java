package com.vorki.app.countdown;

import android.Manifest;
import android.appwidget.AppWidgetProvider;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.vorki.app.R;
import com.vorki.app.core.BaseWidgetActivity;
import com.vorki.app.core.Palette;
import com.vorki.app.core.Settings;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

public class CountdownActivity extends BaseWidgetActivity {

    private View dateRow;
    private View timeRow;
    private MaterialSwitch notifySwitch;

    private final ActivityResultLauncher<String> notificationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (!granted) {
                    notifySwitch.setChecked(false);
                    Toast.makeText(this, R.string.notification_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected int titleRes() {
        return R.string.counters_name;
    }

    @Override
    protected int hintRes() {
        return R.string.countdown_hint;
    }

    @Override
    protected Class<? extends AppWidgetProvider> providerClass() {
        return CountdownProvider.class;
    }

    @Override
    protected int[] previewSizeDp() {
        return new int[]{190, 190};
    }

    @Override
    protected RemoteViews render(int id, Settings s, boolean preview) {
        return CountdownRenderer.render(this, id, s, preview);
    }

    @Override
    protected void updatePlacedWidget(int id, Settings s) {
        super.updatePlacedWidget(id, s);
        CountdownProvider.scheduleDone(this, id, s);
    }

    @Override
    protected void buildOptions() {
        CountdownRenderer.ensureDefaults(settings);
        int type = settings.getInt("ctype", CountdownRenderer.TYPE_COUNTDOWN);

        addSegmented(R.string.counter_type, "ctype",
                new int[]{R.string.type_countdown, R.string.type_since, R.string.type_progress},
                CountdownRenderer.TYPE_COUNTDOWN, pos -> rebuildOptions());

        addTextField(R.string.cd_title_hint, "title", "", 30);

        if (type == CountdownRenderer.TYPE_COUNTDOWN) {
            addSection(R.string.cd_when);
            dateRow = addActionRow(R.string.cd_date, CountdownRenderer.dateLabel(settings), v -> pickDate());
            timeRow = addActionRow(R.string.cd_time, CountdownRenderer.timeLabel(settings), v -> pickTime());
            addNote(R.string.cd_time_note);
        } else if (type == CountdownRenderer.TYPE_SINCE) {
            addSection(R.string.since_section);
            dateRow = addActionRow(R.string.since_date, CountdownRenderer.sinceLabel(settings), v -> pickDate());
            addActionRow(R.string.since_reset, getString(R.string.since_reset_sub), v -> {
                CountdownRenderer.setSince(settings, java.time.LocalDate.now());
                setRowValue(dateRow, CountdownRenderer.sinceLabel(settings));
                changed();
            });
        } else {
            addSection(R.string.progress_section);
            addSegmented(R.string.progress_scope, "scope",
                    new int[]{R.string.scope_year, R.string.scope_month, R.string.scope_week}, 0, null);
        }

        addSection(R.string.colours);
        addSwatches(R.string.text_colour, "text", Palette.TEXT, 0xFFFFFFFF);
        addSwatches(R.string.number_colour, "accent", Palette.ACCENT, 0xFFFF5C8A);
        addFontRow();
        addSlider(R.string.text_size, "scale", 70, 150, 10, 100, "%d%%");
        addBackgroundRows(true, R.string.background_picture);

        if (type == CountdownRenderer.TYPE_COUNTDOWN) {
            addSection(R.string.cd_when_finished);
            addSegmented(R.string.cd_finish_look, "finish",
                    new int[]{R.string.finish_show, R.string.finish_hide}, 0, null);
            notifySwitch = addSwitch(R.string.cd_notify, "notify", false, checked -> {
                if (checked && Build.VERSION.SDK_INT >= 33
                        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            });
            addNote(R.string.cd_notify_note);
        }
    }

    private void pickDate() {
        final boolean since = settings.getInt("ctype", 0) == CountdownRenderer.TYPE_SINCE;
        long utcMillis = (since ? CountdownRenderer.sinceOf(settings) : CountdownRenderer.dateOf(settings))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.cd_date)
                .setSelection(utcMillis)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            LocalDate date = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
            if (since) {
                CountdownRenderer.setSince(settings, date);
                setRowValue(dateRow, CountdownRenderer.sinceLabel(settings));
            } else {
                settings.put("tY", date.getYear());
                settings.put("tM", date.getMonthValue());
                settings.put("tD", date.getDayOfMonth());
                setRowValue(dateRow, CountdownRenderer.dateLabel(settings));
            }
            changed();
        });
        picker.show(getSupportFragmentManager(), "date");
    }

    private void pickTime() {
        LocalTime time = CountdownRenderer.timeOf(settings);
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(DateFormat.is24HourFormat(this) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(time.getHour())
                .setMinute(time.getMinute())
                .setTitleText(R.string.cd_time)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            settings.put("tH", picker.getHour());
            settings.put("tMin", picker.getMinute());
            setRowValue(timeRow, CountdownRenderer.timeLabel(settings));
            changed();
        });
        picker.show(getSupportFragmentManager(), "time");
    }
}
