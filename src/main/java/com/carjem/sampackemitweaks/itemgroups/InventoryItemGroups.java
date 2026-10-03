package com.carjem.sampackemitweaks.itemgroups;

import com.carjem.sampackemitweaks.itemgroups.config.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;

import java.util.*;

/**
 * Collapsible item groups in the creative inventory, ported from Bizarre Cube's Inventory Item
 * Groups (MIT, see LICENSE-InventoryItemGroups). Client-only; the mixins under
 * {@link com.carjem.sampackemitweaks.mixin.itemgroups} drive it.
 */
public final class InventoryItemGroups {
    /**
     * Namespace for this feature's sprites, translations and config files. It keeps the original
     * Inventory Item Groups mod id so existing configs and resource-pack translations still apply.
     */
    public static final String NAMESPACE = "inventory_item_groups";

    public static ArrayList<Group> groups = new ArrayList<>();
    public static ArrayList<RawGroup> rawDefaultGroups = new ArrayList<>();
    public static CreativeModeTab selectedTab;
    public static Group pendingGroup = null;
    public static ArrayList<ItemStack> tempItemStacks = new ArrayList<>();

    // Lookup tables rebuilt by setIndexes() whenever tempItemStacks changes. They replace the
    // linear scans that used to run for every slot on every frame.
    private static final Map<ItemStack, Integer> firstIndexes = new IdentityHashMap<>();
    private static final Map<ItemStack, Integer> lastIndexes = new IdentityHashMap<>();
    private static Group[] groupsByIndex = new Group[0];

    // Matching an item's name against every group's patterns is the expensive part of
    // createGroups(), and selectTab() reruns it on every single tab click -- including
    // clicking back to a tab you were just on. Cache the matched raw groups per tab so that
    // only rebuilds when the config actually changes or the tab's display items are
    // regenerated (e.g. a resource/data reload), instead of on every click.
    private static final Map<CreativeModeTab, CachedRawGroups> rawGroupsCache = new IdentityHashMap<>();
    private static Config cachedConfig = null;

    private static final class CachedRawGroups {
        final Collection<ItemStack> displayItems;
        final ArrayList<RawGroup> rawGroups;

        CachedRawGroups(Collection<ItemStack> displayItems, ArrayList<RawGroup> rawGroups) {
            this.displayItems = displayItems;
            this.rawGroups = rawGroups;
        }
    }

    public static void init() {
        if (ConfigHelper.isSimpleConfigLoaded()) {
            Config.set(SimpleConfig.getInstance().get());
        } else if (ConfigHelper.isClothConfigLoaded()) {
            ClothConfig.load();
            Config.set(ClothConfig.getConfig());
        }
    }

    public static String getTabId(CreativeModeTab tab) {
        ResourceLocation key = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        return key != null ? key.toString() : tab.getDisplayName().getString();
    }

    public static List<Component> getTabIds() {
        List<Component> list = new ArrayList<>();

        for (CreativeModeTab creativeModeTab : CreativeModeTabs.allTabs()) {
            String tabId = InventoryItemGroups.getTabId(creativeModeTab);
            if (!tabId.equals("minecraft:hotbar") && !tabId.equals("minecraft:search") && !tabId.equals("minecraft:op_blocks") && !tabId.equals("minecraft:inventory")) {
                list.add(
                        Component.translatable(
                                        "text.inventory_item_groups.option.id_of_menu_tabs.element",
                                        Component.literal(creativeModeTab.getDisplayName().getString()),
                                        Component.literal(tabId).withStyle(style -> style
                                                .withHoverEvent(getHoverEvent(Component.translatable("chat.copy")))
                                                .withClickEvent(getClickEvent(tabId))
                                                .withColor(ChatFormatting.WHITE))
                                )
                                .withStyle(ChatFormatting.GRAY)
                );
            }
        }

        return list;
    }

    private static int indexOfStack(ItemStack stack) {
        Integer index = firstIndexes.get(stack);
        return index != null ? index : -1;
    }

