package com.carjem.sampackemitweaks.icondump.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiIngredientSerializers;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.runtime.EmiReloadManager;
import com.carjem.sampackemitweaks.icondump.IconDump;
import com.carjem.sampackemitweaks.pristine.PristineEmiIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * emi_dump.json, format 3, what the KubeJS /emidump script used to write:
 *
 * <pre>
 * added       every stack EMI shows, in the order it shows them, as EmiIngredientSerializers
 *             writes it: a string (item:ns:path{..}) or, for the odd stack, a json object
 * index       the same, for EMI's list before any resource pack's index data reorders or hides
 *             it (format 3)
 * components  stack id -> its component patch as a json string, for every stack in either list
 *             that carries one
 * registry    item:ns:path of every registered item, air included, in registry order
 * tags        item tag id -> the item:ns:path of its members
 * </pre>
 *
 * `added` is what EMI shows, so it carries whatever ordering and hiding the pack has already
 * applied; `index` is the order the game itself gives EMI, which InvIndexLedger's own output
 * cannot reach (PristineTabs feeds EMI the creative tabs as their mods built them, and
 * PristineEmiIndex records the list just before EMI applies index data); `registry` is what exists, which nothing at the
 * creative-tab layer can touch. Loaded only when EMI is.
 */
final class EmiDump {
    static final String FILE = "emi_dump.json";
    static final int FORMAT = 3;

    private EmiDump() {
    }

    /** Writes the file and returns a one-line summary of it. */
    static String write() throws IOException, DataCommand.Skipped {
        if (!EmiReloadManager.isLoaded()) {
            throw new DataCommand.Skipped("EMI is still loading its stacks; try again in a moment");
        }
        // The encoding Recreative's ComponentUtil.encode() uses, and so exactly what its parse()
        // reads back: DataComponentPatch.CODEC through registry-aware JsonOps. EMI's own {..}
        // suffix is SNBT, and much of it is not valid json.
        RegistryOps<JsonElement> ops = Minecraft.getInstance().level.registryAccess()
                .createSerializationContext(JsonOps.INSTANCE);

        JsonObject components = new JsonObject();
        JsonArray added = serialize(EmiStackList.stacks, components, ops);
        List<EmiStack> pristine = PristineEmiIndex.stacks();
        JsonArray index = pristine != null ? serialize(pristine, components, ops) : null;

        JsonArray registry = new JsonArray();
        Map<String, JsonArray> tags = new LinkedHashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            String itemId = "item:" + BuiltInRegistries.ITEM.getKey(item);
            registry.add(itemId);
            BuiltInRegistries.ITEM.wrapAsHolder(item).tags().forEach(tag ->
                    tags.computeIfAbsent(tag.location().toString(), t -> new JsonArray()).add(itemId));
        }
        JsonObject tagsJson = new JsonObject();
        tags.forEach(tagsJson::add);

        JsonObject json = new JsonObject();
        json.addProperty("format", FORMAT);
        json.add("added", added);
        if (index != null) {
            json.add("index", index);
        }
        json.add("components", components);
        json.add("registry", registry);
        json.add("tags", tagsJson);
        DataFiles.write(FILE, json);

        return String.format("%,d stacks (%s), %,d with components, %,d items, %,d tags",
                added.size(), index != null ? String.format("%,d before index data", index.size()) : "no pre-index list; EMI has not baked since startup",
                components.size(), registry.size(), tagsJson.size());
    }

    /** Serializes each stack in order, adding its component patch, if any, to `components`. */
    private static JsonArray serialize(List<EmiStack> stacks, JsonObject components, RegistryOps<JsonElement> ops) {
        JsonArray out = new JsonArray();
        int unserializable = 0;
        for (EmiStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;   // minecraft:air; EMI lists it, nothing serializes it
            }
            JsonElement serialized;
            try {
                serialized = EmiIngredientSerializers.serialize(stack);
            } catch (RuntimeException e) {
                serialized = null;
            }
            if (serialized == null || serialized.isJsonNull()) {
                unserializable++;
                continue;
            }
            out.add(serialized);

            // not only item stacks: some others (a mob effect shown as its potion) hand back an
            // item stack too, and are keyed by their own id all the same
            ItemStack itemStack = stack.getItemStack();
            if (itemStack.isEmpty()) {
                continue;
            }
            DataComponentPatch patch = itemStack.getComponentsPatch();
            if (patch.isEmpty()) {
                continue;
            }
            String id = serialized.isJsonPrimitive() ? serialized.getAsString() : serialized.toString();
            if (components.has(id)) {
                continue;   // the other list already encoded it
            }
            DataComponentPatch.CODEC.encodeStart(ops, patch)
                    .ifSuccess(json -> components.addProperty(id, json.toString()))
                    .ifError(error -> IconDump.LOG.warn("Could not encode the components of {}: {}", id, error.message()));
        }
        if (unserializable > 0) {
            IconDump.LOG.info("{} EMI stack(s) have no serializer; left out of {}", unserializable, FILE);
        }
        return out;
    }
}
