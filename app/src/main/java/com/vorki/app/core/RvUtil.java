package com.vorki.app.core;

import android.content.res.ColorStateList;
import android.os.Build;
import android.util.TypedValue;
import android.widget.RemoteViews;

import androidx.core.graphics.ColorUtils;

/** Small helpers for drawing widgets (RemoteViews). */
public final class RvUtil {

    private RvUtil() {
    }

    /** Gives a view a rounded/shaped background in the given colour (alpha included). */
    public static void tintBackground(RemoteViews rv, int viewId, int drawableRes, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rv.setInt(viewId, "setBackgroundResource", drawableRes);
            rv.setColorStateList(viewId, "setBackgroundTintList", ColorStateList.valueOf(color));
        } else {
            rv.setInt(viewId, "setBackgroundColor", color);
        }
    }

    public static void clearBackground(RemoteViews rv, int viewId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rv.setInt(viewId, "setBackgroundResource", 0);
        } else {
            rv.setInt(viewId, "setBackgroundColor", 0);
        }
    }

    /** Colour with an opacity in percent (0-100). */
    public static int withOpacity(int color, int percent) {
        int alpha = Math.max(0, Math.min(100, percent)) * 255 / 100;
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    /** Same colour but fainter - for secondary text. */
    public static int faded(int color) {
        return withOpacity(color, 70);
    }

    /** White or near-black, whichever is readable on top of the given colour. */
    public static int onColor(int background) {
        return ColorUtils.calculateLuminance(background) > 0.55 ? 0xFF12142B : 0xFFFFFFFF;
    }

    public static void textSize(RemoteViews rv, int viewId, float sp, int scalePercent) {
        rv.setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, sp * scalePercent / 100f);
    }
}
