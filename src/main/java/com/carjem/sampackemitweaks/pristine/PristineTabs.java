package com.carjem.sampackemitweaks.pristine;

import com.carjem.sampackemitweaks.mixin.pristine.CreativeModeTabRegistryAccessor;
import com.carjem.sampackemitweaks.tabs.CreativeTabRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every registered creative tab's contents as the tab itself and BuildCreativeModeTabContentsEvent
 * made them, before anything rewrites them.
 *
 * The pack's own creative tab rules ({@link CreativeTabRules}, written by InvIndexLedger) add tabs,
 * hide tabs and reorder them. Anything that reads the live tabs -- EMI's index, IconDump's dumps,
 * and through them InvIndexLedger -- would then get the pack's own output back. This keeps each
 * tab's contents as built, recorded by
 * {@link com.carjem.sampackemitweaks.mixin.pristine.CreativeModeTabPristineMixin} during the builds
 * that already happen, and lists the tabs in the game's own order.
 *
 * The custom tabs {@link CreativeTabRules} registers are left out of everything here.
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
        if (id == null || CreativeTabRules.isOwnTab(id)) {
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

    /** Every registered tab but the pack's custom ones, in registry order: vanilla's allTabs() without them. */
    public static List<CreativeModeTab> registryOrder() {
        return withoutCustomTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList());
    }

    public static List<ResourceLocation> registryOrderIds() {
        return ids(registryOrder());
    }

    /**
     * The tabs in the order NeoForge sorts them for the creative screen (before/after constraints
     * resolved), read from its own list rather than getSortedCreativeModeTabs(), whose return the
     * tab rules rewrite. Excludes the hotbar, search, op and inventory tabs, as NeoForge does, and
     * the pack's custom tabs.
     */
    public static List<CreativeModeTab> displayOrder() {
        return withoutCustomTabs(CreativeModeTabRegistryAccessor.sampack_emitweaks$getSortedTabs());
    }

    public static List<ResourceLocation> displayOrderIds() {
        return ids(displayOrder());
    }

    /**
     * Every tab: {@link #displayOrder()}, then the ones it leaves out (search, hotbar, inventory,
     * op blocks) in registry order. The pack's custom tabs are left out.
     */
    public static List<CreativeModeTab> order() {
        LinkedHashSet<CreativeModeTab> tabs = new LinkedHashSet<>(displayOrder());
        tabs.addAll(registryOrder());
        return List.copyOf(tabs);
    }

    /** The tab's items as last built, or its live display items if it has not been built since startup. */
    public static Collection<ItemStack> displayItems(CreativeModeTab tab) {
        Snapshot snapshot = get(tab);
        return snapshot != null ? snapshot.displayItems() : tab.getDisplayItems();
    }

    private static List<CreativeModeTab> withoutCustomTabs(List<CreativeModeTab> tabs) {
        return tabs.stream()
                .filter(tab -> !CreativeTabRules.isOwnTab(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab)))
                .toList();
    }

    public static List<ResourceLocation> ids(List<CreativeModeTab> tabs) {
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
