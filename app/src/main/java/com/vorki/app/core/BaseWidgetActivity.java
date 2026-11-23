package com.vorki.app.core;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.StringRes;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.vorki.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The settings screen shared by every widget.
 *
 * It works in two ways:
 *  - From the main screen ("template mode"): you edit the look that NEW widgets start with.
 *  - For one placed widget (from the list on this screen, or the launcher's Reconfigure).
 *
 * A new widget only needs to say how to build its options and how to draw itself.
 * Everything else (preview, saving, "Add to home screen", the list of placed widgets,
 * colours, fonts, background pictures) is done here.
 */
public abstract class BaseWidgetActivity extends VorkiActivity {

    protected Settings settings;
    protected int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    protected boolean templateMode;
    protected LinearLayout options;

    private FrameLayout previewHolder;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable saveRunnable = this::persist;

    private Consumer<List<String>> pendingPick;
    private int pendingPickMaxPx = 900;

    private final ActivityResultLauncher<PickVisualMediaRequest> singlePicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    importUris(Collections.singletonList(uri));
                }
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> multiPicker =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(12), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    importUris(uris);
                }
            });

    // ---- what a widget screen must provide ----------------------------------------------

    @StringRes
    protected abstract int titleRes();

    @StringRes
    protected abstract int hintRes();

    protected abstract Class<? extends AppWidgetProvider> providerClass();

    /**
     * Draws the widget. Used for the live preview (preview = true) and for the real widget.
     * Do not attach click actions when preview is true.
     */
    protected abstract RemoteViews render(int id, Settings s, boolean preview);

    /** Adds the setting rows (use the add... helpers below). */
    protected abstract void buildOptions();

    /** Size of the preview box in dp: {width, height}. */
    protected int[] previewSizeDp() {
        return new int[]{200, 200};
    }

    /** Pushes the settings to a widget that is already on the home screen. */
    protected void updatePlacedWidget(int id, Settings s) {
        try {
            AppWidgetManager.getInstance(this).updateAppWidget(id, render(id, s, false));
        } catch (RuntimeException ignored) {
        }
    }

    // ---- lifecycle ----------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_screen);
        Ui.applyInsets(findViewById(R.id.screen_root));

        widgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        templateMode = widgetId == AppWidgetManager.INVALID_APPWIDGET_ID;

        if (templateMode) {
            settings = Settings.load(this, Settings.TEMPLATE_ID);
        } else {
            settings = Settings.loadForWidget(this, widgetId);
            // Needed by launchers that open this screen as the widget's configure screen.
            Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            setResult(RESULT_OK, result);
        }

        ((TextView) findViewById(R.id.screen_title)).setText(titleRes());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tv_hint)).setText(
                templateMode ? hintRes() : R.string.hint_reconfigure);

        previewHolder = findViewById(R.id.preview_holder);
        int[] size = previewSizeDp();
        previewHolder.setLayoutParams(new FrameLayout.LayoutParams(
                Ui.dp(this, size[0]), Ui.dp(this, size[1]), Gravity.CENTER));

        options = findViewById(R.id.options);
        buildOptions();
        setupButtons();
        refreshPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showPlacedWidgets();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(saveRunnable);
        persist();
        BgImages.IO.execute(() -> BgImages.cleanup(getApplicationContext()));
        super.onPause();
    }

    private void setupButtons() {
        MaterialButton primary = findViewById(R.id.btn_primary);
        MaterialButton secondary = findViewById(R.id.btn_secondary);
        if (templateMode) {
            primary.setText(R.string.add_to_home);
            primary.setOnClickListener(v -> {
                persist();
                pinWidget();
            });
            secondary.setOnClickListener(v -> {
                persist();
                applyToPlacedWidgets();
            });
        } else {
            primary.setText(R.string.done);
            primary.setOnClickListener(v -> finish());
            secondary.setVisibility(View.GONE);
        }
    }

    private void pinWidget() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        if (manager.isRequestPinAppWidgetSupported()) {
            manager.requestPinAppWidget(new ComponentName(this, providerClass()), null, null);
        } else {
            Toast.makeText(this, R.string.pin_not_supported, Toast.LENGTH_LONG).show();
        }
    }

    private void applyToPlacedWidgets() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        int[] ids = manager.getAppWidgetIds(new ComponentName(this, providerClass()));
        for (int id : ids) {
            settings.copy().save(this, id);
            updatePlacedWidget(id, settings.copy());
        }
        Toast.makeText(this, getString(R.string.applied_to_n, ids.length), Toast.LENGTH_SHORT).show();
    }

    /** The "Your widgets on the home screen" list, with an Edit button for each. */
    private void showPlacedWidgets() {
        View section = findViewById(R.id.placed_section);
        LinearLayout list = findViewById(R.id.placed_list);
        if (!templateMode) {
            section.setVisibility(View.GONE);
            return;
        }
        int[] ids = AppWidgetManager.getInstance(this)
                .getAppWidgetIds(new ComponentName(this, providerClass()));
        list.removeAllViews();
        section.setVisibility(ids.length == 0 ? View.GONE : View.VISIBLE);
        for (int i = 0; i < ids.length; i++) {
            final int id = ids[i];
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(this, 10), Ui.dp(this, 2), Ui.dp(this, 2), Ui.dp(this, 2));

            TextView label = new TextView(this);
            label.setText(getString(R.string.placed_widget_n, i + 1));
            label.setTextSize(15);
            label.setTextColor(MaterialColors.getColor(this, R.attr.vkInk, Color.BLACK));
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            MaterialButton edit = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            edit.setText(R.string.edit);
            edit.setOnClickListener(v -> {
                Intent intent = new Intent(this, getClass());
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
                startActivity(intent);
            });
            row.addView(edit);
            list.addView(row);
        }
    }

    // ---- preview and saving -------------------------------------------------------------

    protected void refreshPreview() {
        previewHolder.removeAllViews();
        try {
            RemoteViews views = render(widgetId, settings, true);
            // Plain (non-AppCompat) inflation, exactly like the home screen does it.
            View view = views.apply(getApplicationContext(), previewHolder);
            previewHolder.addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } catch (RuntimeException e) {
            TextView error = new TextView(this);
            error.setText(getString(R.string.preview_failed, String.valueOf(e.getMessage())));
            error.setTextSize(11);
            error.setTextColor(0xFFB00020);
            previewHolder.addView(error);
        }
    }

    /** Call after any setting changed. */
    protected void changed() {
        refreshPreview();
        handler.removeCallbacks(saveRunnable);
        handler.postDelayed(saveRunnable, 300);
    }

    /** Rebuilds all option rows (after a change that shows or hides other rows). */
    protected void rebuildOptions() {
        options.post(() -> {
            options.removeAllViews();
            buildOptions();
            changed();
        });
    }

    private void persist() {
        if (templateMode) {
            settings.save(this, Settings.TEMPLATE_ID);
        } else {
            settings.save(this, widgetId);
            updatePlacedWidget(widgetId, settings.copy());
        }
    }

    // ---- picking pictures ---------------------------------------------------------------

    /** Opens the gallery picker. The chosen pictures are copied into the app, then handed back. */
    protected void pickImages(boolean multiple, int maxPx, Consumer<List<String>> done) {
        pendingPick = done;
        pendingPickMaxPx = maxPx;
        PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build();
        if (multiple) {
            multiPicker.launch(request);
        } else {
            singlePicker.launch(request);
        }
    }

    private void importUris(List<Uri> uris) {
        final Consumer<List<String>> callback = pendingPick;
        final int maxPx = pendingPickMaxPx;
        if (callback == null) {
            return;
        }
        Toast.makeText(this, R.string.importing_pictures, Toast.LENGTH_SHORT).show();
        BgImages.IO.execute(() -> {
            List<String> names = new ArrayList<>();
            for (Uri uri : uris) {
                String name = BgImages.importOne(getApplicationContext(), uri, maxPx);
                if (name != null) {
                    names.add(name);
                }
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (names.isEmpty()) {
                    Toast.makeText(this, R.string.picture_failed, Toast.LENGTH_LONG).show();
                } else {
                    callback.accept(names);
                }
            });
        });
    }

    // ---- row helpers --------------------------------------------------------------------

    private View inflateRow(int layout, @StringRes int titleRes) {
        View row = getLayoutInflater().inflate(layout, options, false);
        TextView title = row.findViewById(R.id.row_title);
        if (title != null && titleRes != 0) {
            title.setText(titleRes);
        }
        options.addView(row);
        return row;
    }

    protected void addSection(@StringRes int titleRes) {
        inflateRow(R.layout.row_section, titleRes);
    }

    protected void addNote(@StringRes int textRes) {
        inflateRow(R.layout.row_note, textRes);
    }

    /** A grid of colour circles plus a "custom colour" circle. */
    protected void addSwatches(@StringRes int titleRes, String key, int[] colors, int def) {
        View row = inflateRow(R.layout.row_swatches, titleRes);
        LinearLayout holder = row.findViewById(R.id.swatch_holder);
        int ring = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
        Runnable[] redraw = new Runnable[1];
        redraw[0] = () -> {
            holder.removeAllViews();
            int current = settings.getInt(key, def);
            boolean custom = true;
            for (int color : colors) {
                if (color == current) {
                    custom = false;
                }
            }
            final int perRow = 6;
            LinearLayout line = null;
            for (int i = 0; i <= colors.length; i++) {
                if (i % perRow == 0) {
                    line = new LinearLayout(this);
                    line.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.topMargin = Ui.dp(this, 8);
                    holder.addView(line, lp);
                }
                View swatch = new View(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40));
                lp.setMarginEnd(Ui.dp(this, 12));
                swatch.setLayoutParams(lp);
                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                if (i < colors.length) {
                    int color = colors[i];
                    circle.setColor(color);
                    circle.setStroke(color == current ? Ui.dp(this, 3) : Ui.dp(this, 1),
                            color == current ? ring : 0x44888888);
                    swatch.setContentDescription(String.format("#%06X", color & 0xFFFFFF));
                    swatch.setOnClickListener(v -> {
                        settings.put(key, color);
                        redraw[0].run();
                        changed();
                    });
                } else {
                    // The last circle: a rainbow that opens the custom picker.
                    circle.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                    circle.setColors(custom
                            ? new int[]{current, current}
                            : new int[]{0xFFFF5C8A, 0xFFFFB020, 0xFF34D399, 0xFF22D3EE, 0xFF7C5CFF});
                    circle.setStroke(custom ? Ui.dp(this, 3) : Ui.dp(this, 1),
                            custom ? ring : 0x44888888);
                    swatch.setContentDescription(getString(R.string.custom_colour));
                    swatch.setOnClickListener(v -> ColorPickerDialog.show(this, current, picked -> {
                        settings.put(key, picked);
                        redraw[0].run();
                        changed();
                    }));
                }
                swatch.setBackground(circle);
                line.addView(swatch);
            }
        };
        redraw[0].run();
    }

    /** A slider with whole numbers. (to - from) must be a multiple of step. */
    protected void addSlider(@StringRes int titleRes, String key, int from, int to, int step,
                             int def, String valueFormat) {
        View row = inflateRow(R.layout.row_slider, titleRes);
        TextView valueView = row.findViewById(R.id.row_value);
        Slider slider = row.findViewById(R.id.slider);
        slider.setValueFrom(from);
        slider.setValueTo(to);
        slider.setStepSize(step);

        int current = Math.max(from, Math.min(to, settings.getInt(key, def)));
        current = from + Math.round((current - from) / (float) step) * step;
        slider.setValue(current);
        valueView.setText(String.format(Locale.getDefault(), valueFormat, current));

        slider.addOnChangeListener((s, value, fromUser) -> {
            int v = Math.round(value);
            valueView.setText(String.format(Locale.getDefault(), valueFormat, v));
            if (fromUser) {
                settings.put(key, v);
                changed();
            }
        });
    }

    /** Connected buttons - pick one. Saves the position (0, 1, 2 ...). */
    protected View addSegmented(@StringRes int titleRes, String key, int[] labels, int def,
                                IntConsumer onPick) {
        View row = inflateRow(R.layout.row_segmented, titleRes);
        MaterialButtonToggleGroup group = row.findViewById(R.id.toggle_group);
        int[] ids = new int[labels.length];
        for (int i = 0; i < labels.length; i++) {
            MaterialButton button = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            button.setId(View.generateViewId());
            button.setText(labels[i]);
            button.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            group.addView(button);
            ids[i] = button.getId();
        }
        int current = Math.max(0, Math.min(labels.length - 1, settings.getInt(key, def)));
        group.check(ids[current]);
        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == checkedId) {
                    settings.put(key, i);
                    changed();
                    if (onPick != null) {
                        onPick.accept(i);
                    }
                }
            }
        });
        return row;
    }

    /** A list of radio options - pick one. Saves the position (0, 1, 2 ...). */
    protected View addChoices(@StringRes int titleRes, String key, int[] labels, int def,
                              IntConsumer onPick) {
        View row = inflateRow(R.layout.row_choices, titleRes);
        RadioGroup group = row.findViewById(R.id.radio_group);
        int[] ids = new int[labels.length];
        for (int i = 0; i < labels.length; i++) {
            MaterialRadioButton button = new MaterialRadioButton(this);
            button.setId(View.generateViewId());
            button.setText(labels[i]);
            button.setTextSize(15);
            group.addView(button, new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48)));
            ids[i] = button.getId();
        }
        int current = Math.max(0, Math.min(labels.length - 1, settings.getInt(key, def)));
        group.check(ids[current]);
        group.setOnCheckedChangeListener((g, checkedId) -> {
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == checkedId) {
                    settings.put(key, i);
                    changed();
                    if (onPick != null) {
                        onPick.accept(i);
                    }
                }
            }
        });
        return row;
    }

    /** A tappable row that opens a list dialog - good for long lists (fonts, clock styles). */
    protected View addDialogChoice(@StringRes int titleRes, String key, int[] labels, int def,
                                   IntConsumer onPick) {
        int start = Math.max(0, Math.min(labels.length - 1, settings.getInt(key, def)));
        View row = addActionRow(titleRes, getString(labels[start]), null);
        row.setOnClickListener(v -> {
            String[] names = new String[labels.length];
            for (int i = 0; i < labels.length; i++) {
                names[i] = getString(labels[i]);
            }
            int now = Math.max(0, Math.min(labels.length - 1, settings.getInt(key, def)));
            new MaterialAlertDialogBuilder(this)
                    .setTitle(titleRes)
                    .setSingleChoiceItems(names, now, (dialog, which) -> {
                        settings.put(key, which);
                        setRowValue(row, names[which]);
                        changed();
                        dialog.dismiss();
                        if (onPick != null) {
                            onPick.accept(which);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
        return row;
    }

    /** An on/off switch. */
    protected MaterialSwitch addSwitch(@StringRes int titleRes, String key, boolean def,
                                       Consumer<Boolean> onToggle) {
        View row = inflateRow(R.layout.row_switch, 0);
        MaterialSwitch toggle = (MaterialSwitch) row;
        toggle.setText(titleRes);
        toggle.setChecked(settings.getBool(key, def));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            settings.put(key, checked);
            changed();
            if (onToggle != null) {
                onToggle.accept(checked);
            }
        });
        return toggle;
    }

    /** A text box. Use lines > 1 for a multi-line box. */
    protected TextInputEditText addTextField(@StringRes int hintRes, String key, String def,
                                             int maxLength, int lines) {
        View row = inflateRow(R.layout.row_text, 0);
        TextInputLayout layout = (TextInputLayout) row;
        layout.setHint(hintRes);
        TextInputEditText input = row.findViewById(R.id.input);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        if (lines > 1) {
            input.setSingleLine(false);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            input.setMinLines(lines);
            input.setMaxLines(lines + 4);
            input.setGravity(Gravity.TOP | Gravity.START);
        }
        input.setText(settings.getString(key, def));
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                settings.put(key, s.toString());
                changed();
            }
        });
        return input;
    }

    protected TextInputEditText addTextField(@StringRes int hintRes, String key, String def,
                                             int maxLength) {
        return addTextField(hintRes, key, def, maxLength, 1);
    }

    /** A tappable row with a title and a value underneath. Returns the row. */
    protected View addActionRow(@StringRes int titleRes, String value, View.OnClickListener click) {
        View row = inflateRow(R.layout.row_action, titleRes);
        setRowValue(row, value);
        if (click != null) {
            row.setOnClickListener(click);
        }
        return row;
    }

    protected void setRowValue(View row, String value) {
        ((TextView) row.findViewById(R.id.row_value)).setText(value);
    }

    // ---- ready-made groups --------------------------------------------------------------

    /** Font row (all widgets with text use this). */
    protected void addFontRow() {
        addDialogChoice(R.string.font, FontStyles.KEY, FontStyles.LABELS, 0, null);
    }

    /** Background colour, opacity, and a picture from the gallery with a dim slider. */
    protected void addBackgroundRows(boolean withColour, int pictureLabel) {
        addSection(R.string.background);
        if (withColour) {
            addSwatches(R.string.background_colour, WidgetBg.K_BG, Palette.BACKGROUND, 0xFF12142B);
            addSlider(R.string.background_opacity, WidgetBg.K_OPACITY, 0, 100, 5, 40, "%d%%");
        }
        addPictureRows(pictureLabel);
    }

    /** "Background picture" row: choose / change, remove, and dim. */
    protected void addPictureRows(@StringRes int label) {
        boolean has = WidgetBg.hasPicture(settings);
        addActionRow(label, getString(has ? R.string.picture_chosen : R.string.picture_none), v ->
                pickImages(false, 800, names -> {
                    settings.put(WidgetBg.K_PIC, names.get(0));
                    rebuildOptions();
                }));
        if (has) {
            addActionRow(R.string.picture_remove, getString(R.string.picture_remove_sub), v -> {
                settings.put(WidgetBg.K_PIC, "");
                rebuildOptions();
            });
            addSlider(R.string.picture_dim, WidgetBg.K_DIM, 0, 80, 5, 30, "%d%%");
        }
    }
}
