package com.carjem.sampackemitweaks.search;

/**
 * Where the search history list was last drawn, if it is open, and where the mouse really is.
 * Free of EMI's classes, so {@link com.carjem.sampackemitweaks.mixin.emi.GameRendererSearchMixin}
 * can ask it on every frame without EMI installed. While the mouse is over the list, the screen
 * underneath is drawn as if the mouse were nowhere, the way NeoForge draws background layers, so
 * nothing under the list highlights or shows a tooltip.
 */
public final class SearchOverlay {
    private static boolean open;
    private static int left;
    private static int top;
    private static int right;
    private static int bottom;
    private static int mouseX;
    private static int mouseY;

    private SearchOverlay() {
    }

    static void show(int x1, int y1, int x2, int y2) {
        open = true;
        left = x1;
        top = y1;
        right = x2;
        bottom = y2;
    }

    static void hide() {
        open = false;
    }

    public static boolean covers(double x, double y) {
        return open && x >= left && x < right && y >= top && y < bottom;
    }

    /** Records the real mouse position for this frame; true if the screen should not see it. */
    public static boolean hidesMouse(int x, int y) {
        mouseX = x;
        mouseY = y;
        return covers(x, y);
    }

    static int mouseX() {
        return mouseX;
    }

    static int mouseY() {
        return mouseY;
    }
}
