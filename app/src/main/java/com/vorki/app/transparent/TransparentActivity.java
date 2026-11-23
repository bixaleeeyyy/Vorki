package com.vorki.app.transparent;

import android.app.admin.DevicePolicyManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.vorki.app.R;
import com.vorki.app.core.BaseWidgetActivity;
import com.vorki.app.core.Palette;
import com.vorki.app.core.Settings;

public class TransparentActivity extends BaseWidgetActivity {

    private View appRow;

    private final ActivityResultLauncher<Intent> appPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String component = result.getData().getStringExtra(AppPickerActivity.EXTRA_COMPONENT);
                    String label = result.getData().getStringExtra(AppPickerActivity.EXTRA_LABEL);
                    if (component != null) {
                        settings.put("app", component);
                        settings.put("appLabel", label == null ? "" : label);
                        setRowValue(appRow, label == null ? component : label);
                        changed();
                    }
                }
            });

    @Override
    protected int titleRes() {
        return R.string.transparent_name;
    }

    @Override
    protected int hintRes() {
        return R.string.transparent_hint;
    }

    @Override
    protected Class<? extends AppWidgetProvider> providerClass() {
        return TransparentProvider.class;
    }

    @Override
    protected int[] previewSizeDp() {
        return new int[]{170, 90};
    }

    @Override
    protected RemoteViews render(int id, Settings s, boolean preview) {
        return TransparentRenderer.render(this, id, s, preview);
    }

    @Override
    protected void buildOptions() {
        addNote(R.string.transparent_note);

        addChoices(R.string.on_double_tap, "action",
                new int[]{R.string.act_nothing, R.string.act_lock, R.string.act_app,
                        R.string.act_flash, R.string.act_search},
                TapActivity.ACTION_NOTHING,
                position -> {
                    updateAppRow();
                    if (position == TapActivity.ACTION_LOCK) {
                        askForLockPermission();
                    }
                });

        String label = settings.getString("appLabel", "");
        appRow = addActionRow(R.string.app_to_open,
                label.isEmpty() ? getString(R.string.tap_to_choose) : label,
                v -> appPicker.launch(new Intent(this, AppPickerActivity.class)));
        updateAppRow();

        addSlider(R.string.tap_speed, "delay", 250, 900, 50, 500, "%d ms");

        addSection(R.string.look);
        addSwitch(R.string.show_outline, "outline", true, null);
        addSwatches(R.string.outline_colour, "outlineColor", Palette.OUTLINE, 0xB3FFFFFF);
        addSection(R.string.background);
        addPictureRows(R.string.picture_makes_visible);
    }

    private void updateAppRow() {
        boolean show = settings.getInt("action", TapActivity.ACTION_NOTHING) == TapActivity.ACTION_APP;
        appRow.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void askForLockPermission() {
        DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);
        if (dpm != null && !dpm.isAdminActive(admin)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_explanation));
            startActivity(intent);
        }
    }
}
