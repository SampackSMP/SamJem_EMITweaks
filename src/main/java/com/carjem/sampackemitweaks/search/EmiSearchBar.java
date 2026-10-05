package com.carjem.sampackemitweaks.search;

import com.carjem.sampackemitweaks.client.ClientConfig;
import dev.emi.emi.screen.EmiScreenBase;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.screen.widget.EmiSearchWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Additions to EMI's search bar: buttons at its right end, an x that clears it and an arrow that
 * opens a list of recent searches, with switches for the clearing options at its foot. While the
 * list is open it takes every click, scroll and key meant for what is under it, the screen under
 * it sees no mouse at all ({@link SearchOverlay}), and a click outside it only closes it.
 * Only loaded with EMI; {@link com.carjem.sampackemitweaks.mixin.emi.EmiSearchWidgetMixin} and
 * {@link com.carjem.sampackemitweaks.mixin.emi.EmiScreenManagerSearchMixin} call in here.
 */
public final class EmiSearchBar {
    /** Width of each button at the bar's right end. */
    public static final int BUTTON_WIDTH = 10;
    private static final int ROW_HEIGHT = 11;
    // Space between the bar and the list, so it clears REMI's padded search bar texture.
    private static final int GAP = 3;

    /** Switches at the foot of the list, toggled without closing it. */
    private record Toggle(String key, ModConfigSpec.BooleanValue value) {
    }

    private static final List<Toggle> TOGGLES = List.of(
            new Toggle("clear_on_tab_switch", ClientConfig.CLEAR_SEARCH_ON_TAB_SWITCH),
            new Toggle("clear_on_close", ClientConfig.CLEAR_SEARCH_ON_CLOSE),
            new Toggle("clear_button", ClientConfig.CLEAR_SEARCH_BUTTON));

    private static boolean open;
    // A press the list took; its release is taken too, so it can't land on the screen once the list is gone.
    private static boolean swallowRelease;
    // The screen the list was opened on; another screen closes it.
    private static Screen openOn;
    private static int scroll;
    // The row the arrow keys are on, as an index into the entries; -1 for none.
    private static int selected = -1;

    private EmiSearchBar() {
    }

    /** Clears EMI's search bar, adding what it held to the history first. */
    public static void clear() {
        EmiSearchWidget search = EmiScreenManager.search;
        if (search == null || search.getValue().isEmpty()) return;
        SearchHistory.record(search.getValue());
        search.setValue("");
    }

    /** The search bar, if EMI shows it on the current screen. */
    private static EmiSearchWidget activeBar() {
        EmiSearchWidget search = EmiScreenManager.search;
        if (search == null || !search.isVisible() || EmiScreenManager.isDisabled()) return null;
        return EmiScreenBase.getCurrent().isEmpty() ? null : search;
    }

    // ---- Buttons ----

    private static boolean hasClearButton() {
        return ClientConfig.get(ClientConfig.CLEAR_SEARCH_BUTTON);
    }

    private static boolean hasHistoryButton() {
        return ClientConfig.get(ClientConfig.SEARCH_HISTORY_DROPDOWN) && ClientConfig.get(ClientConfig.SEARCH_HISTORY_SIZE) > 0;
    }

    /** Width kept free at the bar's right end, whether or not the x is showing, so the text doesn't jump. */
    public static int reservedWidth() {
        return BUTTON_WIDTH * ((hasClearButton() ? 1 : 0) + (hasHistoryButton() ? 1 : 0));
    }

    /** The left edge of the arrow, or of the x (0 rightmost, 1 left of the arrow). */
    private static int buttonX(EmiSearchWidget search, int slot) {
        return search.getX() + search.getWidth() - (slot + 1) * BUTTON_WIDTH - 1;
    }

    private static boolean isOver(EmiSearchWidget search, int slot, double mouseX, double mouseY) {
        int x = buttonX(search, slot);
        return mouseX >= x && mouseX < x + BUTTON_WIDTH && mouseY >= search.getY() && mouseY < search.getY() + search.getHeight();
    }

