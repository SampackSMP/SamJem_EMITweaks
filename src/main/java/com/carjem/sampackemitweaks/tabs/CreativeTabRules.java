package com.carjem.sampackemitweaks.tabs;

import com.carjem.sampackemitweaks.SampackEmiTweaks;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The pack's creative tabs, read from {@code config/sampack_emitweaks/creative_tabs.json}, which
 * InvIndexLedger's {@code build} writes. Replaces the Recreative rules file the pack used before.
 *
 * The file is a json array of rules, in the format Recreative read:
 * <ul>
 *   <li>{@code {"action": "custom_tab", "tabs": [id], "name": .., "icon": item id, "items": [..]}}
 *   adds a tab. Each item is an id, or {@code {"item": id, "components": patch}} where the patch is
 *   a data component patch as json, or as a string holding that json.</li>
 *   <li>{@code {"action": "remove_tab", "tabs": [ids]}} hides tabs.</li>
 *   <li>{@code {"action": "tab_order", "order": [ids]}} puts the named tabs first, in that order.
 *   Every other tab follows in NeoForge's order.</li>
 * </ul>
 * Recreative's {@code modify_tab}, {@code #tag} entries, png icons and item placement anchors
 * are not supported; the ledger never writes them.
 *
 * The custom tabs are registered for real (client only; the creative tab registry is not synced),
 * so everything that walks the registry sees them. {@link com.carjem.sampackemitweaks.pristine.PristineTabs}
 * leaves them out, so EMI's index and IconDump still see only the game's own tabs.
 *
 * The file is read at startup, and again by {@link CreativeTabReload}. A reload applies everything
 * except a custom tab id that was not registered at startup, which needs a restart.
 */
public final class CreativeTabRules {
    public static final String FILE = "creative_tabs.json";

    /** The tabs the inventory screen is built from, plus op_blocks; never reordered or hidden. */
    private static final Set<ResourceLocation> SPECIAL_TABS = Set.of(
            ResourceLocation.withDefaultNamespace("search"),
            ResourceLocation.withDefaultNamespace("inventory"),
            ResourceLocation.withDefaultNamespace("hotbar"),
            ResourceLocation.withDefaultNamespace("op_blocks"));

    public record CustomTab(ResourceLocation id, String name, ResourceLocation icon, List<Entry> items) {
    }

    public record Entry(ResourceLocation item, @Nullable JsonElement components) {
    }

    private static final CreativeTabRules EMPTY = new CreativeTabRules(Map.of(), Set.of(), Map.of());
    private static volatile CreativeTabRules current = EMPTY;
    // The custom tabs registered at startup. Fixed from then on: a reload can change what they
    // hold, but not add or remove one.
    private static final Set<ResourceLocation> REGISTERED = new HashSet<>();

    private final Map<ResourceLocation, CustomTab> customTabs;
    private final Set<ResourceLocation> removed;
    private final Map<ResourceLocation, Integer> order;

    private CreativeTabRules(Map<ResourceLocation, CustomTab> customTabs, Set<ResourceLocation> removed,
                             Map<ResourceLocation, Integer> order) {
        this.customTabs = customTabs;
        this.removed = removed;
        this.order = order;
    }

