package com.vorki.app.core;

/** Colour choices shown as swatches on the settings screens (plus a custom picker). */
public final class Palette {

    private Palette() {
    }

    public static final int[] COLORS = {
            0xFFFFFFFF, 0xFFE5E7EB, 0xFF9CA3AF, 0xFF4B5563, 0xFF12142B, 0xFF000000,
            0xFFEF4444, 0xFFF97316, 0xFFFFB020, 0xFFFFE08A, 0xFFA3E635, 0xFF34D399,
            0xFF14B8A6, 0xFF22D3EE, 0xFF0EA5E9, 0xFF2563EB, 0xFF7C5CFF, 0xFFA855F7,
            0xFFD946EF, 0xFFFF5C8A
    };

    public static final int[] TEXT = COLORS;
    public static final int[] ACCENT = COLORS;
    public static final int[] BACKGROUND = COLORS;
    public static final int[] OUTLINE = COLORS;
}
