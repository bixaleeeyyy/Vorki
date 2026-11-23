package com.vorki.app.core;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.text.style.TypefaceSpan;
import android.widget.RemoteViews;

import com.vorki.app.R;

/** The font choices. They are fonts already on every Android phone, so nothing is bundled. */
public final class FontStyles {

    private FontStyles() {
    }

    public static final String KEY = "font";

    public static final int[] LABELS = {
            R.string.font_default, R.string.font_light, R.string.font_medium,
            R.string.font_condensed, R.string.font_serif, R.string.font_mono,
            R.string.font_handwriting, R.string.font_casual
    };

    public static final String[] FAMILIES = {
            "sans-serif", "sans-serif-light", "sans-serif-medium", "sans-serif-condensed",
            "serif", "monospace", "cursive", "casual"
    };

    public static int index(Settings s) {
        return Math.max(0, Math.min(FAMILIES.length - 1, s.getInt(KEY, 0)));
    }

    /** Sets the text of a TextView in the chosen font. */
    public static void text(RemoteViews rv, int viewId, CharSequence text, int font) {
        SpannableString span = new SpannableString(text == null ? "" : text);
        span.setSpan(new TypefaceSpan(FAMILIES[Math.max(0, Math.min(FAMILIES.length - 1, font))]),
                0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        rv.setTextViewText(viewId, span);
    }

    /** Same, optionally crossed out (done to-do items). */
    public static void text(RemoteViews rv, int viewId, CharSequence text, int font, boolean strike) {
        SpannableString span = new SpannableString(text == null ? "" : text);
        span.setSpan(new TypefaceSpan(FAMILIES[Math.max(0, Math.min(FAMILIES.length - 1, font))]),
                0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (strike) {
            span.setSpan(new StrikethroughSpan(), 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        rv.setTextViewText(viewId, span);
    }
}
