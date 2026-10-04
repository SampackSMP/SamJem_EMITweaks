package com.carjem.sampackemitweaks.pristine;

import dev.emi.emi.api.stack.EmiStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * EMI's index as the game and its plugins built it, before any resource pack's index data is
 * applied.
 *
 * EmiStackList.bake() drops the stacks EMI plugins invalidated, then applies every
 * assets/emi/index/stacks file -- InvIndexLedger's own output among them -- removing, filtering
 * and re-placing stacks. EmiStackList.stacks afterwards is the pack's order, so a dump of it hands
 * the pack its own output back. This is the list from between those two steps, recorded by
 * {@link com.carjem.sampackemitweaks.mixin.emi.EmiStackListPristineIndexMixin}. With
 * {@link PristineTabs} feeding EMI's creative-tab source, nothing the pack's creative tab rules or the index data
 * does reaches it.
 *
 * Only loaded when EMI is. Public API: other mods (IconDump) read this by reflection; keep the
 * signatures stable.
 */
public final class PristineEmiIndex {
    private static volatile List<EmiStack> stacks;

    private PristineEmiIndex() {
    }

    public static void record(List<EmiStack> current) {
        stacks = List.copyOf(current);
    }

    /** The list from EMI's last bake, or null if EMI has not baked since the game started. */
    public static @Nullable List<EmiStack> stacks() {
        return stacks;
    }
}
