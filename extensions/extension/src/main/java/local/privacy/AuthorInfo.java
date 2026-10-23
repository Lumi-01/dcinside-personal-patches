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
    private static volatile int cachedButtonId, cachedNameId;
    private static String postId = "S", commentId = "h0", headerId = "W1";
    private AuthorInfo() { }
    public static void configure(boolean modern) {
        postId = modern ? "Q" : "S";
        commentId = modern ? "g0" : "h0";
        headerId = modern ? "X1" : "W1";
    }

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

    public static String listName(Object post, String name) {
        return withId(name, SettingsState.get("show_author_id") ? value(post, postId) : null);
    }

    public static String commentName(Object comment, String name) {
        return withId(name, SettingsState.get("show_author_id") ? value(comment, commentId) : null);
    }

    public static void applyPostHeader(View header, Object post) {
        if (header == null || post == null) return;
        int buttonId = cachedButtonId;
        int nameId = cachedNameId;
        if (buttonId == 0 || nameId == 0) {
            String packageName = header.getContext().getPackageName();
            buttonId = header.getResources().getIdentifier("lumi_read_ip_expand", "id", packageName);
            nameId = header.getResources().getIdentifier("read_header_name", "id", packageName);
            if (buttonId != 0 && nameId != 0) {
                cachedButtonId = buttonId;
                cachedNameId = nameId;
            }
        }
        if (buttonId == 0 || nameId == 0) return;
        View buttonView = header.findViewById(buttonId);
        View nameView = header.findViewById(nameId);
        if (!(buttonView instanceof TextView) || !(nameView instanceof TextView)) return;
        TextView button = (TextView) buttonView;
        TextView name = (TextView) nameView;
        final int tagId = nameId;
        boolean showId = SettingsState.get("show_author_id");
        boolean showIp = SettingsState.get("show_ip_info");
        name.setTag(tagId, post);
        if (!showId && !showIp) {
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);
            return;
        }
        String userId = showId ? value(post, headerId) : null;
        String rawIp = value(post, "b");
        name.post(() -> {
            if (name.getTag(tagId) != post) return;
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
                if (name.getTag(tagId) != post || name.getLayout() == null) return;
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
        if (!SettingsState.get("show_author_id") || userId == null) return;
        String id = userId.trim();
        if (id.isEmpty()) return;
        CharSequence current = name.getText();
        if (current == null || current.length() == 0) return;
        String label = " (" + id + ")";
        String text = current.toString();
        if (text.contains("(" + id + ")")) return;
        String ip = rawIp == null ? "" : rawIp.trim();
        int ipStart = ip.isEmpty() ? -1 : text.indexOf("(" + ip + ")");
        SpannableStringBuilder updated = new SpannableStringBuilder(current);
        if (ipStart >= 0) updated.insert(ipStart, label + " ");
        else updated.append(label);
        name.setText(updated);
    }
}
