package com.vorki.app.core;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/** Base of every screen: applies the palette the user chose. */
public abstract class VorkiActivity extends AppCompatActivity {

    private int appliedPalette = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        appliedPalette = AppThemes.index(this);
        setTheme(AppThemes.themeRes(this));
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appliedPalette != AppThemes.index(this)) {
            recreate();
        }
    }
}
