package com.carjem.sampackemitweaks.itemgroups.config;

import com.carjem.sampackemitweaks.client.ClientConfig;
import com.carjem.sampackemitweaks.itemgroups.InventoryItemGroups;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class ClothConfig implements Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", InventoryItemGroups.NAMESPACE + ".json");

    public static ClothConfig config;
    public Sort sort = Config.super.sort();
    public List<ItemGroup> groups = Config.super.groups();

    public static Screen getConfigScreen(Screen parent) {
        load();
        Config.set(config);

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(getTranslate("group.general"))
                .setSavingRunnable(() -> {
                    config.save();
                    ClientConfig.SPEC.save();
                });
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory groups = builder.getOrCreateCategory(getTranslate("option.groups"));
        groups.addEntry(entryBuilder.startEnumSelector(getTranslate("option.sort"), Sort.class, config.sort)
                .setDefaultValue(Sort.DEFAULT)
                .setEnumNameProvider(e -> getTranslate(((Sort) e).getKey()))
                .setSaveConsumer(value -> config.sort = value)
                .build()
        );
        groups.addEntry(new NestedListListEntry<ItemGroup, MultiElementListEntry<ItemGroup>>(
                getTranslate("option.groups"),
                config.groups,
                false,
                Optional::empty,
                newList -> config.groups = newList,
                List::of,
                entryBuilder.getResetButtonKey(),
                true,
                true,
                (elem, nestedListListEntry) -> {
                    ItemGroup currentElem = elem != null ? elem : new ItemGroup();
                    if (currentElem.groupName == null) currentElem.groupName = "name";
                    return new MultiElementListEntry<>(
                            getTranslate("option.groups.element"),
                            currentElem,
                            List.of(
                                    entryBuilder.startStrField(getTranslate("option.groups.group_name"), currentElem.groupName)
                                            .setDefaultValue("")
                                            .setSaveConsumer(value -> currentElem.groupName = value)
                                            .build(),
                                    entryBuilder.startStrField(getTranslate("option.groups.tab_id"), currentElem.tabId)
                                            .setDefaultValue("")
                                            .setSaveConsumer(value -> currentElem.tabId = value)
                                            .build(),
                                    entryBuilder.startStrList(getTranslate("option.groups.equivalent_items"), currentElem.equivalentItems.stream().map(Object::toString).toList())
                                            .setDefaultValue(List.of())
                                            .setTooltip(getTranslate("option.groups.equivalent_items.tooltip"))
                                            .setSaveConsumer(objects -> {
                                                currentElem.equivalentItems.clear();
                                                currentElem.equivalentItems.addAll(objects);
                                            })
                                            .build(),
                                    entryBuilder.startStrList(getTranslate("option.groups.contained_items"), currentElem.containedItems.stream().map(Object::toString).toList())
                                            .setDefaultValue(List.of())
                                            .setTooltip(getTranslate("option.groups.contained_items.tooltip"))
                                            .setSaveConsumer(objects -> {
                                                currentElem.containedItems.clear();
                                                currentElem.containedItems.addAll(objects);
                                            })
                                            .build(),
                                    entryBuilder.startStrList(getTranslate("option.groups.non_contained_items"), currentElem.nonContainedItems.stream().map(Object::toString).toList())
                                            .setDefaultValue(List.of())
                                            .setTooltip(getTranslate("option.groups.non_contained_items.tooltip"))
                                            .setSaveConsumer(objects -> {
                                                currentElem.nonContainedItems.clear();
                                                currentElem.nonContainedItems.addAll(objects);
                                            })
                                            .build()
                            ),
                            true
                    );
                }
        ));
        SubCategoryBuilder ids = entryBuilder.startSubCategory(getTranslate("option.id_of_menu_tabs")).setExpanded(false);
        InventoryItemGroups.getTabIds().forEach(id -> ids.add(entryBuilder.startTextDescription(id).build()));
        groups.addEntry(ids.build());

        addIconExportCategory(builder, entryBuilder);

        return builder.build();
    }

    /** The icon export settings, also in the NeoForge client config. */
    private static void addIconExportCategory(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        ConfigCategory export = builder.getOrCreateCategory(Component.translatable("sampack_emitweaks.configuration.icon_export"));
        export.addEntry(entryBuilder.startIntField(Component.translatable("sampack_emitweaks.configuration.default_size"),
                        ClientConfig.ICON_SIZE.get())
                .setDefaultValue(32).setMin(1).setMax(512)
                .setTooltip(Component.translatable("sampack_emitweaks.configuration.default_size.tooltip"))
                .setSaveConsumer(ClientConfig.ICON_SIZE::set)
                .build());
        export.addEntry(entryBuilder.startIntField(Component.translatable("sampack_emitweaks.configuration.max_sheet_size"),
                        ClientConfig.MAX_SHEET_SIZE.get())
                .setDefaultValue(2048).setMin(16).setMax(16384)
                .setTooltip(Component.translatable("sampack_emitweaks.configuration.max_sheet_size.tooltip"))
                .setSaveConsumer(ClientConfig.MAX_SHEET_SIZE::set)
                .build());
        export.addEntry(entryBuilder.startIntField(Component.translatable("sampack_emitweaks.configuration.icons_per_frame"),
                        ClientConfig.ICONS_PER_FRAME.get())
                .setDefaultValue(64).setMin(1).setMax(4096)
                .setTooltip(Component.translatable("sampack_emitweaks.configuration.icons_per_frame.tooltip"))
                .setSaveConsumer(ClientConfig.ICONS_PER_FRAME::set)
                .build());
        export.addEntry(entryBuilder.startBooleanToggle(Component.translatable("sampack_emitweaks.configuration.include_names"),
                        ClientConfig.INCLUDE_NAMES.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("sampack_emitweaks.configuration.include_names.tooltip"))
                .setSaveConsumer(ClientConfig.INCLUDE_NAMES::set)
                .build());
    }

    public static ClothConfig getConfig() {
        return config;
    }

    public static void load() {
        if (CONFIG_PATH.toFile().exists()) {
            try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
                config = GSON.fromJson(reader, ClothConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            config = new ClothConfig();
        }
    }

    public void save() {
        Config.set(config);
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Component getTranslate(String text) {
        return Component.translatable("text.inventory_item_groups." + text);
    }

    @Override
    public Sort sort() {
        return this.sort;
    }

    @Override
    public List<ItemGroup> groups() {
        return this.groups;
    }
}
