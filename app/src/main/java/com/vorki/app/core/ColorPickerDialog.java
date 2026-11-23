package com.vorki.app.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.vorki.app.R;

import java.util.function.IntConsumer;

/** A small colour picker: three sliders and a preview. */
public final class ColorPickerDialog {

    private ColorPickerDialog() {
    }

    public static void show(Context c, int start, IntConsumer onPicked) {
        float[] hsv = new float[3];
        Color.colorToHSV(start | 0xFF000000, hsv);

        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(c, 24);
        box.setPadding(pad, Ui.dp(c, 12), pad, 0);

        View preview = new View(c);
        box.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(c, 56)));

        Slider hue = slider(c, box, R.string.picker_hue, 0, 360, hsv[0]);
        Slider sat = slider(c, box, R.string.picker_saturation, 0, 100, hsv[1] * 100);
        Slider val = slider(c, box, R.string.picker_brightness, 0, 100, hsv[2] * 100);

        Runnable update = () -> {
            hsv[0] = hue.getValue();
            hsv[1] = sat.getValue() / 100f;
            hsv[2] = val.getValue() / 100f;
            GradientDrawable d = new GradientDrawable();
            d.setCornerRadius(Ui.dp(c, 16));
            d.setColor(Color.HSVToColor(hsv));
            d.setStroke(Ui.dp(c, 1), 0x55888888);
            preview.setBackground(d);
        };
        update.run();
        hue.addOnChangeListener((s, v, u) -> update.run());
        sat.addOnChangeListener((s, v, u) -> update.run());
        val.addOnChangeListener((s, v, u) -> update.run());

        new MaterialAlertDialogBuilder(c)
                .setTitle(R.string.custom_colour)
                .setView(box)
                .setPositiveButton(R.string.ok, (d, w) -> onPicked.accept(Color.HSVToColor(hsv)))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static Slider slider(Context c, LinearLayout box, int label, float from, float to,
                                 float value) {
        TextView t = new TextView(c);
        t.setText(label);
        t.setTextSize(13);
        t.setPadding(0, Ui.dp(c, 12), 0, 0);
        box.addView(t);
        Slider s = new Slider(c);
        s.setValueFrom(from);
        s.setValueTo(to);
        s.setValue(Math.max(from, Math.min(to, value)));
        box.addView(s, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return s;
    }
}
