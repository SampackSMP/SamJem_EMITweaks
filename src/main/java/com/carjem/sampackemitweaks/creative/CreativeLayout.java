package com.carjem.sampackemitweaks.creative;

import com.carjem.sampackemitweaks.client.ClientConfig;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Size of the creative inventory: item columns and rows, and everything derived from them. The
 * mixins under {@link com.carjem.sampackemitweaks.mixin.creative} apply it to the screen. The
 * columns are always vanilla's 9 now; only the rows are configurable, but the panel drawing and
 * the tab layout still handle extra columns.
 *
 * <p>Extra columns and rows are inserted into the vanilla 195x136 panel, so every vanilla
 * offset left of / above the item grid stays put and everything right of / below it moves by
 * {@link #extraWidth()} / {@link #extraHeight()}. The hotbar stays the bottom row, the
 * scrollbar the right column, and the search box stays right-aligned above the grid.
 */
public final class CreativeLayout {
    public static final int VANILLA_COLUMNS = 9;
    public static final int VANILLA_ROWS = 5;
    public static final int MAX_ROWS = 20;

    private static final int SLOT_SIZE = 18;
    private static final int VANILLA_WIDTH = 195;
    private static final int VANILLA_HEIGHT = 136;
    private static final int VANILLA_HOTBAR_Y = 112;
    private static final int TEXTURE_SIZE = 256;
    /** Vanilla's tab side border: the black outline and 2px of bevel. */
    public static final int TAB_BORDER = 3;
    /**
     * Rows of plain fill left out of each tab sprite so tabs come out about square: top tabs
     * drop rows 6-8 (above the icon), bottom tabs row 5 (above) and rows 23-24 (below).
     */
    public static final int TAB_HEIGHT_CUT = 3;
    /**
     * How far a tab's icon moves from vanilla's spot once those rows are cut: centered between
     * the top border and the panel (top row), and 1px below the panel (bottom row).
     */
    public static final int TAB_ICON_SHIFT_Y = -2;
    private static final int TAB_SPRITE_WIDTH = 26;
    private static final int TAB_SPRITE_HEIGHT = 32;
    // A tab, once cut, is this long; its last 4px are under the panel.
    private static final int TAB_LENGTH = TAB_SPRITE_HEIGHT - TAB_HEIGHT_CUT;
    /** How far a side tab sticks out of the panel, the same as a top tab sticks up. */
    public static final int SIDE_TAB_DEPTH = TAB_LENGTH - 4;
    // How far the side tabs sit above the panel's bottom edge.
    private static final int SIDE_TAB_LIFT = 4;
    // A top and a bottom tab's icon row, once cut (vanilla: 9 and 7).
    private static final int TOP_TAB_ICON_Y = 9 + TAB_ICON_SHIFT_Y;
    private static final int BOTTOM_TAB_ICON_Y = 7 + TAB_ICON_SHIFT_Y;
    // Narrowest tab: both borders around an 18px fill (1px either side of the icon).
    private static final int MIN_TAB_WIDTH = 2 * TAB_BORDER + 18;
    // Gap the panel is widened for, so the tabs that fit it don't have to touch (vanilla's gap).
    private static final int TAB_GAP = 1;
    // Plain columns of the item and search tab backgrounds, between the grid and the scrollbar
    // and right of the scrollbar, that the panel padding stretches.
    private static final int PAD_COLUMN_BEFORE_SCROLLBAR = 172;
    private static final int PAD_COLUMN_AFTER_SCROLLBAR = 189;
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
     * Re-reads the window size. Called once per creative screen, when its menu is built, because
     * the slot count is fixed for the life of the menu.
     */
    public static void refresh() {
        active = compute();
    }

    /**
     * The layout the config and the current window size call for, without making it active:
     * vanilla's 9 columns, and the configured rows, which with fit to screen shrink (never below
     * vanilla's 5) to what the window has room for, so the rows fill its height.
     */
    public static CreativeLayout compute() {
        int rows = ClientConfig.get(ClientConfig.CREATIVE_ROWS);
        if (ClientConfig.get(ClientConfig.CREATIVE_FIT_TO_SCREEN)) {
            Window window = Minecraft.getInstance().getWindow();
            int spareHeight = window.getGuiScaledHeight() - 2 * FIT_VERTICAL_MARGIN - VANILLA_HEIGHT;
            rows = Math.min(rows, VANILLA_ROWS + Math.max(0, spareHeight / SLOT_SIZE));
        }
        rows = Math.clamp(rows, VANILLA_ROWS, MAX_ROWS);
        return rows == VANILLA_ROWS ? VANILLA : new CreativeLayout(VANILLA_COLUMNS, rows);
    }

    public boolean sameSize(CreativeLayout other) {
        return columns == other.columns && rows == other.rows;
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
        return gridPanelWidth() + panelPad();
    }

    // The panel's width from its columns alone.
    private int gridPanelWidth() {
        return VANILLA_WIDTH + extraWidth();
    }

    /**
     * Pixels the panel is widened by so the tabs that fit it side by side get {@link #TAB_GAP}
     * between them: 4 at vanilla width (8 tabs need 199px), none once the columns leave room.
     */
    public int panelPad() {
        int spots = gridPanelWidth() / MIN_TAB_WIDTH;
        return Math.max(0, spots * MIN_TAB_WIDTH + (spots - 1) * TAB_GAP - gridPanelWidth());
    }

    /** How far the scrollbar moves right: the extra columns and the padding left of it. */
    public int scrollbarShift() {
        return extraWidth() + panelPad() / 2;
    }

    /** True when the panel is vanilla's 195x136, so its background is drawn as vanilla draws it. */
    public boolean hasVanillaPanel() {
        return isVanilla() && panelPad() == 0;
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

    /** Tab spots per row: as many equal tabs as fit side by side across the panel. 8 at vanilla width. */
    public int tabSpots() {
        return gridPanelWidth() / MIN_TAB_WIDTH;
    }

    /** Width of every tab: the panel shared out between the spots, at most vanilla's 26px. */
    public int tabWidth() {
        return Math.min(width() / tabSpots(), TAB_SPRITE_WIDTH);
    }

    /**
     * Regular tabs per row: all the spots, since the right-aligned tabs are moved to the sides
     * (see {@link #sideTabY}). 8 at vanilla width (vanilla: 5).
     */
    public int tabsPerRow() {
        return tabSpots();
    }

    /**
     * A regular tab's x relative to the panel. A full row runs from the panel's left edge to its
     * right edge, with the width left over shared out as gaps between the tabs.
     */
    public int tabX(int column) {
        int spots = tabSpots();
        int spare = width() - spots * tabWidth();
        return column * tabWidth() + (spots > 1 ? column * spare / (spots - 1) : 0);
    }

    /** True for the last spot in a row, which ends at the panel's right edge. */
    public boolean isLastTabColumn(int column) {
        return column == tabSpots() - 1;
    }

    /**
     * Where a side tab sits across, relative to the panel. The tabs vanilla aligns right in the
     * top and bottom rows (search, inventory, hotbar, op) are side tabs instead: a pair at the
     * bottom of each side, sticking out as far as a top tab sticks up.
     */
    public int sideTabX(boolean left) {
        return left ? -SIDE_TAB_DEPTH : width() + SIDE_TAB_DEPTH - TAB_LENGTH;
    }

    /**
     * Where the n-th (from the top) of a side's {@code count} tabs sits down, relative to the
     * panel: the last one {@link #SIDE_TAB_LIFT} above the panel's bottom edge, clear of its
     * border and the bottom tabs, the others above it with {@link #TAB_GAP} between.
     */
    public int sideTabY(int index, int count) {
        int fromBottom = count - index;
        return height() - SIDE_TAB_LIFT - fromBottom * tabWidth() - (fromBottom - 1) * TAB_GAP;
    }

    public int tabsPerPage() {
        return 2 * tabsPerRow();
    }

    /** Where a tab's icon starts across it: centered in the fill. */
    private int tabIconX() {
        int fill = tabWidth() - 2 * TAB_BORDER;
        return TAB_BORDER + (fill - 16 + 1) / 2;
    }

    /** How far a tab's icon moves right from vanilla's spot (5px in) to center it in the fill. */
    public int tabIconShiftX() {
        return tabIconX() - 5;
    }

    /** A side tab's icon, relative to the tab: a top (left) or bottom (right) tab's, mirrored like the tab. */
    public int sideTabIconX(boolean left) {
        return left ? TOP_TAB_ICON_Y : BOTTOM_TAB_ICON_Y;
    }

    public int sideTabIconY() {
        return tabIconX();
    }

    /** A side tab's area, relative to the panel: the part that sticks out. */
    public boolean isOverSideTab(boolean left, int index, int count, double x, double y) {
        int outside = left ? -SIDE_TAB_DEPTH : width();
        int top = sideTabY(index, count);
        return x >= outside && x < outside + SIDE_TAB_DEPTH && y >= top && y < top + tabWidth();
    }

    /**
     * Draws a side tab: a tab sprite mirrored across its diagonal. On the left a top tab, so it
     * opens onto the panel's lit left border with its highlight outside and on top; on the right a
     * bottom tab, opening onto the shaded right border with its shadow outside and below.
     */
    public void blitSideTab(GuiGraphics graphics, ResourceLocation sprite, int x, int y, boolean left) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.mulPose(new Matrix4f(0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1));
        // The mirror flips the quads' winding.
        RenderSystem.disableCull();
        blitTab(graphics, sprite, 0, 0, left);
        RenderSystem.enableCull();
        pose.popPose();
    }

    /**
     * Draws a tab sprite at {@link #tabWidth()} wide and {@link #TAB_HEIGHT_CUT} shorter, by
     * leaving out a middle column strip and rows of plain fill, so its borders, corners and the
     * edge that joins the panel stay intact.
     */
    public void blitTab(GuiGraphics graphics, ResourceLocation sprite, int x, int y, boolean top) {
        int width = tabWidth();
        int left = width / 2;
        List<Segment> xs = List.of(
                Segment.copy(0, 0, left),
                Segment.copy(TAB_SPRITE_WIDTH - (width - left), left, width - left));
        List<Segment> ys = top
                ? List.of(Segment.copy(0, 0, 6), Segment.copy(9, 6, TAB_SPRITE_HEIGHT - 9))
                : List.of(Segment.copy(0, 0, 5), Segment.copy(6, 5, 17), Segment.copy(25, 22, TAB_SPRITE_HEIGHT - 25));
        for (Segment sx : xs) {
            for (Segment sy : ys) {
                graphics.blitSprite(sprite, TAB_SPRITE_WIDTH, TAB_SPRITE_HEIGHT, sx.src, sy.src, x + sx.dst, y + sy.dst, sx.dstLength, sy.dstLength);
            }
        }
    }

    /**
     * Draws a creative tab background, laid out for a 195x136 panel, at this layout's size.
     *
     * <p>Item tabs (and the search tab) repeat a slot column and a slot row from the middle of
     * the grid. Those strips also cross the title bar, hotbar and scrollbar track, which look the
     * same at every column/row, so the copies line up. The search box sits right of the
     * repeated column and moves right with the rest of the panel.
     *
     * The {@link #panelPad()} is split between two plain columns either side of the scrollbar.
     *
     * <p>The inventory tab has no repeating strip, so it stretches a plain column (x=190, just
     * inside the right border) and a plain row (y=130, just under the hotbar) instead, so the
     * drawn slots stay at their vanilla top-left positions along with the real ones.
     */
    public void blitBackground(GuiGraphics graphics, ResourceLocation texture, int x, int y, boolean inventoryTab) {
        int pad = panelPad();
        List<Segment> xs = inventoryTab
                ? axis(VANILLA_WIDTH, Insert.stretch(190, extraWidth() + pad))
                : axis(VANILLA_WIDTH, Insert.tile(44, 26, VANILLA_COLUMNS - 1, extraWidth()),
                        Insert.stretch(PAD_COLUMN_BEFORE_SCROLLBAR, pad / 2),
                        Insert.stretch(PAD_COLUMN_AFTER_SCROLLBAR, pad - pad / 2));
        List<Segment> ys = inventoryTab
                ? axis(VANILLA_HEIGHT, Insert.stretch(130, extraHeight()))
                : axis(VANILLA_HEIGHT, Insert.tile(53, 35, VANILLA_ROWS - 1, extraHeight()));
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
     * Extra pixels put into the panel along one axis at {@code at} (a position in the texture):
     * copies of the slot strips starting at {@code stripStart} (up to {@code maxStrips} per copy),
     * or, with no strip, the single pixel at {@code at} stretched.
     */
    private record Insert(int at, int stripStart, int maxStrips, int extra) {
        static Insert tile(int at, int stripStart, int maxStrips, int extra) {
            return new Insert(at, stripStart, maxStrips, extra);
        }

        static Insert stretch(int at, int extra) {
            return new Insert(at, -1, 0, extra);
        }
    }

    /** The texture's {@code [0, total)} with the inserts (in order of {@code at}) put in, the rest shifted. */
    private static List<Segment> axis(int total, Insert... inserts) {
        List<Segment> segments = new ArrayList<>();
        int src = 0;
        int dst = 0;
        for (Insert insert : inserts) {
            if (insert.at > src) {
                segments.add(Segment.copy(src, dst, insert.at - src));
                dst += insert.at - src;
                src = insert.at;
            }
            if (insert.stripStart < 0) {
                segments.add(new Segment(insert.at, 1, dst, 1 + insert.extra));
                dst += 1 + insert.extra;
                src = insert.at + 1;
            } else {
                for (int strips = insert.extra / SLOT_SIZE; strips > 0; ) {
                    int count = Math.min(strips, insert.maxStrips);
                    segments.add(Segment.copy(insert.stripStart, dst, count * SLOT_SIZE));
                    dst += count * SLOT_SIZE;
                    strips -= count;
                }
            }
        }
        segments.add(Segment.copy(src, dst, total - src));
        return segments;
    }
}