    public static CreativeTabRules get() {
        return current;
    }

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(SampackEmiTweaks.MOD_ID).resolve(FILE);
    }

    public boolean isEmpty() {
        return customTabs.isEmpty() && removed.isEmpty() && order.isEmpty();
    }

    /** True for a tab this class registered, whether or not the current rules still define it. */
    public static boolean isOwnTab(@Nullable ResourceLocation id) {
        return id != null && REGISTERED.contains(id);
    }

    public @Nullable CustomTab customTab(ResourceLocation id) {
        return customTabs.get(id);
    }

    /** The custom tab ids the rules define but that were not registered at startup. */
    public List<ResourceLocation> unregisteredCustomTabs() {
        return customTabs.keySet().stream().filter(id -> !REGISTERED.contains(id)).toList();
    }

    public boolean isRemoved(CreativeModeTab tab) {
        if (removed.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        return id != null && removed.contains(id);
    }

    /**
     * The tabs in the configured order: tabs named by tab_order first, in that order, then the
     * rest in the order given, then the special tabs. Removed tabs are dropped.
     */
    public List<CreativeModeTab> sort(List<CreativeModeTab> tabs) {
        if (isEmpty()) return tabs;

        Map<CreativeModeTab, Integer> rank = new HashMap<>();
        List<CreativeModeTab> sorted = new ArrayList<>(tabs.size());
        for (CreativeModeTab tab : tabs) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id != null && removed.contains(id)) continue;
            int r;
            if (id != null && SPECIAL_TABS.contains(id)) r = Integer.MAX_VALUE;
            else if (id != null && order.containsKey(id)) r = order.get(id);
            else r = Integer.MAX_VALUE - 1;
            rank.put(tab, r);
            sorted.add(tab);
        }
        // stable, so the unnamed tabs keep the order they came in
        sorted.sort(Comparator.comparingInt(rank::get));
        return List.copyOf(sorted);
    }

    /** Reads the rules file; a missing or unreadable file leaves every tab as the game made it. */
    public static void load() {
        Path path = path();
        if (!Files.exists(path)) {
            SampackEmiTweaks.LOGGER.info("No creative tab rules at {}; the tabs are left as the game made them", path);
            current = EMPTY;
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            current = parse(JsonParser.parseReader(reader));
            SampackEmiTweaks.LOGGER.info("Loaded creative tab rules from {}: {} custom tabs, {} hidden, {} ordered",
                    path, current.customTabs.size(), current.removed.size(), current.order.size());
        } catch (Exception e) {
            SampackEmiTweaks.LOGGER.error("Could not read creative tab rules from {}; the tabs are left as they are", path, e);
            current = EMPTY;
        }
    }

    private static CreativeTabRules parse(JsonElement json) {
        Map<ResourceLocation, CustomTab> customTabs = new LinkedHashMap<>();
        Set<ResourceLocation> removed = new HashSet<>();
        Map<ResourceLocation, Integer> order = new HashMap<>();

        JsonArray rules = json.isJsonArray() ? json.getAsJsonArray() : new JsonArray();
        if (json.isJsonObject()) rules.add(json);
        for (JsonElement element : rules) {
            if (!element.isJsonObject()) continue;
            JsonObject rule = element.getAsJsonObject();
            String action = rule.has("action") ? rule.get("action").getAsString().toLowerCase(Locale.ROOT) : "";
            switch (action) {
                case "custom_tab" -> {
                    String name = rule.has("name") ? rule.get("name").getAsString() : null;
                    ResourceLocation icon = rule.has("icon") ? ResourceLocation.tryParse(rule.get("icon").getAsString()) : null;
                    List<Entry> items = new ArrayList<>();
                    for (JsonElement item : asList(rule.get("items"))) {
                        Entry entry = parseEntry(item);
                        if (entry != null) items.add(entry);
                    }
                    for (ResourceLocation id : ids(rule.get("tabs"))) {
                        customTabs.put(id, new CustomTab(id, name != null ? name : id.toString(),
                                icon != null ? icon : BuiltInRegistries.ITEM.getDefaultKey(), List.copyOf(items)));
                    }
                }
                case "remove_tab" -> removed.addAll(ids(rule.get("tabs")));
                case "tab_order" -> {
                    for (ResourceLocation id : ids(rule.get("order"))) {
                        order.putIfAbsent(id, order.size());
                    }
                }
                default -> SampackEmiTweaks.LOGGER.warn("Ignoring creative tab rule with unsupported action '{}'", action);
            }
        }
        // a tab this file defines is never also hidden by it
        removed.removeAll(customTabs.keySet());
        removed.removeAll(SPECIAL_TABS);
        // insertion order kept: the tabs register in the order the file lists them
        return new CreativeTabRules(Collections.unmodifiableMap(customTabs), Set.copyOf(removed), Map.copyOf(order));
    }

    private static @Nullable Entry parseEntry(JsonElement json) {
        if (json.isJsonPrimitive()) {
            ResourceLocation id = ResourceLocation.tryParse(json.getAsString());
            return id != null ? new Entry(id, null) : null;
        }
        if (json.isJsonObject() && json.getAsJsonObject().has("item")) {
            JsonObject object = json.getAsJsonObject();
            ResourceLocation id = ResourceLocation.tryParse(object.get("item").getAsString());
            if (id == null) return null;
            JsonElement components = object.get("components");
            if (components != null && components.isJsonPrimitive()) {
                components = JsonParser.parseString(components.getAsString());
            }
            return new Entry(id, components == null || components.isJsonNull() ? null : components);
        }
        return null;
    }

    private static List<JsonElement> asList(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull()) return List.of();
        if (json.isJsonArray()) return json.getAsJsonArray().asList();
        return List.of(json);
    }

    private static List<ResourceLocation> ids(@Nullable JsonElement json) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (JsonElement element : asList(json)) {
            ResourceLocation id = element.isJsonPrimitive() ? ResourceLocation.tryParse(element.getAsString()) : null;
            if (id != null) ids.add(id);
        }
        return ids;
    }

    /**
     * Registers the custom tabs. An id another mod already registered is left to that mod; its
     * rule is skipped with a warning rather than crashing on the duplicate.
     */
    public static void register(RegisterEvent event) {
        event.register(Registries.CREATIVE_MODE_TAB, helper -> {
            for (CustomTab custom : current.customTabs.values()) {
                if (BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(custom.id())) {
                    SampackEmiTweaks.LOGGER.warn("Creative tab {} already exists; its custom_tab rule is ignored", custom.id());
                    continue;
                }
                // name, icon and items are read from the current rules, so a reload reaches them
                ResourceLocation id = custom.id();
                helper.register(id, CreativeModeTab.builder()
                        .title(Component.translatable(custom.name()))
                        .icon(() -> {
                            CustomTab now = current.customTab(id);
                            return now != null ? new ItemStack(BuiltInRegistries.ITEM.get(now.icon())) : ItemStack.EMPTY;
                        })
                        .displayItems((parameters, output) -> {
                            CustomTab now = current.customTab(id);
                            if (now != null) fill(now, parameters, output);
                        })
                        .build());
                REGISTERED.add(id);
            }
        });
    }

    private static void fill(CustomTab custom, CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, parameters.holders());
        // Two entries can still make the same stack (a patch that matches the item's defaults);
        // the tab builder throws on a repeat, so skip it here.
        Set<ItemStack> added = ItemStackLinkedSet.createTypeAndComponentsSet();
        for (Entry entry : custom.items()) {
            Item item = BuiltInRegistries.ITEM.getOptional(entry.item()).orElse(null);
            if (item == null) continue;
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) continue;
            if (entry.components() != null) {
                DataComponentPatch.CODEC.parse(ops, entry.components())
                        .resultOrPartial(error -> SampackEmiTweaks.LOGGER.warn(
                                "Bad components for {} in creative tab {}: {}", entry.item(), custom.id(), error))
                        .ifPresent(stack::applyComponents);
            }
            if (added.add(stack)) {
                output.accept(stack);
            }
        }
    }
}
