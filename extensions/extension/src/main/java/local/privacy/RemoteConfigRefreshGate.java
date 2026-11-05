package local.privacy;

import android.os.SystemClock;

/** Avoid redundant remote-config fetches while retaining startup and stale refreshes. */
public final class RemoteConfigRefreshGate {
    private static final long ONE_HOUR_MS = 3_600_000L;
    private static final long EIGHTEEN_HOURS_MS = 64_800_000L;
    private static volatile long lastAttemptElapsed;
    private static String preferencesClass;
    private static String forceGetter;

    private RemoteConfigRefreshGate() { }

    public static void configure(String owner, String getter) {
        preferencesClass = owner;
        forceGetter = getter;
    }

    public static void markAttempt() {
        lastAttemptElapsed = SystemClock.elapsedRealtime();
    }

    public static boolean shouldRefreshOnHomeStop() {
        if (!SettingsState.get("reduce_config_refresh")) return true;
        try {
            Class<?> prefs = Class.forName(preferencesClass);
            Object instance = prefs.getField("a").get(null);
            if ((Boolean) prefs.getMethod(forceGetter).invoke(instance)) return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return true;
        }

        long lastAttempt = lastAttemptElapsed;
        if (lastAttempt != 0 && SystemClock.elapsedRealtime() - lastAttempt < ONE_HOUR_MS) {
            return false;
        }
        try {
            Object config = Class.forName("com.google.firebase.remoteconfig.p")
                .getMethod("t").invoke(null);
            Object info = config.getClass().getMethod("s").invoke(config);
            long lastFetch = ((Number) info.getClass().getMethod("a").invoke(info)).longValue();
            if (lastFetch > 0 && System.currentTimeMillis() - lastFetch < EIGHTEEN_HOURS_MS) {
                return false;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return true;
        }
        return true;
    }
}
