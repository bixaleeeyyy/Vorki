package com.vorki.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.google.android.material.button.MaterialButtonToggleGroup;
import com.vorki.app.core.AppThemes;
import com.vorki.app.core.Ui;
import com.vorki.app.core.VorkiActivity;

public class SettingsActivity extends VorkiActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Ui.applyInsets(findViewById(R.id.screen_root));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        MaterialButtonToggleGroup group = findViewById(R.id.theme_group);
        int mode = getSharedPreferences(VorkiApp.PREFS_APP, Context.MODE_PRIVATE)
                .getInt(VorkiApp.KEY_THEME, 0);
        group.check(mode == 1 ? R.id.theme_light : (mode == 2 ? R.id.theme_dark : R.id.theme_system));
        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            int chosen = checkedId == R.id.theme_light ? 1 : (checkedId == R.id.theme_dark ? 2 : 0);
            getSharedPreferences(VorkiApp.PREFS_APP, Context.MODE_PRIVATE)
                    .edit().putInt(VorkiApp.KEY_THEME, chosen).apply();
            VorkiApp.applyTheme(this);
        });

        buildPaletteChooser();

        String version = "";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        ((TextView) findViewById(R.id.about_version)).setText(getString(R.string.about_version, version));
    }

    /** Five little gradient circles - tap one to change the colours of the whole app. */
    private void buildPaletteChooser() {
        LinearLayout row = findViewById(R.id.palette_row);
        row.removeAllViews();
        int current = AppThemes.index(this);
        for (int i = 0; i < AppThemes.NAMES.length; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    Ui.dp(this, 72), LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(Ui.dp(this, 8));
            item.setLayoutParams(lp);

            View dot = new View(this);
            GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{AppThemes.PREVIEW[i][0], AppThemes.PREVIEW[i][1]});
            d.setShape(GradientDrawable.OVAL);
            d.setStroke(Ui.dp(this, i == current ? 3 : 1), i == current ? 0xFF888888 : 0x44888888);
            dot.setBackground(d);
            item.addView(dot, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 52)));

            TextView name = new TextView(this);
            name.setText(AppThemes.NAMES[i]);
            name.setTextSize(12);
            name.setGravity(Gravity.CENTER);
            name.setPadding(0, Ui.dp(this, 6), 0, 0);
            name.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                    this, R.attr.vkInk, 0xFF000000));
            item.addView(name);

            item.setOnClickListener(v -> {
                if (index != AppThemes.index(this)) {
                    AppThemes.set(this, index);
                    recreate();
                }
            });
            row.addView(item);
        }
    }
}