    public static int calculateIndex(List<Slot> slots, int slotIndex) {
        if (slots.isEmpty()) return -1;

        int result;
        int secondItemIndex = slots.size() >= 2 ? indexOfStack(slots.get(1).getItem()) : -1;

        if (secondItemIndex >= 0) {
            result = secondItemIndex;
            if (!slots.get(0).getItem().equals(slots.get(1).getItem())) result--;
        } else {
            result = indexOfStack(slots.get(0).getItem());
        }

        if (result < 0) result = 0;
        return result + slotIndex;
    }

    public static boolean hasGroups() {
        return !groups.isEmpty();
    }

    public static ArrayList<Group> groupsOnSelectedTab(CreativeModeTab selectedTab) {
        ArrayList<Group> groupsOnSelectedTab = new ArrayList<>();
        for (Group group : groups) {
            if (selectedTab.equals(group.getTab()))
                groupsOnSelectedTab.add(group);
        }
        return groupsOnSelectedTab;
    }

    public static Group findGroupByIndex(int index) {
        // groups is cleared when the screen closes, without touching the table below
        if (groups.isEmpty()) return null;
        return index >= 0 && index < groupsByIndex.length ? groupsByIndex[index] : null;
    }

    /**
     * Builds the list shown for a tab: the tab's display items with every group collapsed
     * down to its icon. Also refreshes the index tables.
     */
    public static ArrayList<ItemStack> buildTabItems(Collection<ItemStack> displayItems) {
        ArrayList<ItemStack> newStack = new ArrayList<>(displayItems);

        Set<ItemStack> hidden = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Group group : groupsOnSelectedTab(selectedTab)) {
            hidden.addAll(group.getItems());
            hidden.remove(group.getIcon());
        }
        if (!hidden.isEmpty())
            newStack.removeIf(hidden::contains);

