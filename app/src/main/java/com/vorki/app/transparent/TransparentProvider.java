package com.vorki.app.transparent;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;

import com.vorki.app.core.Settings;
import com.vorki.app.core.VorkiProvider;

public class TransparentProvider extends VorkiProvider {

    @Override
    protected void update(Context c, AppWidgetManager m, int id) {
        m.updateAppWidget(id, TransparentRenderer.render(c, id, Settings.loadForWidget(c, id), false));
    }

    @Override
    protected boolean wantsTimedRefresh() {
        return false;
    }

    @Override
    public void onDeleted(Context context, int[] ids) {
        super.onDeleted(context, ids);
        SharedPreferences.Editor editor =
                context.getSharedPreferences(TapActivity.RUNTIME_PREFS, Context.MODE_PRIVATE).edit();
        for (int id : ids) {
            editor.remove("tap_" + id);
        }
        editor.apply();
    }
}
