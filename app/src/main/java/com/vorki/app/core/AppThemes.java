package com.vorki.app.core;

import android.content.Context;

import com.vorki.app.R;
import com.vorki.app.VorkiApp;

/** The five colour palettes of the app. Aurora is the default. */
public final class AppThemes {

    private AppThemes() {
    }

    public static final String KEY_PALETTE = "palette";

    public static final int[] NAMES = {
            R.string.palette_aurora, R.string.palette_sunset, R.string.palette_mono,
            R.string.palette_ocean, R.string.palette_rose
    };

    private static final int[] STYLES = {
            R.style.Theme_Vorki_Aurora, R.style.Theme_Vorki_Sunset, R.style.Theme_Vorki_Mono,
            R.style.Theme_Vorki_Ocean, R.style.Theme_Vorki_Rose
    };

    /** Two colours per palette, used for the little previews in Settings. */
    public static final int[][] PREVIEW = {
            {0xFF14B8A6, 0xFF8B5CF6},
            {0xFFFF7A59, 0xFFFF5C8A},
            {0xFFC6F135, 0xFF8A8A8A},
            {0xFF2563EB, 0xFF0EA5E9},
            {0xFFFF5C9E, 0xFFD946EF}
    };

    public static int index(Context c) {
        int i = c.getSharedPreferences(VorkiApp.PREFS_APP, Context.MODE_PRIVATE)
                .getInt(KEY_PALETTE, 0);
        return Math.max(0, Math.min(STYLES.length - 1, i));
    }

    public static void set(Context c, int i) {
        c.getSharedPreferences(VorkiApp.PREFS_APP, Context.MODE_PRIVATE)
                .edit().putInt(KEY_PALETTE, i).apply();
    }

    public static int themeRes(Context c) {
        return STYLES[index(c)];
    }
}
