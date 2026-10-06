package local.privacy;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Offline two-octet range hints copied from a community memo preset. */
public final class IpInfo {
    private static volatile Map<String, String> prefixLabels;

    private IpInfo() { }

    public static String decorateName(String name, String ip) {
        if (!SettingsState.get("show_ip_info")) return name;
        String estimate = prefixEstimate(ip);
        return estimate == null ? name : name + " [대역: " + estimate + "]";
    }

    public static void showWhenAvailable(TextView label, String ip) {
        if (!SettingsState.get("show_ip_info")) return;
        String estimate = prefixEstimate(ip);
        if (estimate != null) label.setText(new SpannableStringBuilder(label.getText())
                .append(" · 대역: ").append(estimate));
    }

    private static String prefixEstimate(String ip) {
        if (ip == null) return null;
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 2 && parts.length != 4) return null;
        for (int i = 0; i < 2; i++) {
            if (parts[i].isEmpty() || parts[i].length() > 3) return null;
            try {
                int value = Integer.parseInt(parts[i]);
                if (value < 0 || value > 255) return null;
            } catch (NumberFormatException ignored) { return null; }
        }
        return labels().get(parts[0] + "." + parts[1]);
    }

    private static Map<String, String> labels() {
        Map<String, String> found = prefixLabels;
        if (found != null) return found;
        synchronized (IpInfo.class) {
            if (prefixLabels != null) return prefixLabels;
            HashMap<String, String> result = new HashMap<>();
            Set<String> ambiguous = new HashSet<>();
            Context context = SettingsState.context();
            if (context != null) {
                int id = context.getResources().getIdentifier("lumi_ip_prefixes", "raw", context.getPackageName());
                if (id != 0) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            context.getResources().openRawResource(id), "UTF-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            int dash = line.indexOf('-');
                            if (dash <= 0 || dash >= line.length() - 1) continue;
                            String key = line.substring(0, dash).trim();
                            String value = line.substring(dash + 1).trim();
                            if (!key.matches("[0-9]{1,3}\\.[0-9]{1,3}") || value.isEmpty()) continue;
                            if (value.length() > 15) value = value.substring(0, 15);
                            if (ambiguous.contains(key)) continue;
                            String existing = result.get(key);
                            if (existing != null && !existing.equals(value)) {
                                result.remove(key);
                                ambiguous.add(key);
                            } else {
                                result.put(key, value);
                            }
                        }
                    } catch (Exception ignored) { }
                }
            }
            prefixLabels = Collections.unmodifiableMap(result);
            return prefixLabels;
        }
    }
}
