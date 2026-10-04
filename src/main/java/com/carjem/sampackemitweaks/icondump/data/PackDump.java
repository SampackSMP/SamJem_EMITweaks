package com.carjem.sampackemitweaks.icondump.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.carjem.sampackemitweaks.icondump.IconDump;
import com.carjem.sampackemitweaks.pristine.PristineTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * <pre>
 * /emitweaks export gamedata
 * </pre>
 *
 * A snapshot of everything the pack registers, for auditing it from outside the game: content
 * overlap between mods, recipe coverage, food balance, how the worldgen mods stack. What the
 * KubeJS /packdump script wrote, into &lt;minecraft&gt;/icondump/pack/ instead of kubejs/dump/pack/.
 *
 * The icon export only covers what it can render and emi_dump.json only what EMI shows; this
 * reads the registries themselves. `registries` and `tags` are generic, every id and every tag of
 * every registry, built-in and datapack alike; the other sections add the detail an id alone
 * does not carry.
 *
 * Singleplayer only: the datapack registries, recipes, biomes and levels are the integrated
 * server's. Each section is built on the server thread and written as soon as it is done, so one
 * that fails outright still leaves the rest on disk; manifest.json, written last, holds every
 * section's count and every entry that failed.
 */
public final class PackDump {
    static final String DIRECTORY = "pack";
    static final int FORMAT = 1;

    /** A tag ingredient can resolve to hundreds of items; past this many only the count is kept. */
    private static final int MAX_INGREDIENT_ITEMS = 24;

    private record Section(String name, Function<PackDump, JsonElement> build) {
    }

    private static final List<Section> SECTIONS = List.of(
            new Section("mods", PackDump::mods),
            new Section("registries", PackDump::registries),
            new Section("tags", PackDump::tags),
            new Section("items", PackDump::items),
            new Section("blocks", PackDump::blocks),
            new Section("entities", PackDump::entities),
            new Section("effects", PackDump::effects),
            new Section("enchantments", PackDump::enchantments),
            new Section("creative_tabs", PackDump::creativeTabs),
            new Section("recipes", PackDump::recipes),
            new Section("biomes", PackDump::biomes),
            new Section("dimensions", PackDump::dimensions),
            new Section("structures", PackDump::structures),
            new Section("loot_tables", PackDump::lootTables));

    private final MinecraftServer server;
    private final RegistryAccess registries;
    private final List<String> errors = new ArrayList<>();

    private PackDump(MinecraftServer server) {
        this.server = server;
        this.registries = server.registryAccess();
    }

