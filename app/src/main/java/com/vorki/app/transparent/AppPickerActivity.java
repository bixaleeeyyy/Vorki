package com.vorki.app.transparent;

import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.vorki.app.core.VorkiActivity;

import com.vorki.app.R;
import com.vorki.app.core.Ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lets you pick an app to open. Returns "package/activity" and the app's name. */
public class AppPickerActivity extends VorkiActivity {

    public static final String EXTRA_COMPONENT = "component";
    public static final String EXTRA_LABEL = "label";

    private static final class Entry {
        final String label;
        final String packageName;
        final String component;
        final Drawable icon;

        Entry(String label, String packageName, String component, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.component = component;
            this.icon = icon;
        }
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Entry> allApps = new ArrayList<>();
    private final List<Entry> shown = new ArrayList<>();
    private ArrayAdapter<Entry> adapter;
    private View progress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_picker);
        Ui.applyInsets(findViewById(R.id.screen_root));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        progress = findViewById(R.id.progress);
        ListView list = findViewById(R.id.apps_list);
        adapter = new ArrayAdapter<Entry>(this, 0, shown) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = LayoutInflater.from(getContext()).inflate(R.layout.item_app, parent, false);
                }
                Entry entry = shown.get(position);
                ((TextView) view.findViewById(R.id.name)).setText(entry.label);
                ((TextView) view.findViewById(R.id.package_name)).setText(entry.packageName);
                ((ImageView) view.findViewById(R.id.icon)).setImageDrawable(entry.icon);
                return view;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            Entry entry = shown.get(position);
            Intent result = new Intent();
            result.putExtra(EXTRA_COMPONENT, entry.component);
            result.putExtra(EXTRA_LABEL, entry.label);
            setResult(RESULT_OK, result);
            finish();
        });

        ((TextView) findViewById(R.id.search)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilter(s.toString());
            }
        });

        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        final Context context = getApplicationContext();
        executor.execute(() -> {
            List<Entry> found = new ArrayList<>();
            LauncherApps apps = context.getSystemService(LauncherApps.class);
            if (apps != null) {
                try {
                    for (LauncherActivityInfo info : apps.getActivityList(null, Process.myUserHandle())) {
                        found.add(new Entry(
                                info.getLabel().toString(),
                                info.getApplicationInfo().packageName,
                                info.getComponentName().getPackageName() + "/"
                                        + info.getComponentName().getClassName(),
                                info.getBadgedIcon(0)));
                    }
                } catch (RuntimeException ignored) {
                }
            }
            Collections.sort(found, Comparator.comparing((Entry e) -> e.label.toLowerCase(Locale.ROOT)));
            runOnUiThread(() -> {
                allApps.clear();
                allApps.addAll(found);
                applyFilter("");
                progress.setVisibility(View.GONE);
            });
        });
    }

    private void applyFilter(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        shown.clear();
        for (Entry e : allApps) {
            if (q.isEmpty() || e.label.toLowerCase(Locale.ROOT).contains(q)) {
                shown.add(e);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
