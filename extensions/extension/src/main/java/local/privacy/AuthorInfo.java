package local.privacy;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/** Displays only author identifiers already supplied by the app's post data. */
public final class AuthorInfo {
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>> GETTERS =
            new ConcurrentHashMap<>();
    private AuthorInfo() { }

    private static String value(Object item, String getter) {
        if (item == null) return null;
        try {
            Class<?> type = item.getClass();
            ConcurrentHashMap<String, Method> methods = GETTERS.computeIfAbsent(type,
                    unused -> new ConcurrentHashMap<>());
            Method method = methods.get(getter);
            if (method == null) {
                Method resolved = type.getDeclaredMethod(getter);
                Method existing = methods.putIfAbsent(getter, resolved);
                method = existing == null ? resolved : existing;
            }
            Object result = method.invoke(item);
            return result instanceof String ? (String) result : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static String withId(String name, String id) {
        if (name == null) name = "";
        if (id == null) return name;
        String visibleId = id.trim();
        if (visibleId.isEmpty() || visibleId.equals(name)) return name;
        return name + " (" + visibleId + ")";
    }

    public static String listName(Object post) {
        String name = value(post, "z");
        return withId(name, SettingsState.get("show_author_id") ? value(post, "S") : null);
    }

    public static String commentName(Object comment) {
        String name = value(comment, "Y");
        return withId(name, SettingsState.get("show_author_id") ? value(comment, "h0") : null);
    }

    public static void applyPostHeader(View header, Object post) {
        if (header == null || post == null) return;
        int buttonId = header.getResources().getIdentifier("lumi_read_ip_expand", "id",
                header.getContext().getPackageName());
        int nameId = header.getResources().getIdentifier("read_header_name", "id",
                header.getContext().getPackageName());
        if (buttonId == 0 || nameId == 0) return;
        View buttonView = header.findViewById(buttonId);
        View nameView = header.findViewById(nameId);
        if (!(buttonView instanceof TextView) || !(nameView instanceof TextView)) return;
        TextView button = (TextView) buttonView;
        TextView name = (TextView) nameView;
        String userId = value(post, "W1");
        String rawIp = value(post, "b");
        name.setTag(nameId, post);
        name.post(() -> {
            if (name.getTag(nameId) != post) return;
            decoratePostName(name, userId, rawIp);
            boolean hasIp = SettingsState.get("show_ip_info")
                    && rawIp != null && !rawIp.trim().isEmpty();
            if (!hasIp) {
                button.setVisibility(View.GONE);
                button.setOnClickListener(null);
                return;
            }
            boolean open = !SettingsState.get("collapse_long_ip_info");
            setExpanded(name, button, open);
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(view -> setExpanded(name, button,
                    name.getMaxLines() == 1));
            if (!open) name.post(() -> {
                if (name.getTag(nameId) != post || name.getLayout() == null) return;
                // A short name and range need no expand control.
                if (name.getLayout().getEllipsisCount(0) == 0) button.setVisibility(View.GONE);
            });
        });
    }

    private static void setExpanded(TextView name, TextView button, boolean expanded) {
        name.setSingleLine(!expanded);
        if (expanded) {
            name.setMaxLines(Integer.MAX_VALUE);
            name.setEllipsize(null);
        } else {
            name.setEllipsize(TextUtils.TruncateAt.END);
        }
        button.setText(expanded ? "접기" : "펼치기");
        button.setContentDescription(expanded ? "IP 정보 접기" : "IP 정보 펼치기");
    }

    private static void decoratePostName(TextView name, String userId, String rawIp) {
        CharSequence current = name.getText();
        if (current == null || current.length() == 0) return;
        SpannableStringBuilder updated = new SpannableStringBuilder(current);
        if (SettingsState.get("show_author_id") && userId != null && !userId.trim().isEmpty()) {
            String id = userId.trim();
            if (!updated.toString().contains("(" + id + ")")) {
                String label = " (" + id + ")";
                String ip = rawIp == null ? "" : rawIp.trim();
                int ipStart = ip.isEmpty() ? -1 : updated.toString().indexOf("(" + ip + ")");
                if (ipStart >= 0) updated.insert(ipStart, label + " ");
                else updated.append(label);
            }
        }
        if (!updated.toString().contentEquals(current)) name.setText(updated);
    }
}
