package com.vorki.app;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import com.vorki.app.transparent.FlashHelper;

public class VorkiApp extends Application {

    public static final String PREFS_APP = "vorki_app";
    public static final String KEY_THEME = "theme"; // 0 = system, 1 = light, 2 = dark

    @Override
    public void onCreate() {
        super.onCreate();
        applyTheme(this);
        FlashHelper.register(this);
    }

    public static void applyTheme(Context context) {
        int mode = context.getSharedPreferences(PREFS_APP, MODE_PRIVATE).getInt(KEY_THEME, 0);
        if (mode == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (mode == 2) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