    private static boolean isOverClear(EmiSearchWidget search, double mouseX, double mouseY) {
        return hasClearButton() && !search.getValue().isEmpty() && isOver(search, hasHistoryButton() ? 1 : 0, mouseX, mouseY);
    }

    private static boolean isOverArrow(EmiSearchWidget search, double mouseX, double mouseY) {
        return hasHistoryButton() && isOver(search, 0, mouseX, mouseY);
    }

    /** The x (while the bar has text) and the arrow, white while hovered or, for the arrow, while open. */
    public static void renderButtons(GuiGraphics graphics, EmiSearchWidget search, int mouseX, int mouseY) {
        if (!search.isVisible()) return;
        int y = search.getY() + search.getHeight() / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1);
        if (hasHistoryButton()) {
            int color = open || isOverArrow(search, mouseX, mouseY) ? 0xFFFFFFFF : 0xFFA0A0A0;
            int x = buttonX(search, 0) + 2;
            // A 5x3 triangle pointing to where the list opens.
            boolean up = opensAbove(search, visibleRows());
            for (int i = 0; i < 3; i++) {
                int row = up ? y + 1 - i : y - 1 + i;
                graphics.fill(x + i, row, x + 5 - i, row + 1, color);
            }
        }
        if (hasClearButton() && !search.getValue().isEmpty()) {
            int color = isOverClear(search, mouseX, mouseY) ? 0xFFFFFFFF : 0xFFA0A0A0;
            int x = buttonX(search, hasHistoryButton() ? 1 : 0) + 2;
            for (int i = 0; i < 5; i++) {
                graphics.fill(x + i, y - 2 + i, x + i + 1, y - 1 + i, color);
                graphics.fill(x + 4 - i, y - 2 + i, x + 5 - i, y - 1 + i, color);
            }
        }
        graphics.pose().popPose();
    }

    // ---- History list ----

    private static void open() {
        open = true;
        openOn = Minecraft.getInstance().screen;
        scroll = 0;
        selected = -1;
    }

    public static void close() {
        open = false;
        openOn = null;
        SearchOverlay.hide();
    }

    /** The searches the list holds, newest first; narrowed to those containing the bar's text if the config says so. */
    private static List<String> entries() {
        List<String> entries = new ArrayList<>();
        String text = EmiScreenManager.search.getValue();
        String lower = text.toLowerCase(Locale.ROOT);
        boolean filter = ClientConfig.get(ClientConfig.SEARCH_HISTORY_FILTER) && !text.isEmpty();
        for (String entry : SearchHistory.entries()) {
            if (!entry.isBlank() && (!filter || entry.toLowerCase(Locale.ROOT).contains(lower))) entries.add(entry);
        }
        return entries;
    }

    private static int visibleRows() {
        return Math.max(1, Math.min(ClientConfig.get(ClientConfig.SEARCH_HISTORY_ROWS), entries().size()));
    }

    /** The list's height: its entry rows, a divider, then the toggles. */
    private static int listHeight(int rows) {
        return rows * ROW_HEIGHT + 3 + TOGGLES.size() * ROW_HEIGHT + 2;
    }

    /** Where the toggles start, below the entry rows and the divider. */
    private static int togglesTop(int top, int rows) {
        return top + 1 + rows * ROW_HEIGHT + 3;
    }

    private static boolean opensAbove(EmiSearchWidget search, int rows) {
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return search.getY() + search.getHeight() + GAP + listHeight(rows) > screenHeight;
    }

    /** The list's top edge: below the bar if it fits there, otherwise above (EMI's bar is usually at the bottom). */
    private static int listTop(EmiSearchWidget search, int rows) {
        return opensAbove(search, rows)
                ? search.getY() - GAP - listHeight(rows)
                : search.getY() + search.getHeight() + GAP;
    }

    /** The list, if it is open on this screen; closes it once its screen or bar is gone. */
    private static EmiSearchWidget openBar() {
        if (!open) return null;
        EmiSearchWidget search = activeBar();
        if (search == null || Minecraft.getInstance().screen != openOn || !hasHistoryButton()) {
            close();
            return null;
        }
        return search;
    }

    private static boolean isOverList(EmiSearchWidget search, double mouseX, double mouseY) {
        int rows = visibleRows();
        int top = listTop(search, rows);
        return mouseX >= search.getX() - 1 && mouseX < search.getX() + search.getWidth() + 1
                && mouseY >= top - 1 && mouseY < top + listHeight(rows) + 1;
    }

    /** The toggle under the mouse, or -1. */
    private static int toggleAt(EmiSearchWidget search, double mouseX, double mouseY) {
        int rows = visibleRows();
        int top = togglesTop(listTop(search, rows), rows);
        if (mouseX < search.getX() || mouseX >= search.getX() + search.getWidth() || mouseY < top) return -1;
        int index = (int) ((mouseY - top) / ROW_HEIGHT);
        return index < TOGGLES.size() ? index : -1;
    }

    /** The entry under the mouse, as an index into the entries, or -1. */
    private static int entryAt(EmiSearchWidget search, List<String> entries, double mouseX, double mouseY) {
        int rows = visibleRows();
        int top = listTop(search, rows) + 1;
        if (mouseX < search.getX() || mouseX >= search.getX() + search.getWidth() || mouseY < top) return -1;
        int row = (int) ((mouseY - top) / ROW_HEIGHT);
        int index = scroll + row;
        return row < rows && index < entries.size() ? index : -1;
    }

    /** Called from EMI's foreground pass, over everything EMI draws. */
    public static void renderList(GuiGraphics graphics) {
        EmiSearchWidget search = openBar();
        if (search == null) {
            SearchOverlay.hide();
            return;
        }
        List<String> entries = entries();
        int rows = visibleRows();
        scroll = Math.max(0, Math.min(scroll, entries.size() - rows));
        int x = search.getX();
        int width = search.getWidth();
        int top = listTop(search, rows);
        int bottom = top + listHeight(rows);
        SearchOverlay.show(x - 1, top - 1, x + width + 1, bottom + 1);

        // The screen was drawn without the mouse while it is over the list; this is where it really is.
        int mouseX = SearchOverlay.mouseX();
        int mouseY = SearchOverlay.mouseY();
        int hovered = entryAt(search, entries, mouseX, mouseY);
        int hoveredToggle = toggleAt(search, mouseX, mouseY);
        Font font = Minecraft.getInstance().font;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 450);
        graphics.fill(x - 1, top - 1, x + width + 1, bottom + 1, 0xFF505050);
        graphics.fill(x, top, x + width, bottom, 0xF0101010);
        if (entries.isEmpty()) {
            graphics.drawString(font, I18n.get("sampack_emitweaks.search.no_history"), x + 4, top + 3, 0x707070);
        }
        boolean scrollable = entries.size() > rows;
        int textRight = x + width - (scrollable ? 4 : 0);
        for (int row = 0; row < rows && scroll + row < entries.size(); row++) {
            int index = scroll + row;
            int y = top + 1 + row * ROW_HEIGHT;
            boolean lit = index == hovered || index == selected;
            if (lit) graphics.fill(x, y, textRight, y + ROW_HEIGHT, 0x40FFFFFF);
            String text = entries.get(index);
            int room = textRight - x - 8;
            if (font.width(text) > room) {
                text = font.plainSubstrByWidth(text, room - font.width("...")) + "...";
            }
            boolean current = entries.get(index).equals(search.getValue());
            graphics.drawString(font, text, x + 4, y + 2, lit ? 0xFFFFFF : current ? 0xFFFFA0 : 0xA0A0A0);
        }
        if (scrollable) {
            int track = rows * ROW_HEIGHT;
            int thumb = Math.max(6, track * rows / entries.size());
            int thumbY = top + 1 + (track - thumb) * scroll / (entries.size() - rows);
            graphics.fill(x + width - 3, top + 1, x + width - 1, top + 1 + track, 0xFF303030);
            graphics.fill(x + width - 3, thumbY, x + width - 1, thumbY + thumb, 0xFF909090);
        }

        int togglesTop = togglesTop(top, rows);
        graphics.fill(x + 2, togglesTop - 2, x + width - 2, togglesTop - 1, 0xFF505050);
        for (int i = 0; i < TOGGLES.size(); i++) {
            Toggle toggle = TOGGLES.get(i);
            int y = togglesTop + i * ROW_HEIGHT;
            boolean lit = i == hoveredToggle;
            if (lit) graphics.fill(x, y, x + width, y + ROW_HEIGHT, 0x40FFFFFF);
            // A 7x7 box, filled while on.
            int boxX = x + 4;
            int boxY = y + 2;
            graphics.fill(boxX, boxY, boxX + 7, boxY + 7, lit ? 0xFFFFFFFF : 0xFFA0A0A0);
            graphics.fill(boxX + 1, boxY + 1, boxX + 6, boxY + 6, 0xFF101010);
            if (toggle.value().get()) graphics.fill(boxX + 2, boxY + 2, boxX + 5, boxY + 5, 0xFF80FF80);
            String label = I18n.get("sampack_emitweaks.configuration." + toggle.key());
            int room = width - 18;
            if (font.width(label) > room) label = font.plainSubstrByWidth(label, room - font.width("...")) + "...";
            graphics.drawString(font, label, x + 15, y + 2, lit ? 0xFFFFFF : 0xA0A0A0);
        }
        graphics.pose().popPose();
    }

    private static void pick(EmiSearchWidget search, String entry) {
        search.setValue(entry);
        search.moveCursorToEnd(false);
        SearchHistory.record(entry);
        close();
    }

    // ---- Input, ahead of EMI's own handling; true consumes it ----

    public static boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean taken = takeClick(mouseX, mouseY, button);
        swallowRelease = taken;
        return taken;
    }

    private static boolean takeClick(double mouseX, double mouseY, int button) {
        EmiSearchWidget list = openBar();
        if (list != null && isOverList(list, mouseX, mouseY)) {
            List<String> entries = entries();
            int index = entryAt(list, entries, mouseX, mouseY);
            int toggle = toggleAt(list, mouseX, mouseY);
            if (index >= 0 && button == 0) {
                pick(list, entries.get(index));
            } else if (index >= 0 && button == 1) {
                SearchHistory.remove(entries.get(index));
                selected = -1;
            } else if (toggle >= 0 && button == 0) {
                ModConfigSpec.BooleanValue value = TOGGLES.get(toggle).value();
                value.set(!value.get());
                ClientConfig.SPEC.save();
            }
            return true;
        }

        EmiSearchWidget search = activeBar();
        if (search != null && button == 0 && isOverArrow(search, mouseX, mouseY)) {
            if (open) {
                close();
            } else {
                open();
            }
            return true;
        }
        if (search != null && button == 0 && isOverClear(search, mouseX, mouseY)) {
            clear();
            return true;
        }
        // Anywhere else only closes the list; a click on the search bar still goes on, to focus it.
        if (list != null) {
            close();
            return !list.isMouseOver(mouseX, mouseY);
        }
        return false;
    }

    public static boolean mouseReleased(double mouseX, double mouseY) {
        if (swallowRelease) {
            swallowRelease = false;
            return true;
        }
        EmiSearchWidget list = openBar();
        return list != null && isOverList(list, mouseX, mouseY);
    }

    public static boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        EmiSearchWidget list = openBar();
        if (list == null || !isOverList(list, mouseX, mouseY)) return false;
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(amount), entries().size() - visibleRows()));
        return true;
    }

    /** Escape closes the list, the arrow keys move through it and Enter searches the chosen entry. */
    public static boolean keyPressed(int keyCode) {
        EmiSearchWidget list = openBar();
        if (list == null) return false;
        List<String> entries = entries();
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> close();
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                if (entries.isEmpty()) return true;
                int step = keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1;
                selected = selected < 0 ? (step > 0 ? 0 : entries.size() - 1)
                        : Math.floorMod(selected + step, entries.size());
                int rows = visibleRows();
                if (selected < scroll) scroll = selected;
                if (selected >= scroll + rows) scroll = selected - rows + 1;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (selected >= 0 && selected < entries.size()) {
                    pick(list, entries.get(selected));
                } else {
                    close();
                }
            }
            default -> {
                // Typing still goes to the search bar, which narrows the list if filtering is on.
                selected = -1;
                return false;
            }
        }
        return true;
    }
}
