package com.vorki.app.transparent;

import android.app.Activity;
import android.app.SearchManager;
import android.app.admin.DevicePolicyManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.vorki.app.R;
import com.vorki.app.core.Settings;

/**
 * Invisible helper screen. A tap on the Transparent widget opens it for a split second;
 * on the second tap within the chosen time it does the chosen action.
 * (Android only lets an app start other apps from a screen the user just touched.)
 */
public class TapActivity extends Activity {

    static final String RUNTIME_PREFS = "vorki_runtime";

    static final int ACTION_NOTHING = 0;
    static final int ACTION_LOCK = 1;
    static final int ACTION_APP = 2;
    static final int ACTION_FLASH = 3;
    static final int ACTION_SEARCH = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int id = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            handleTap(id);
        }
        finish();
    }

    private void handleTap(int id) {
        Settings s = Settings.load(this, id);
        SharedPreferences runtime = getSharedPreferences(RUNTIME_PREFS, Context.MODE_PRIVATE);
        long last = runtime.getLong("tap_" + id, 0L);
        long now = System.currentTimeMillis();
        int delay = s.getInt("delay", 500);

        if (last != 0L && now - last <= delay) {
            runtime.edit().putLong("tap_" + id, 0L).apply();
            perform(s.getInt("action", ACTION_NOTHING), s.getString("app", ""));
        } else {
            runtime.edit().putLong("tap_" + id, now).apply();
        }
    }

    private void perform(int action, String app) {
        switch (action) {
            case ACTION_LOCK:
                lockScreen();
                break;
            case ACTION_APP:
                openApp(app);
                break;
            case ACTION_FLASH:
                if (!FlashHelper.toggle(this)) {
                    Toast.makeText(this, R.string.flash_not_available, Toast.LENGTH_LONG).show();
                }
                break;
            case ACTION_SEARCH:
                openSearch();
                break;
            default:
                break;
        }
    }

    private void lockScreen() {
        DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);
        if (dpm != null && dpm.isAdminActive(admin)) {
            dpm.lockNow();
            return;
        }
        // Not allowed yet: ask once.
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_explanation));
        startActivity(intent);
    }

    private void openApp(String component) {
        int slash = component.indexOf('/');
        if (slash <= 0) {
            Toast.makeText(this, R.string.no_app_chosen, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(new ComponentName(
                    component.substring(0, slash), component.substring(slash + 1)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.app_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void openSearch() {
        try {
            Intent intent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.search_not_available, Toast.LENGTH_LONG).show();
        }
    }
}
