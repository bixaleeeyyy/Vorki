package com.vorki.app.apps;

import android.appwidget.AppWidgetProvider;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.vorki.app.R;
import com.vorki.app.core.BaseWidgetActivity;
import com.vorki.app.core.Palette;
import com.vorki.app.core.Settings;
import com.vorki.app.transparent.AppPickerActivity;

public class AppsActivity extends BaseWidgetActivity {

    private int pickingSlot = -1;

    private final ActivityResultLauncher<Intent> picker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && pickingSlot >= 0) {
                    String comp = result.getData().getStringExtra(AppPickerActivity.EXTRA_COMPONENT);
                    String label = result.getData().getStringExtra(AppPickerActivity.EXTRA_LABEL);
                    if (comp != null) {
                        settings.put("a" + pickingSlot, comp);
                        settings.put("a" + pickingSlot + "l", label == null ? "" : label);
                        rebuildOptions();
                    }
                }
            });

    @Override
    protected int titleRes() {
        return R.string.apps_name;
    }

    @Override
    protected int hintRes() {
        return R.string.apps_hint;
    }

    @Override
    protected Class<? extends AppWidgetProvider> providerClass() {
        return AppsProvider.class;
    }

    @Override
    protected int[] previewSizeDp() {
        return new int[]{260, 100};
    }

    @Override
    protected RemoteViews render(int id, Settings s, boolean preview) {
        return AppsProvider.render(this, id, s, preview);
    }

    @Override
    protected void buildOptions() {
        addSection(R.string.apps_list);
        for (int i = 0; i < AppsProvider.MAX; i++) {
            final int slot = i;
            String label = settings.getString("a" + i + "l", "");
            boolean has = !settings.getString("a" + i, "").isEmpty();
            View row = addActionRow(R.string.apps_slot,
                    has ? (label.isEmpty() ? getString(R.string.tap_to_choose) : label)
                            : getString(R.string.tap_to_choose), v -> {
                        pickingSlot = slot;
                        picker.launch(new Intent(this, AppPickerActivity.class));
                    });
            if (has) {
                row.setOnLongClickListener(v -> {
                    settings.put("a" + slot, "");
                    settings.put("a" + slot + "l", "");
                    rebuildOptions();
                    return true;
                });
            }
        }
        addNote(R.string.apps_note);
        addSwitch(R.string.apps_labels, "labels", true, null);

        addSection(R.string.colours);
        addSwatches(R.string.text_colour, "text", Palette.TEXT, 0xFFFFFFFF);
        addFontRow();
        addSlider(R.string.text_size, "scale", 70, 150, 10, 100, "%d%%");
        addBackgroundRows(true, R.string.background_picture);
    }
}
