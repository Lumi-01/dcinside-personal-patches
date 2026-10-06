package local.privacy;

import android.view.View;

/** Keeps the original binding path while hiding page separators. */
public final class PageIndicator {
    private PageIndicator() { }

    public static void hide(View view, int originalVisibility) {
        view.setVisibility(View.GONE);
    }
}
