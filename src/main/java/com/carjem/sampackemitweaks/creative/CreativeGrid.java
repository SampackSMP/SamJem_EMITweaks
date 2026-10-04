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
 * whose group has just one item in the list is shown on its own.
 *
 * <p>The grid is a list position per slot. A slot's position is its index in the grid plus the
 * first item of the top row, which {@link #setFirstRow} records whenever the menu scrolls, so
 * every lookup is an array read.
 */
public final class CreativeGrid {
    /**
     * One collapsible group, as {@link CreativeContents} found it.
     *
     * @param id   what the expanded state is kept by; the same group must keep the same id
     *             across reloads
     */
    public record GroupKey(Object id, Component name) {
    }

    private static final class Run {
        final GroupKey key;
        final List<ItemStack> items = new ArrayList<>();
        boolean placed;

        Run(GroupKey key) {
            this.key = key;
        }
    }

    // Kept for the session, like REMI's: a group stays open across tabs, searches and screens.
    private static final Set<Object> EXPANDED = new HashSet<>();

    private static boolean active;
    // Each entry is an ItemStack or a Run.
    private static List<Object> entries = List.of();
    private static List<ItemStack> shown = List.of();
    private static Run[] runAt = new Run[0];
    private static boolean[] headerAt = new boolean[0];
    private static int firstPosition;

    private CreativeGrid() {
    }

    /**
     * Lays out an item tab and returns what its grid shows.
     *
     * @param groups each item's group, by position; null for none
     */
    public static List<ItemStack> layout(List<ItemStack> items, GroupKey[] groups) {
        Map<GroupKey, Run> runs = new IdentityHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            GroupKey key = groups[i];
            if (key != null) {
                runs.computeIfAbsent(key, Run::new).items.add(items.get(i));
            }
        }

        List<Object> laidOut = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            Run run = groups[i] != null ? runs.get(groups[i]) : null;
            if (run == null || run.items.size() < 2) {
                laidOut.add(items.get(i));
            } else if (!run.placed) {
                run.placed = true;
                laidOut.add(run);
            }
        }

        entries = laidOut;
        active = true;
        render();
        return shown;
    }

    /** The open tab shows no items, or is not grouped. */
    public static void clear() {
        active = false;
        entries = List.of();
        shown = List.of();
        runAt = new Run[0];
        headerAt = new boolean[0];
    }

    private static void render() {
        int size = 0;
        for (Object entry : entries) {
            size += entry instanceof Run run && EXPANDED.contains(run.key.id()) ? 1 + run.items.size() : 1;
        }

        List<ItemStack> items = new ArrayList<>(size);
        Run[] runs = new Run[size];
        boolean[] headers = new boolean[size];
        for (Object entry : entries) {
            if (entry instanceof Run run) {
                runs[items.size()] = run;
                headers[items.size()] = true;
                items.add(run.items.getFirst());
                if (EXPANDED.contains(run.key.id())) {
                    for (ItemStack item : run.items) {
                        runs[items.size()] = run;
                        items.add(item);
                    }
                }
            } else {
                items.add((ItemStack) entry);
            }
        }

        shown = items;
        runAt = runs;
        headerAt = headers;
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
        return position >= 0 && headerAt[position];
    }

    /** True for an item shown inside an expanded group (its header included). */
    public static boolean isInExpandedGroup(int position) {
        return position >= 0 && runAt[position] != null && EXPANDED.contains(runAt[position].key.id());
    }

    public static boolean isExpanded(int position) {
        return isHeader(position) && EXPANDED.contains(runAt[position].key.id());
    }

    /** The header's tooltip: the group's name and size. */
    public static List<Component> tooltip(int position) {
        Run run = runAt[position];
        return List.of(run.key.name(),
                Component.translatable("sampack_emitweaks.creative.group_size", run.items.size())
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    /** Opens or closes the group whose header is at the position; returns what the grid now shows. */
    public static List<ItemStack> toggle(int position) {
        Object id = runAt[position].key.id();
        if (!EXPANDED.remove(id)) {
            EXPANDED.add(id);
        }
        render();
        return shown;
    }
}
