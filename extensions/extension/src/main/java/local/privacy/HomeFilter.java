package local.privacy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Filters only the verified DC Inside 5.3.6 home adapter item types. */
public final class HomeFilter {
    private static volatile Method typeGetter;

    private HomeFilter() { }

    public static List<?> filter(List<?> items, int mask) {
        if (items == null || items.isEmpty() || mask == 0) return items;
        ArrayList<Object> result = new ArrayList<>(items.size());
        for (Object item : items) {
            int type = itemType(item);
            if (!hidden(type, mask)) result.add(item);
        }
        return result;
    }

    private static int itemType(Object item) {
        if (item == null) return Integer.MIN_VALUE;
        try {
            Method getter = typeGetter;
            if (getter == null || getter.getDeclaringClass() != item.getClass()) {
                getter = item.getClass().getDeclaredMethod("h");
                getter.setAccessible(true);
                typeGetter = getter;
            }
            return ((Number) getter.invoke(item)).intValue();
        } catch (ReflectiveOperationException | RuntimeException error) {
            // A changed item class must remain visible rather than breaking the home screen.
            return Integer.MIN_VALUE;
        }
    }

    private static boolean hidden(int type, int mask) {
        return (mask & 1) != 0 && type == 1
            || (mask & 2) != 0 && type == 2
            || (mask & 4) != 0 && type == 9
            || (mask & 8) != 0 && type == 3
            || (mask & 16) != 0 && (type == 50 || type == 36 || type == 37 || type == 38)
            || (mask & 32) != 0 && (type == 35 || type == 39);
    }
}
