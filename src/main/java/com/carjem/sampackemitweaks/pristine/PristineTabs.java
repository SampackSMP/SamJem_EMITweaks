package com.carjem.sampackemitweaks.pristine;

import com.carjem.sampackemitweaks.mixin.pristine.CreativeModeTabRegistryAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every registered creative tab's contents as the tab itself and BuildCreativeModeTabContentsEvent
 * made them, before anything rewrites them.
 *
 * Recreative rewrites each tab at the tail of {@code CreativeModeTab.buildContents}, and reorders,
 * extends and filters {@code CreativeModeTabs.allTabs()} and NeoForge's sorted tab list. Anything
 * that reads the live tabs after that -- EMI's index, IconDump's dumps, and through them
 * InvIndexLedger -- gets the pack's own output back. This keeps the copy from just before that,
 * recorded by {@link com.carjem.sampackemitweaks.mixin.pristine.CreativeModeTabPristineMixin}
 * during the builds that already happen. Recreative's own reloadTabs() rebuilds therefore record
 * the same data every time.
 *
 * Tabs Recreative creates at runtime are never registered, so they never appear here.
 *
 * Public API: other mods (IconDump) read this by reflection; keep the signatures stable.
 */
public final class PristineTabs {

    /**
     * One tab as last built.
     *
     * @param displayItems what the tab itself shows, in order
     * @param searchItems  what it contributes to the search tab, in order; what EMI's index reads
     * @param generation   the {@link #generation()} this build was recorded at
     */
    public record Snapshot(ResourceLocation id, List<ItemStack> displayItems, List<ItemStack> searchItems,
                           CreativeModeTab.ItemDisplayParameters parameters, int generation) {
    }

    private static final Map<ResourceLocation, Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();
    private static volatile int generation;

    private PristineTabs() {
    }

    public static void record(CreativeModeTab tab, CreativeModeTab.ItemDisplayParameters parameters,
                              Collection<ItemStack> displayItems, Collection<ItemStack> searchItems) {
        ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (id == null) {
            return;
        }
        // copied: the collections are the ones the tab keeps, and a later hook may edit them in place
        SNAPSHOTS.put(id, new Snapshot(id, List.copyOf(displayItems), List.copyOf(searchItems),
                parameters, ++generation));
    }

    /** The tab's last recorded build, or null if it was never built or is not registered. */
    public static @Nullable Snapshot get(ResourceLocation id) {
        return SNAPSHOTS.get(id);
    }

    public static @Nullable Snapshot get(CreativeModeTab tab) {
        ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        return id != null ? SNAPSHOTS.get(id) : null;
    }

    /** Bumped by every tab build, so a reader can tell whether anything was rebuilt since. */
    public static int generation() {
        return generation;
    }

    /** Every registered tab, in registry order: what vanilla's allTabs() returns. */
    public static List<CreativeModeTab> registryOrder() {
        return BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList();
    }

    public static List<ResourceLocation> registryOrderIds() {
        return ids(registryOrder());
    }

    /**
     * The tabs in the order NeoForge sorts them for the creative screen (before/after constraints
     * resolved), read from its own list rather than getSortedCreativeModeTabs(), whose return
     * Recreative rewrites. Excludes the hotbar, search, op and inventory tabs, as NeoForge does.
     */
    public static List<CreativeModeTab> displayOrder() {
        return List.copyOf(CreativeModeTabRegistryAccessor.sampack_emitweaks$getSortedTabs());
    }

    public static List<ResourceLocation> displayOrderIds() {
        return ids(displayOrder());
    }

    private static List<ResourceLocation> ids(List<CreativeModeTab> tabs) {
        List<ResourceLocation> ids = new ArrayList<>(tabs.size());
        for (CreativeModeTab tab : tabs) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }
}
