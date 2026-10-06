package local.privacy;

import android.content.Context;
import android.content.SharedPreferences;

/** Small, local-only preferences shared by the patched entry points. */
public final class SettingsState {
    private static volatile SharedPreferences preferences;
    private static volatile Context applicationContext;

    private SettingsState() { }

    public static void init(Context context) {
        if (preferences == null && context != null) {
            synchronized (SettingsState.class) {
                if (preferences == null) {
                    applicationContext = context.getApplicationContext();
                    preferences = applicationContext
                            .getSharedPreferences("lumi_morphe_settings", Context.MODE_PRIVATE);
                }
            }
        }
    }

    public static Context context() { return applicationContext; }

    public static boolean get(String key) {
        SharedPreferences prefs = preferences;
        return prefs != null && prefs.getBoolean(key, false);
    }

    public static void set(String key, boolean value) {
        SharedPreferences prefs = preferences;
        if (prefs != null) prefs.edit().putBoolean(key, value).apply();
    }

    public static int homeMask() {
        int mask = 0;
        if (get("hide_home_search")) mask |= 1;
        if (get("hide_home_recent")) mask |= 2;
        if (get("hide_home_recommended_galleries")) mask |= 4;
        if (get("hide_home_ranking")) mask |= 8;
        if (get("hide_home_live_best")) mask |= 16;
        if (get("hide_home_recommended_posts")) mask |= 32;
        return mask;
    }
}
