package com.carjem.sampackemitweaks.icondump.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.carjem.sampackemitweaks.icondump.IconDump;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * chipped_recipes.json, format 2: {"recipes": {recipe id: its json}} for every chipped:workbench
 * recipe. Each Chipped workstation is one such recipe, and its ingredients are exactly the tags
 * that bench crafts, so this is the list of chipped sets in the game. everycomp, stonezone and
 * gemsrealm generate theirs at runtime, which leaves the recipe manager the only place the full
 * list exists.
 *
 * Read from the integrated server, so singleplayer only: the copy synced to a client has every
 * tag ingredient flattened into its items, and the tag names are the point. Re-encoding a
 * recipe through Recipe.CODEC gives back {"type": "chipped:workbench", "ingredients":
 * [{"tag": ..}]}, the same shape the datapack declared.
 */
final class ChippedRecipes {
    static final String FILE = "chipped_recipes.json";
    static final int FORMAT = 2;
    private static final ResourceLocation WORKBENCH = ResourceLocation.fromNamespaceAndPath("chipped", "workbench");

    private ChippedRecipes() {
    }

    static String write() throws IOException, DataCommand.Skipped {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            throw new DataCommand.Skipped("the recipe tags are only readable in singleplayer");
        }
        Map<String, JsonElement> recipes;
        try {
            // on the server thread, which owns the recipe manager and swaps it out on /reload
            recipes = server.submit(() -> capture(server)).get(30, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IOException("could not read the server's recipes: " + e, e);
        }

        JsonObject recipesJson = new JsonObject();
        recipes.forEach(recipesJson::add);
        JsonObject json = new JsonObject();
        json.addProperty("format", FORMAT);
        json.add("recipes", recipesJson);
        DataFiles.write(FILE, json);
        return String.format("%,d chipped:workbench recipes", recipes.size());
    }

    /** Sorted by recipe id, so two captures of the same pack diff clean. */
    private static Map<String, JsonElement> capture(MinecraftServer server) {
        RegistryOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        Map<String, JsonElement> recipes = new TreeMap<>();
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (!WORKBENCH.equals(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()))) {
                continue;
            }
            String id = holder.id().toString();
            Recipe.CODEC.encodeStart(ops, recipe)
                    .ifSuccess(encoded -> recipes.put(id, encoded))
                    .ifError(error -> IconDump.LOG.warn("Could not encode chipped recipe {}: {}", id, error.message()));
        }
        return recipes;
    }
}
