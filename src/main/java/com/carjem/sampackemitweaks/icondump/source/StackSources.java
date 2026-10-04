package com.carjem.sampackemitweaks.icondump.source;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * What gets exported, in order: everything EMI lists (or, without EMI, the creative tabs and
 * fluids, as IconExporter does), then every registered item none of those showed.
 *
 * The second pass is what Item Obliterator hides, items no creative tab carries, and debug or
 * technical items. Each is rendered as its default stack and keyed by its bare `item:ns:path`
 * id, the same form emi_dump.json's `registry` list uses.
 */
public final class StackSources {

    /** Draws one 16x16 icon at (0, 0) in the current pose. */
    @FunctionalInterface
    public interface IconRenderer {
        void render(GuiGraphics graphics);
    }

    /**
     * @param id        the key in meta.json: the EMI serialized id, e.g. item:minecraft:stone
     * @param unlisted  true for a registry item that neither EMI nor a creative tab showed
     */
    public record Entry(String id, String namespace, String name, boolean unlisted, IconRenderer renderer) {
    }

    public record Collected(String source, List<Entry> entries) {
    }

    private StackSources() {
    }

    public static boolean emiPresent() {
        return ModList.get().isLoaded("emi");
    }

    /** Null when EMI is installed but has not finished loading its stack list. */
    public static String notReady() {
        return emiPresent() && !EmiSource.ready() ? "EMI is still loading its stacks; try again in a moment" : null;
    }

    /**
     * @param wantNamespace which mods to export, by namespace
     * @param wantId        which stacks to export, by the id they are keyed by. Applied last, so a
     *                      stack it leaves out is still known to have been listed, and never comes
     *                      back as an unlisted registry item.
     */
    public static Collected collect(Predicate<String> wantNamespace, Predicate<String> wantId) {
        boolean emi = emiPresent();
        List<Entry> entries = new ArrayList<>(emi ? EmiSource.collect(wantNamespace) : VanillaSource.collect(wantNamespace));
        Function<ItemStack, Entry> unlisted = emi ? EmiSource::unlisted : VanillaSource::unlisted;

        Set<String> seen = new HashSet<>();
        for (Entry entry : entries) {
            seen.add(entry.id());
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR || !wantNamespace.test(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                continue;
            }
            // seen by bare id only: an item EMI shows solely as component variants (every potion
            // is a potion{...}) still gets its default stack, which is what a base-id lookup finds
            if (seen.contains("item:" + BuiltInRegistries.ITEM.getKey(item))) {
                continue;
            }
            Entry entry = unlisted.apply(new ItemStack(item));
            if (entry != null && seen.add(entry.id())) {
                entries.add(entry);
            }
        }
        entries.removeIf(entry -> !wantId.test(entry.id()));
        return new Collected(emi ? "emi" : "creative", entries);
    }

    static String nameOf(Supplier<Component> name) {
        try {
            return name.get().getString();
        } catch (RuntimeException e) {
            return "";
        }
    }
}