        tempItemStacks = newStack;
        setIndexes();
        return newStack;
    }

    public static void setIndexes() {
        firstIndexes.clear();
        lastIndexes.clear();
        for (int i = 0; i < tempItemStacks.size(); i++) {
            ItemStack itemStack = tempItemStacks.get(i);
            firstIndexes.putIfAbsent(itemStack, i);
            lastIndexes.put(itemStack, i);
        }

        for (Group group : groupsOnSelectedTab(selectedTab)) {
            boolean setIcon = false;
            boolean resetItems = false;
            for (IndexedItemStack entry : group.getItemsWithIndexes()) {
                Integer first = firstIndexes.get(entry.getItemStack());
                if (first == null) continue;

                int firstIndex = first;
                int lastIndex = lastIndexes.get(entry.getItemStack());
                if (group.isVisibility()) {
                    if (firstIndex != lastIndex && !setIcon) {
                        group.setIconIndex(firstIndex);
                        entry.setIndex(lastIndex);
                        setIcon = true;
                    } else
                        entry.setIndex(firstIndex);
                } else {
                    group.setIconIndex(firstIndex);
                    if (!resetItems) {
                        group.resetItemIndexes();
                        resetItems = true;
                    }
                }
            }
        }

        Group[] table = new Group[tempItemStacks.size()];
        for (Group group : groups) {
            putGroup(table, group.getIconIndex(), group);
            for (IndexedItemStack entry : group.getItemsWithIndexes())
                putGroup(table, entry.getIndex(), group);
        }
        groupsByIndex = table;
    }

    private static void putGroup(Group[] table, int index, Group group) {
        if (index >= 0 && index < table.length && table[index] == null)
            table[index] = group;
    }

    private static boolean containsAny(String name, List<String> parts) {
        for (String part : parts)
            if (name.contains(part)) return true;
        return false;
    }

    private static boolean missesAny(String name, List<String> parts) {
        for (String part : parts)
            if (!name.contains(part)) return true;
        return false;
    }

    private static void addItems(List<RawGroup> target, String groupName, List<String> containedItems, List<String> nonContainedItems, List<String> equivalentItems, List<ItemStack> stacks, String[] names) {
        RawGroup rawGroup = new RawGroup();
        rawGroup.name = groupName;
        if (nonContainedItems.isEmpty())
            nonContainedItems = List.of("1111111");

        for (int i = 0; i < names.length; i++) {
            String itemName = names[i];

            boolean contained = containsAny(itemName, containedItems)
                    && missesAny(itemName, nonContainedItems);

            if (contained || equivalentItems.contains(itemName))
                rawGroup.items.add(stacks.get(i));
        }

        target.add(rawGroup);
    }

    private static ArrayList<RawGroup> matchRawGroups(String selectedTabId, Collection<ItemStack> displayItems) {
        ArrayList<RawGroup> result = new ArrayList<>();

        List<ItemGroup> tabGroups = new ArrayList<>();
        for (ItemGroup group : Config.get().groups()) {
            if (selectedTabId.equals(group.tabId))
                tabGroups.add(group);
        }
        if (tabGroups.isEmpty()) return result;

        // Item names are what the group patterns match against, so build them once per tab
        // instead of once per group.
        List<ItemStack> stacks = new ArrayList<>(displayItems);
        String[] names = new String[stacks.size()];
        for (int i = 0; i < names.length; i++)
            names[i] = stacks.get(i).getItem().toString();

        for (ItemGroup group : tabGroups)
            addItems(result, group.groupName, group.containedItems, group.nonContainedItems, group.equivalentItems, stacks, names);

        return result;
    }

    public static void createGroups() {
        groups.clear();

        if (Config.get() != cachedConfig) {
            rawGroupsCache.clear();
            cachedConfig = Config.get();
        }

        Collection<ItemStack> displayItems = selectedTab.getDisplayItems();
        CachedRawGroups cached = rawGroupsCache.get(selectedTab);
        if (cached == null || cached.displayItems != displayItems) {
            cached = new CachedRawGroups(displayItems, matchRawGroups(getTabId(selectedTab), displayItems));
            rawGroupsCache.put(selectedTab, cached);
        }
        rawDefaultGroups = cached.rawGroups;

        rawDefaultGroups.forEach(rawGroup -> groups.add(new Group(getGroupTranslate(rawGroup), selectedTab, rawGroup.items)));
        groups.removeIf(group -> group.getItems().size() < 3);
    }

    public static List<ItemGroup> getDefaultGroups() {
        List<ItemGroup> defaults = new ArrayList<>();

        String tabId = "minecraft:building_blocks";
        addGroup(defaults, tabId, "logs", List.of("log", "stem", "bamboo_block"), List.of("stripped"));
        addGroup(defaults, tabId, "woods", List.of("wood", "hyphae"), List.of("stripped"));
        addGroup(defaults, tabId, "stripped_logs", List.of("log", "stem", "bamboo_block"));
        addGroup(defaults, tabId, "stripped_woods", List.of("wood", "hyphae"));
        addGroup(defaults, tabId, "stairs", List.of("stair"));
        addGroup(defaults, tabId, "slabs", List.of("slab"));
        addGroup(defaults, tabId, "planks", List.of("planks", "mosaic"));
        addGroup(defaults, tabId, "fence_gates", List.of("fence_gate"));
        addGroup(defaults, tabId, "fences", List.of("fence"));
        addGroup(defaults, tabId, "trapdoors", List.of("trapdoor"));
        addGroup(defaults, tabId, "doors", List.of("door"));
        addGroup(defaults, tabId, "pressure_plates", List.of("pressure_plate"));
        addGroup(defaults, tabId, "buttons", List.of("button"));
        addGroup(defaults, tabId, "bars", List.of("bar"), List.of("cinnabar"));
        addGroup(defaults, tabId, "chains", List.of("chain"));
        addGroup(defaults, tabId, "copper", List.of("copper"));
        addGroup(defaults, tabId, "walls", List.of("wall"));
        addGroup(defaults, tabId, "decorative_stone", List.of("bricks", "chiseled", "tiles", "polished"));
        addGroup(defaults, tabId, "sandstone", List.of("sandstone"));

        tabId = "minecraft:colored_blocks";
        addGroup(defaults, tabId, "wool_stairs", List.of("wool_stairs"));
        addGroup(defaults, tabId, "wool_slabs", List.of("wool_slab"));
        addGroup(defaults, tabId, "wool", List.of("wool"));
        addGroup(defaults, tabId, "carpets", List.of("carpet"));
        addGroup(defaults, tabId, "glazed_terracotta", List.of("glazed_terracotta"));
        addGroup(defaults, tabId, "terracotta", List.of("terracotta"));
        addGroup(defaults, tabId, "concrete_powder", List.of("concrete_powder"));
        addGroup(defaults, tabId, "concrete_stairs", List.of("concrete_stairs"));
        addGroup(defaults, tabId, "concrete_slabs", List.of("concrete_slab"));
        addGroup(defaults, tabId, "concrete", List.of("concrete"));
        addGroup(defaults, tabId, "glass_panes", List.of("glass_pane"));
        addGroup(defaults, tabId, "glass", List.of("glass"));
        addGroup(defaults, tabId, "shulker_boxes", List.of("shulker_box"));
        addGroup(defaults, tabId, "candles", List.of("candle"));
        addGroup(defaults, tabId, "banners", List.of("banner"));
        addGroup(defaults, tabId, "beds", List.of("bed"));
        addGroup(defaults, tabId, "cushions", List.of("_cushion"));

        tabId = "minecraft:natural_blocks";
        addGroup(defaults, tabId, "ores", List.of("_ore", "debris", "raw_"));
        addGroup(defaults, tabId, "mushrooms", List.of("mushroom", "fungus"));
        addGroup(defaults, tabId, "saplings", List.of("sapling", "propagule"));
        addGroup(defaults, tabId, "ground_cover", List.of("fern", "_grass", "bush", "_sprouts", "hanging_moss", "_vines", "shrub"), List.of("_bush"));
        addGroup(defaults, tabId, "seeds", List.of("seeds", "_pod"));
        addGroup(defaults, tabId, "flowers", List.of("dandelion", "poppy", "orchid", "allium", "tulip", "daisy", "cornflower", "torchflower", "azure_bluet", "valley", "cactus_flower", "eyeblossom", "rose", "petals", "wildflower", "crimson_roots", "warped_roots", "sunflower", "peony", "lilac", "pitcher_plant"));
        addGroup(defaults, tabId, "leaves", List.of("leaves"));
        addGroup(defaults, tabId, "coral_blocks", List.of("coral_block"));
        addGroup(defaults, tabId, "coral_decorations", List.of("coral"));
        addGroup(defaults, tabId, "stone", List.of(":stone", "diorite", "andesite", "granite", "tuff", "basalt", "blackstone", "deepslate"));
        addGroup(defaults, tabId, "logs", List.of("log", "stem"));

        tabId = "minecraft:functional_blocks";
        addGroup(defaults, tabId, "lanterns", List.of("lantern"), List.of("sea"));
        addGroup(defaults, tabId, "chains", List.of("chain"));
        addGroup(defaults, tabId, "bulbs", List.of("bulb"));
        addGroup(defaults, tabId, "anvils", List.of("anvil"));
        addGroup(defaults, tabId, "lightning_rods", List.of("lightning_rod"));
        addGroup(defaults, tabId, "shelves", List.of("_shelf"));
        addGroup(defaults, tabId, "hanging_signs", List.of("hanging_sign"));
        addGroup(defaults, tabId, "signs", List.of("sign"));
        addGroup(defaults, tabId, "chests", List.of("chest"));
        addGroup(defaults, tabId, "shulker_boxes", List.of("shulker_box"));
        addGroup(defaults, tabId, "beds", List.of("_bed"));
        addGroup(defaults, tabId, "candles", List.of("candle"));
        addGroup(defaults, tabId, "banners", List.of("banner"));
        addGroup(defaults, tabId, "skulls", List.of("head", "skull"));
        addGroup(defaults, tabId, "golem_statues", List.of("golem_statue"));
        addGroup(defaults, tabId, "infested_stone", List.of("infested"));
        addGroup(defaults, tabId, "paintings", List.of("painting"));

        tabId = "minecraft:redstone_blocks";
        addGroup(defaults, tabId, "bulbs", List.of("bulb"));
        addGroup(defaults, tabId, "pressure_plates", List.of("pressure_plate"));
        addGroup(defaults, tabId, "transport", List.of("minecart", "boat", "_raft"));
        addGroup(defaults, tabId, "chests", List.of("chest"));
        addGroup(defaults, tabId, "rails", List.of("rail"));

        tabId = "minecraft:tools_and_utilities";
        addGroup(defaults, tabId, "shovels", List.of("shovel"));
        addGroup(defaults, tabId, "pickaxes", List.of("pickaxe"));
        addGroup(defaults, tabId, "axes", List.of("axe"));
        addGroup(defaults, tabId, "hoes", List.of("hoe"));
        addGroup(defaults, tabId, "bundles", List.of("bundle"));
        addGroup(defaults, tabId, "firework_rockets", List.of("firework_rocket"));
        addGroup(defaults, tabId, "harnesses", List.of("harness"));
        addGroup(defaults, tabId, "chest_boats", List.of("chest_boat", "chest_raft"));
        addGroup(defaults, tabId, "boats", List.of("boat", "_raft"));
        addGroup(defaults, tabId, "rails", List.of("rail"));
        addGroup(defaults, tabId, "minecarts", List.of("minecart"));
        addGroup(defaults, tabId, "discs", List.of("disc"));
        addGroup(defaults, tabId, "goat_horns", List.of("goat_horn"));
        addGroup(defaults, tabId, "creature_buckets", List.of("cod_bucket", "salmon_bucket", "tropical_fish_bucket", "pufferfish_bucket", "axolotl_bucket", "tadpole_bucket", "sulfur_cube_bucket"));

        tabId = "minecraft:combat";
        addGroup(defaults, tabId, "swords", List.of("sword"));
        addGroup(defaults, tabId, "spears", List.of("spear"));
        addGroup(defaults, tabId, "axes", List.of("axe"));
        addGroup(defaults, tabId, "helmets", List.of("helmet"));
        addGroup(defaults, tabId, "chestplates", List.of("chestplate"));
        addGroup(defaults, tabId, "leggings", List.of("leggings"));
        addGroup(defaults, tabId, "boots", List.of("boots"));
        addGroup(defaults, tabId, "horse_armor", List.of("horse_armor"));
        addGroup(defaults, tabId, "nautilus_armor", List.of("nautilus_armor"));
        addGroup(defaults, tabId, "eggs", List.of("egg"));
        addGroup(defaults, tabId, "tipped_arrows", List.of("tipped_arrow"));
        addGroup(defaults, tabId, "firework_rockets", List.of("firework_rocket"));

        tabId = "minecraft:food_and_drinks";
        addGroup(defaults, tabId, "suspicious_stews", List.of("suspicious_stew"));
        addGroup(defaults, tabId, "ominous_bottles", List.of("ominous_bottle"));
        addGroup(defaults, tabId, "splash_potions", List.of("splash_potion"));
        addGroup(defaults, tabId, "lingering_potions", List.of("lingering_potion"));
        addGroup(defaults, tabId, "potions", List.of("potion"));
        addGroup(defaults, tabId, "cooked_food", List.of("cooked"));
        addGroup(defaults, tabId, "raw_food", List.of("beef", "porkchop", "mutton", "chicken", "rabbit", ":cod", "salmon"), List.of("rabbit_"));

        tabId = "minecraft:ingredients";
        addGroup(defaults, tabId, "dyes", List.of("dye"));
        addGroup(defaults, tabId, "banner_patterns", List.of("banner_pattern"));
        addGroup(defaults, tabId, "pottery_sherds", List.of("pottery_sherd"));
        addGroup(defaults, tabId, "smithing_templates", List.of("smithing_template"));
        addGroup(defaults, tabId, "enchanted_books", List.of("enchanted_book"));

        return defaults;
    }

    private static void addGroup(List<ItemGroup> list, String tabId, String groupName, List<String> containedItems) {
        addGroup(list, tabId, groupName, containedItems, List.of());
    }

    private static void addGroup(List<ItemGroup> list, String tabId, String groupName, List<String> containedItems, List<String> nonContainedItems) {
        ItemGroup group = new ItemGroup();
        group.tabId = tabId;
        group.groupName = groupName;
        group.containedItems = new ArrayList<>(containedItems);
        group.nonContainedItems = new ArrayList<>(nonContainedItems);
        list.add(group);
    }

    public static Component getGroupTranslate(RawGroup rawGroup) {
        String key = "group_name.inventory_item_groups." + rawGroup.name;
        return Language.getInstance().has(key)
                ? Component.translatable(key)
                : Component.literal(rawGroup.name);
    }

    private static HoverEvent getHoverEvent(Component component) {
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, component);
    }

    private static ClickEvent getClickEvent(String tabId) {
        return new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tabId);
    }
}
