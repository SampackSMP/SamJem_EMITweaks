package com.carjem.sampackemitweaks.creative;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Size of the creative inventory: item columns and rows, and everything derived from them. The
 * mixins under {@link com.carjem.sampackemitweaks.mixin.creative} apply it to the screen.
 *
 * <p>Extra columns and rows are inserted into the vanilla 195x136 panel, so every vanilla
 * offset left of / above the item grid stays put and everything right of / below it moves by
 * {@link #extraWidth()} / {@link #extraHeight()}. The hotbar stays the bottom row, the
 * scrollbar the right column, and the search box stays right-aligned above the grid.
 */
public final class CreativeLayout {
    public static final int VANILLA_COLUMNS = 9;
    public static final int VANILLA_ROWS = 5;
    public static final int MAX_COLUMNS = 32;
    public static final int MAX_ROWS = 20;

    private static final int SLOT_SIZE = 18;
    private static final int VANILLA_WIDTH = 195;
    private static final int VANILLA_HEIGHT = 136;
    private static final int VANILLA_HOTBAR_Y = 112;
    private static final int TEXTURE_SIZE = 256;
    private static final int TAB_SPACING = 27;
    // The right-aligned search/inventory/hotbar/op tabs take the two rightmost tab spots.
    private static final int RIGHT_ALIGNED_TABS_WIDTH = 2 * TAB_SPACING + 26;
    // Room left beside the panel for widgets such as ReCreative's editor button.
    private static final int FIT_SIDE_MARGIN = 32;
    // Room above (page buttons sit 50px over the panel) and below (bottom tabs) the panel.
    private static final int FIT_VERTICAL_MARGIN = 50;

    public static final CreativeLayout VANILLA = new CreativeLayout(VANILLA_COLUMNS, VANILLA_ROWS);

    private static CreativeLayout active = VANILLA;

    public final int columns;
    public final int rows;

    private CreativeLayout(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
    }

    /** The layout of the creative screen currently open (or last opened). */
    public static CreativeLayout get() {
        return active;
    }

    /**
     * Re-reads the config. Called once per creative screen, when its menu is built, because the
     * slot count is fixed for the life of the menu.
     */
    public static void refresh() {
        active = CreativeLayoutConfig.SPEC.isLoaded() ? fromConfig() : VANILLA;
    }

    private static CreativeLayout fromConfig() {
        int columns = CreativeLayoutConfig.COLUMNS.get();
        int rows = CreativeLayoutConfig.ROWS.get();
        if (CreativeLayoutConfig.FIT_TO_SCREEN.get()) {
            Window window = Minecraft.getInstance().getWindow();
            int spareWidth = window.getGuiScaledWidth() - 2 * FIT_SIDE_MARGIN - VANILLA_WIDTH;
            int spareHeight = window.getGuiScaledHeight() - 2 * FIT_VERTICAL_MARGIN - VANILLA_HEIGHT;
            columns = Math.min(columns, VANILLA_COLUMNS + Math.max(0, spareWidth / SLOT_SIZE));
            rows = Math.min(rows, VANILLA_ROWS + Math.max(0, spareHeight / SLOT_SIZE));
        }
        columns = Math.clamp(columns, VANILLA_COLUMNS, MAX_COLUMNS);
        rows = Math.clamp(rows, VANILLA_ROWS, MAX_ROWS);
        return columns == VANILLA_COLUMNS && rows == VANILLA_ROWS ? VANILLA : new CreativeLayout(columns, rows);
    }

    public boolean isVanilla() {
        return this == VANILLA;
    }

    public int extraWidth() {
        return (columns - VANILLA_COLUMNS) * SLOT_SIZE;
    }

    public int extraHeight() {
        return (rows - VANILLA_ROWS) * SLOT_SIZE;
    }

    public int width() {
        return VANILLA_WIDTH + extraWidth();
    }

    public int height() {
        return VANILLA_HEIGHT + extraHeight();
    }

    public int gridSize() {
        return columns * rows;
    }

    public int hotbarY() {
        return VANILLA_HOTBAR_Y + extraHeight();
    }

    /** Regular tabs per row: as many as fit left of the two right-aligned tab spots. Vanilla: 5. */
    public int tabsPerRow() {
        return (width() - RIGHT_ALIGNED_TABS_WIDTH) / TAB_SPACING + 1;
    }

    public int tabsPerPage() {
        return 2 * tabsPerRow();
    }

    /**
     * Draws a creative tab background, laid out for a 195x136 panel, at this layout's size.
     *
     * <p>Item tabs (and the search tab) repeat a slot column and a slot row from the middle of
     * the grid. Those strips also cross the title bar, hotbar and scrollbar track, which look the
     * same at every column/row, so the copies line up. The search box sits right of the
     * repeated column and moves right with the rest of the panel.
     *
     * <p>The inventory tab has no repeating strip, so it stretches a plain column (x=190, just
     * inside the right border) and a plain row (y=4, just under the top border) instead. Its
     * contents stay in the bottom-left corner, where the hotbar lines up with the item tabs'.
     */
    public void blitBackground(GuiGraphics graphics, ResourceLocation texture, int x, int y, boolean inventoryTab) {
        List<Segment> xs = inventoryTab
                ? stretched(190, extraWidth(), VANILLA_WIDTH)
                : tiled(44, 26, VANILLA_COLUMNS - 1, extraWidth(), VANILLA_WIDTH);
        List<Segment> ys = inventoryTab
                ? stretched(4, extraHeight(), VANILLA_HEIGHT)
                : tiled(53, 35, VANILLA_ROWS - 1, extraHeight(), VANILLA_HEIGHT);
        for (Segment sx : xs) {
            for (Segment sy : ys) {
                graphics.blit(texture, x + sx.dst, y + sy.dst, sx.dstLength, sy.dstLength,
                        sx.src, sy.src, sx.srcLength, sy.srcLength, TEXTURE_SIZE, TEXTURE_SIZE);
            }
        }
    }

    /** One span of the panel along one axis: where it comes from in the texture and where it goes. */
    private record Segment(int src, int srcLength, int dst, int dstLength) {
        static Segment copy(int src, int dst, int length) {
            return new Segment(src, length, dst, length);
        }
    }

    /**
     * {@code [0, splitAt)} as-is, then {@code extra} pixels of copies of the slot strips starting
     * at {@code stripStart} (up to {@code maxStrips} strips per copy), then the rest shifted.
     */
    private static List<Segment> tiled(int splitAt, int stripStart, int maxStrips, int extra, int total) {
        List<Segment> segments = new ArrayList<>();
        segments.add(Segment.copy(0, 0, splitAt));
        int dst = splitAt;
        for (int strips = extra / SLOT_SIZE; strips > 0; ) {
            int count = Math.min(strips, maxStrips);
            segments.add(Segment.copy(stripStart, dst, count * SLOT_SIZE));
            dst += count * SLOT_SIZE;
            strips -= count;
        }
        segments.add(Segment.copy(splitAt, dst, total - splitAt));
        return segments;
    }

    /** {@code [0, at)} as-is, the single pixel at {@code at} stretched by {@code extra}, then the rest shifted. */
    private static List<Segment> stretched(int at, int extra, int total) {
        return List.of(
                Segment.copy(0, 0, at),
                new Segment(at, 1, at, 1 + extra),
                Segment.copy(at + 1, at + 1 + extra, total - at - 1));
    }
}
