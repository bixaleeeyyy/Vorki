package com.vorki.app.core;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A small bag of settings for ONE widget (or for the "template" that new widgets start with).
 * Stored as JSON, so every widget can add its own keys without any other change.
 */
public final class Settings {

    private static final String FILE = "vorki_widgets";

    /** The settings new widgets start from. Edited on the in-app widget screens. */
    public static final int TEMPLATE_ID = 0;

    private final JSONObject json;

    private Settings(JSONObject json) {
        this.json = json;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static Settings load(Context c, int id) {
        String raw = prefs(c).getString("w" + id, null);
        if (raw == null) {
            return new Settings(new JSONObject());
        }
        try {
            return new Settings(new JSONObject(raw));
        } catch (JSONException e) {
            return new Settings(new JSONObject());
        }
    }

    /** Settings of a placed widget. A brand-new widget gets a copy of the template. */
    public static Settings loadForWidget(Context c, int id) {
        if (prefs(c).contains("w" + id)) {
            return load(c, id);
        }
        Settings fresh = load(c, TEMPLATE_ID).copy();
        fresh.save(c, id);
        return fresh;
    }

    public static void delete(Context c, int[] ids) {
        SharedPreferences.Editor editor = prefs(c).edit();
        for (int id : ids) {
            editor.remove("w" + id);
        }
        editor.apply();
    }

    public void save(Context c, int id) {
        prefs(c).edit().putString("w" + id, json.toString()).apply();
    }

    public Settings copy() {
        try {
            return new Settings(new JSONObject(json.toString()));
        } catch (JSONException e) {
            return new Settings(new JSONObject());
        }
    }

    public boolean has(String key) {
        return json.has(key);
    }

    public int getInt(String key, int def) {
        return json.optInt(key, def);
    }

    public long getLong(String key, long def) {
        return json.optLong(key, def);
    }

    public boolean getBool(String key, boolean def) {
        return json.optBoolean(key, def);
    }

    public String getString(String key, String def) {
        return json.optString(key, def);
    }

    public void put(String key, int value) {
        try {
            json.put(key, value);
        } catch (JSONException ignored) {
        }
    }

    public void put(String key, long value) {
        try {
            json.put(key, value);
        } catch (JSONException ignored) {
        }
    }

    public void put(String key, boolean value) {
        try {
            json.put(key, value);
        } catch (JSONException ignored) {
        }
    }

    public void put(String key, String value) {
        try {
            json.put(key, value);
        } catch (JSONException ignored) {
        }
    }
}