    public static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            source.sendFailure(Component.literal("/emitweaks export gamedata reads the integrated server; run it in singleplayer"));
            return 0;
        }
        // the tabs' contents exist only once the client has built them; the KubeJS script made
        // you open the creative inventory first instead
        CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(),
                mc.options.operatorItemsTab().get(), mc.level.registryAccess());

        PackDump dump = new PackDump(server);
        JsonObject counts = new JsonObject();
        for (Section section : SECTIONS) {
            try {
                JsonElement data = server.submit(() -> section.build().apply(dump)).get(5, TimeUnit.MINUTES);
                JsonObject json = new JsonObject();
                json.addProperty("format", FORMAT);
                json.add("data", data);
                DataFiles.write(DIRECTORY + "/" + section.name() + ".json", json);
                int size = data.isJsonArray() ? data.getAsJsonArray().size() : data.getAsJsonObject().size();
                counts.addProperty(section.name(), size);
                source.sendSuccess(() -> Component.literal(String.format("pack/%s.json: %,d", section.name(), size)), false);
            } catch (Exception e) {
                IconDump.LOG.error("Pack dump section {} failed", section.name(), e);
                dump.errors.add(section.name() + ": section failed (" + e + ")");
                source.sendFailure(Component.literal("pack/" + section.name() + ".json failed; see manifest.json"));
            }
        }

        JsonObject manifest = new JsonObject();
        manifest.addProperty("format", FORMAT);
        manifest.add("counts", counts);
        JsonArray errors = new JsonArray();
        dump.errors.forEach(errors::add);
        manifest.add("errors", errors);
        try {
            DataFiles.write(DIRECTORY + "/manifest.json", manifest);
        } catch (IOException e) {
            IconDump.LOG.error("Could not write the pack dump manifest", e);
            source.sendFailure(Component.literal("manifest.json failed: " + e.getMessage()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(String.format("Pack dump done, %d error(s), in %s/%s/",
                dump.errors.size(), DataFiles.DIRECTORY, DIRECTORY)), false);
        return counts.size();
    }

    // --- helpers -----------------------------------------------------------

    /** One entry's failure goes in the manifest instead of costing the whole section. */
    private interface EntryBuilder {
        void build();
    }

    private void entry(String section, Object id, EntryBuilder builder) {
        try {
            builder.build();
        } catch (RuntimeException e) {
            errors.add(section + ": " + id + " (" + e + ")");
        }
    }

    private static String keyOf(Holder<?> holder) {
        return holder.unwrapKey().map(key -> key.location().toString()).orElse("<inline>");
    }

    private static JsonArray holderIds(HolderSet<?> holders) {
        JsonArray ids = new JsonArray();
        holders.stream().forEach(holder -> ids.add(keyOf(holder)));
        return ids;
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static JsonArray sorted(Iterable<?> ids) {
        Set<String> sorted = new TreeSet<>();
        ids.forEach(id -> sorted.add(String.valueOf(id)));
        JsonArray json = new JsonArray();
        sorted.forEach(json::add);
        return json;
    }

    // --- sections ----------------------------------------------------------

    private JsonElement mods() {
        JsonObject mods = new JsonObject();
        for (IModInfo info : ModList.get().getMods()) {
            entry("mods", info.getModId(), () -> {
                JsonObject mod = new JsonObject();
                mod.addProperty("name", info.getDisplayName());
                mod.addProperty("version", info.getVersion().toString());
                mods.add(info.getModId(), mod);
            });
        }
        return mods;
    }

    /** Every id of every registry in the server's registry access, built-in and datapack alike. */
    private JsonElement registries() {
        JsonObject out = new JsonObject();
        registries.registries().forEach(entry -> {
            String name = entry.key().location().toString();
            entry("registries", name, () -> out.add(name, sorted(entry.value().keySet())));
        });
        return out;
    }

    private JsonElement tags() {
        JsonObject out = new JsonObject();
        registries.registries().forEach(entry -> {
            String name = entry.key().location().toString();
            JsonObject byTag = new JsonObject();
            tagsOf(name, entry.value(), byTag);
            if (byTag.size() > 0) {
                out.add(name, byTag);
            }
        });
        return out;
    }

    private <T> void tagsOf(String registryName, Registry<T> registry, JsonObject byTag) {
        registry.getTagNames().forEach((TagKey<T> tag) -> {
            String id = tag.location().toString();
            entry("tags", registryName + " #" + id, () -> byTag.add(id,
                    registry.getTag(tag).map(PackDump::holderIds).orElseGet(JsonArray::new)));
        });
    }

    private JsonElement items() {
        JsonObject items = new JsonObject();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            entry("items", id, () -> {
                ItemStack stack = item.getDefaultInstance();
                JsonObject entry = new JsonObject();
                entry.addProperty("name", stack.getHoverName().getString());
                entry.addProperty("stack", stack.getMaxStackSize());
                if (stack.getMaxDamage() > 0) {
                    entry.addProperty("durability", stack.getMaxDamage());
                }
                FoodProperties food = stack.get(DataComponents.FOOD);
                if (food != null) {
                    JsonObject foodJson = new JsonObject();
                    foodJson.addProperty("nutrition", food.nutrition());
                    foodJson.addProperty("saturation", food.saturation());
                    entry.add("food", foodJson);
                }
                items.add(id.toString(), entry);
            });
        }
        return items;
    }

    private JsonElement blocks() {
        JsonObject blocks = new JsonObject();
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            entry("blocks", id, () -> {
                JsonObject entry = new JsonObject();
                entry.addProperty("name", block.getName().getString());
                entry.addProperty("hardness", block.defaultDestroyTime());
                entry.addProperty("resistance", block.getExplosionResistance());
                ResourceLocation item = BuiltInRegistries.ITEM.getKey(block.asItem());
                if (!item.equals(BuiltInRegistries.ITEM.getDefaultKey())) {
                    entry.addProperty("item", item.toString());
                }
                blocks.add(id.toString(), entry);
            });
        }
        return blocks;
    }

    private JsonElement entities() {
        JsonObject entities = new JsonObject();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            entry("entities", id, () -> {
                JsonObject entry = new JsonObject();
                entry.addProperty("name", type.getDescription().getString());
                entry.addProperty("category", type.getCategory().getName());
                entities.add(id.toString(), entry);
            });
        }
        return entities;
    }

    private JsonElement effects() {
        JsonObject effects = new JsonObject();
        for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            entry("effects", id, () -> {
                JsonObject entry = new JsonObject();
                entry.addProperty("name", effect.getDisplayName().getString());
                entry.addProperty("category", effect.getCategory().name());
                effects.add(String.valueOf(id), entry);
            });
        }
        return effects;
    }

    private JsonElement enchantments() {
        JsonObject enchantments = new JsonObject();
        Registry<Enchantment> registry = registries.registryOrThrow(Registries.ENCHANTMENT);
        for (Map.Entry<ResourceKey<Enchantment>, Enchantment> e : registry.entrySet()) {
            String id = e.getKey().location().toString();
            entry("enchantments", id, () -> {
                Enchantment enchantment = e.getValue();
                JsonObject entry = new JsonObject();
                entry.addProperty("name", enchantment.description().getString());
                entry.addProperty("max_level", enchantment.getMaxLevel());
                entry.add("exclusive_with", holderIds(enchantment.exclusiveSet()));
                enchantments.add(id, entry);
            });
        }
        return enchantments;
    }

    /**
     * Every registered tab but the pack's custom ones, in NeoForge's creative-screen order:
     * `items` as its mods built it (see PristineTabs), and `displayed` as the screen shows it,
     * only where something changed it after the build.
     */
    private JsonElement creativeTabs() {
        JsonObject tabs = new JsonObject();
        for (CreativeModeTab tab : PristineTabs.order()) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            entry("creative_tabs", id, () -> {
                JsonArray items = itemIds(PristineTabs.displayItems(tab));
                JsonArray displayed = itemIds(tab.getDisplayItems());
                JsonObject entry = new JsonObject();
                entry.addProperty("name", tab.getDisplayName().getString());
                entry.add("items", items);
                if (!displayed.equals(items)) {
                    entry.add("displayed", displayed);
                }
                tabs.add(String.valueOf(id), entry);
            });
        }
        return tabs;
    }

    private static JsonArray itemIds(Iterable<ItemStack> stacks) {
        JsonArray ids = new JsonArray();
        stacks.forEach(stack -> ids.add(itemId(stack)));
        return ids;
    }

    private JsonElement recipes() {
        JsonObject recipes = new JsonObject();
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            String id = holder.id().toString();
            entry("recipes", id, () -> {
                Recipe<?> recipe = holder.value();
                JsonObject entry = new JsonObject();
                entry.addProperty("type", String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType())));
                // special recipes (map cloning, firework stars) have no fixed result, and some
                // mods throw here rather than return empty
                try {
                    ItemStack result = recipe.getResultItem(registries);
                    if (!result.isEmpty()) {
                        entry.addProperty("result", itemId(result));
                        entry.addProperty("count", result.getCount());
                    }
                } catch (RuntimeException e) {
                    entry.addProperty("result_error", e.toString());
                }
                JsonArray ingredients = new JsonArray();
                for (Ingredient ingredient : recipe.getIngredients()) {
                    ingredients.add(ingredient(ingredient));
                }
                entry.add("ingredients", ingredients);
                recipes.add(id, entry);
            });
        }
        return recipes;
    }

    /**
     * A plain tag ingredient is kept by name, since that is what says whether two mods' versions
     * of an item are interchangeable; otherwise its distinct items, or just their count past
     * MAX_INGREDIENT_ITEMS.
     */
    private static JsonElement ingredient(Ingredient ingredient) {
        ItemStack[] stacks = ingredient.getItems();
        if (!ingredient.isCustom() && ingredient.getValues().length == 1
                && ingredient.getValues()[0] instanceof Ingredient.TagValue tag) {
            JsonObject json = new JsonObject();
            json.addProperty("tag", tag.tag().location().toString());
            json.addProperty("options", stacks.length);
            return json;
        }
        if (stacks.length > MAX_INGREDIENT_ITEMS) {
            JsonObject json = new JsonObject();
            json.addProperty("options", stacks.length);
            return json;
        }
        Set<String> options = new LinkedHashSet<>();
        for (ItemStack stack : stacks) {
            options.add(itemId(stack));
        }
        JsonArray json = new JsonArray();
        options.forEach(json::add);
        return json;
    }

    /**
     * Biomes as they stand after every biome modifier has run, so a feature or spawn one mod
     * injected into another mod's biome shows up where it landed.
     */
    private JsonElement biomes() {
        JsonObject biomes = new JsonObject();
        Registry<Biome> registry = registries.registryOrThrow(Registries.BIOME);
        for (Map.Entry<ResourceKey<Biome>, Biome> e : registry.entrySet()) {
            String id = e.getKey().location().toString();
            entry("biomes", id, () -> {
                Biome biome = e.getValue();
                JsonArray features = new JsonArray();
                for (HolderSet<PlacedFeature> step : biome.getGenerationSettings().features()) {
                    features.add(holderIds(step));
                }
                JsonObject spawns = new JsonObject();
                for (MobCategory category : MobCategory.values()) {
                    List<MobSpawnSettings.SpawnerData> spawners = biome.getMobSettings().getMobs(category).unwrap();
                    if (spawners.isEmpty()) {
                        continue;
                    }
                    JsonArray types = new JsonArray();
                    spawners.forEach(spawner -> types.add(String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(spawner.type))));
                    spawns.add(category.getName(), types);
                }
                JsonObject entry = new JsonObject();
                entry.add("features", features);
                entry.add("spawns", spawns);
                biomes.add(id, entry);
            });
        }
        return biomes;
    }

    /**
     * Which biomes each dimension can actually place: the set that decides how several End or
     * Nether overhauls stack up, as opposed to the biomes that merely exist in the registry.
     */
    private JsonElement dimensions() {
        JsonObject dimensions = new JsonObject();
        for (ServerLevel level : server.getAllLevels()) {
            String id = level.dimension().location().toString();
            entry("dimensions", id, () -> {
                ChunkGenerator generator = level.getChunkSource().getGenerator();
                Set<String> biomes = new TreeSet<>();
                generator.getBiomeSource().possibleBiomes().forEach(holder -> biomes.add(keyOf(holder)));
                JsonObject entry = new JsonObject();
                entry.addProperty("generator", generator.getClass().getName());
                entry.addProperty("biome_source", generator.getBiomeSource().getClass().getName());
                entry.add("biomes", sorted(biomes));
                dimensions.add(id, entry);
            });
        }
        return dimensions;
    }

    private JsonElement structures() {
        JsonObject structures = new JsonObject();
        Registry<Structure> registry = registries.registryOrThrow(Registries.STRUCTURE);
        for (Map.Entry<ResourceKey<Structure>, Structure> e : registry.entrySet()) {
            String id = e.getKey().location().toString();
            entry("structures", id, () -> {
                JsonObject entry = new JsonObject();
                entry.addProperty("step", e.getValue().step().getName());
                entry.add("biomes", holderIds(e.getValue().biomes()));
                structures.add(id, entry);
            });
        }
        return structures;
    }

    /** Loot tables live in the reloadable registries, not the registry access the rest read. */
    private JsonElement lootTables() {
        return sorted(server.reloadableRegistries().getKeys(Registries.LOOT_TABLE));
    }
}
