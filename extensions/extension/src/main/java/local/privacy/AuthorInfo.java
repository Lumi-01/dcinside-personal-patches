package local.privacy;

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
        if (!SettingsState.get("show_author_id") || id == null || id.trim().isEmpty()
                || id.trim().equals(name)) return name;
        return name + " (" + id.trim() + ")";
    }

    public static String listName(Object post) {
        return withId(value(post, "z"), value(post, "S"));
    }

    public static String commentName(Object comment) {
        return withId(value(comment, "Y"), value(comment, "h0"));
    }

    public static void applyPostHeader(View header, Object post) {
        if (header == null || post == null) return;
        int id = header.getResources().getIdentifier("lumi_read_author_info", "id",
                header.getContext().getPackageName());
        if (id == 0) return;
        View child = header.findViewById(id);
        if (!(child instanceof TextView)) return;
        TextView metadata = (TextView) child;
        String userId = value(post, "W1");
        String ip = IpInfo.postMetadata(value(post, "b"));
        String shownId = SettingsState.get("show_author_id") && userId != null
                && !userId.trim().isEmpty() ? "아이디: " + userId.trim() : null;
        String display = shownId == null ? ip : ip == null ? shownId : shownId + "  ·  " + ip;
        metadata.setText(display == null ? "" : display);
        metadata.setVisibility(display == null ? View.GONE : View.VISIBLE);
    }
}
