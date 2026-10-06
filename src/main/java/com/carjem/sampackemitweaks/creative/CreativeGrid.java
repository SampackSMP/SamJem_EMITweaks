package com.carjem.sampackemitweaks.creative;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The open item tab's grid, with its collapsible groups, laid out the way REMI lays out EMI's
 * index: a group with at least two of its items in the list is shown once, as its first item,
 * where that item would have been; its other items follow it only while it is expanded. An item
 * whose group has just one item in the list is shown on its own. A group can sit inside another
 * ({@link GroupKey#parent}); it is then laid out inside it the same way (see {@link GroupTree}).
 *
 * <p>The grid is a list position per slot. A slot's position is its index in the grid plus the
 * first item of the top row, which {@link #setFirstRow} records whenever the menu scrolls, so
 * every lookup is an array read.
 */
public final class CreativeGrid {
    /**
     * One collapsible group, as {@link CreativeContents} found it.
     *
     * @param id     what the expanded state is kept by; the same group must keep the same id
     *               across reloads
     * @param icon   what its collapsed slot shows; null for the configured default
     * @param parent the group it sits inside; null for none
     */
    public record GroupKey(Object id, Component name, @Nullable GroupIcon icon, @Nullable GroupKey parent) {
    }

    private record Run(GroupKey key, List<ItemStack> items, List<Object> children) {
    }

    // Kept for the session, like REMI's: a group stays open across tabs, searches and screens.
    private static final Set<Object> EXPANDED = new HashSet<>();

    private static boolean active;
    // Each entry is an ItemStack or a Run.
    private static List<Object> entries = List.of();
    private static List<ItemStack> shown = List.of();
    // The group whose header is at a position; null for an item.
    private static Run[] headerAt = new Run[0];
    // True for a position inside an expanded group, an expanded header included.
    private static boolean[] insideAt = new boolean[0];
    private static int firstPosition;

    private CreativeGrid() {
    }

    /**
     * Lays out an item tab and returns what its grid shows.
     *
     * @param groups each item's group, by position; null for none
     */
    public static List<ItemStack> layout(List<ItemStack> items, GroupKey[] groups) {
        Map<ItemStack, GroupKey> groupOf = new IdentityHashMap<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            if (groups[i] != null) groupOf.put(items.get(i), groups[i]);
        }
        entries = runs(GroupTree.build(items, groupOf::get, GroupKey::parent));
        active = true;
        render();
        return shown;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> runs(List<Object> nodes) {
        List<Object> result = new ArrayList<>(nodes.size());
        for (Object node : nodes) {
            if (node instanceof GroupTree.Node<?, ?> group) {
                GroupTree.Node<ItemStack, GroupKey> typed = (GroupTree.Node<ItemStack, GroupKey>) group;
                result.add(new Run(typed.group(), typed.items(), runs(typed.children())));
            } else {
                result.add(node);
            }
        }
        return result;
    }

    /** The open tab shows no items, or is not grouped. */
    public static void clear() {
        active = false;
        entries = List.of();
        shown = List.of();
        headerAt = new Run[0];
        insideAt = new boolean[0];
    }

    private static void render() {
        List<ItemStack> items = new ArrayList<>();
        List<Run> headers = new ArrayList<>();
        List<Boolean> inside = new ArrayList<>();
        render(entries, false, items, headers, inside);

        boolean[] insideArray = new boolean[inside.size()];
        for (int i = 0; i < insideArray.length; i++) insideArray[i] = inside.get(i);
        shown = items;
        headerAt = headers.toArray(new Run[0]);
        insideAt = insideArray;
    }

    private static void render(List<Object> entries, boolean inExpanded, List<ItemStack> items, List<Run> headers, List<Boolean> inside) {
        for (Object entry : entries) {
            if (entry instanceof Run run) {
                boolean expanded = EXPANDED.contains(run.key().id());
                items.add(run.items().getFirst());
                headers.add(run);
                inside.add(inExpanded || expanded);
                if (expanded) render(run.children(), true, items, headers, inside);
            } else {
                items.add((ItemStack) entry);
                headers.add(null);
                inside.add(inExpanded);
            }
        }
    }

    /** Called with the top row whenever the menu scrolls. */
    public static void setFirstRow(int row) {
        firstPosition = row * CreativeLayout.get().columns;
    }

    /** The slot's position in the list, or -1 if it is not an item grid slot of a grouped tab. */
    public static int position(@Nullable Slot slot) {
        if (!active || slot == null || slot.container != CreativeModeInventoryScreen.CONTAINER) return -1;
        int position = firstPosition + slot.getContainerSlot();
        return position >= 0 && position < shown.size() ? position : -1;
    }

    public static boolean isHeader(int position) {
        return position >= 0 && headerAt[position] != null;
    }

    /**
     * True for a slot shown inside an expanded group: its items, its expanded header, and the
     * headers of the groups inside it, open or not.
     */
    public static boolean isInExpandedGroup(int position) {
        return position >= 0 && insideAt[position];
    }

    public static boolean isExpanded(int position) {
        return isHeader(position) && EXPANDED.contains(headerAt[position].key().id());
    }

    /**
     * Draws the header's icon, or the configured default, in place of its first item.
     *
     * @return false if that is its first item, so the slot draws it as usual
     */
    public static boolean renderIcon(net.minecraft.client.gui.GuiGraphics graphics, int position, int x, int y) {
        if (!isHeader(position)) return false;
        Run run = headerAt[position];
        GroupIcon icon = run.key().icon();
        if (icon == null) icon = com.carjem.sampackemitweaks.client.ClientConfig.get(
                com.carjem.sampackemitweaks.client.ClientConfig.CREATIVE_GROUP_ICON).icon();
        if (icon.kind == GroupIcon.Kind.FIRST) return false;
        icon.render(graphics, x, y, run.items());
        return true;
    }

    /** The header's tooltip: the group's name and size. */
    public static List<Component> tooltip(int position) {
        Run run = headerAt[position];
        return List.of(run.key().name(),
                Component.translatable("sampack_emitweaks.creative.group_size", run.items().size())
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    /** Opens or closes the group whose header is at the position; returns what the grid now shows. */
    public static List<ItemStack> toggle(int position) {
        Object id = headerAt[position].key().id();
        if (!EXPANDED.remove(id)) {
            EXPANDED.add(id);
        }
        render();
        return shown;
    }
}
