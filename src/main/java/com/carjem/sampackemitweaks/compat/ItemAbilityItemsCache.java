package com.carjem.sampackemitweaks.compat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * The items that can perform each ItemAbility, found by one scan of the item
 * registry per ability. Cleared whenever tags reload, since some tools decide
 * abilities through tags.
 */
public final class ItemAbilityItemsCache {
    private static final Map<ItemAbility, List<ItemStack>> ITEMS = new ConcurrentHashMap<>();

    private ItemAbilityItemsCache() {
    }

    /** Returns shared stacks; callers must copy before handing them out. */
    public static List<ItemStack> itemsFor(ItemAbility ability) {
        return ITEMS.computeIfAbsent(ability, a -> BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new)
                .filter(stack -> stack.canPerformAction(a))
                .toList());
    }

    public static void clear() {
        ITEMS.clear();
    }
}
