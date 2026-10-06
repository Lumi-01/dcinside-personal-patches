package local.privacy;

import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.TextView;
import java.lang.reflect.Method;

/** Displays only author identifiers already supplied by the app's post data. */
public final class AuthorInfo {
    private AuthorInfo() { }

    private static String value(Object item, String getter) {
        if (item == null) return null;
        try {
            Method method = item.getClass().getDeclaredMethod(getter);
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
        return IpInfo.decorateName(withId(value(post, "z"), value(post, "S")), value(post, "t"));
    }

    public static String commentName(Object comment) {
        return IpInfo.decorateName(withId(value(comment, "Y"), value(comment, "h0")), value(comment, "S"));
    }

    public static void applyPostHeader(View header, Object post) {
        if (header == null || post == null) return;
        // Verified 5.3.6 resource ID. This patch is version-scoped and fails closed on changes.
        View child = header.findViewById(0x7f0b0dd0);
        if (!(child instanceof TextView)) return;
        TextView nameView = (TextView) child;
        String userId = value(post, "W1");
        if (SettingsState.get("show_author_id") && userId != null && !userId.trim().isEmpty()) {
            CharSequence original = nameView.getText();
            SpannableStringBuilder text = new SpannableStringBuilder(original == null ? "" : original);
            text.append(" (").append(userId.trim()).append(")");
            nameView.setText(text);
        }
        IpInfo.showWhenAvailable(nameView, value(post, "b"));
    }
}
