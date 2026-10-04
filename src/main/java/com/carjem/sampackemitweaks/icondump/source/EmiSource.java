package com.carjem.sampackemitweaks.icondump.source;

import com.google.gson.JsonElement;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiIngredientSerializers;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.runtime.EmiReloadManager;
import com.carjem.sampackemitweaks.icondump.IconDump;
import com.carjem.sampackemitweaks.icondump.source.StackSources.Entry;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * EMI's own stack list, keyed exactly as emi_dump.json's `added` list keys it: the
 * EmiIngredientSerializers form, a string such as item:ns:path{...} for items and a json object
 * for the odd stack that needs one (a fluid with an amount). Loaded only when EMI is.
 */
final class EmiSource {

    private EmiSource() {
    }

    static boolean ready() {
        return EmiReloadManager.isLoaded();
    }

    static List<Entry> collect(Predicate<String> wantNamespace) {
        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int unserializable = 0;
        for (EmiStack stack : EmiStackList.stacks) {
            if (stack.isEmpty() || !wantNamespace.test(stack.getId().getNamespace())) {
                continue;
            }
            Entry entry = entry(stack, false);
            if (entry == null) {
                unserializable++;
            } else if (seen.add(entry.id())) {
                entries.add(entry);
            }
        }
        if (unserializable > 0) {
            IconDump.LOG.info("{} EMI stack(s) have no serializer, so no id to key them by; skipped", unserializable);
        }
        return entries;
    }

    static Entry unlisted(ItemStack stack) {
        return entry(EmiStack.of(stack), true);
    }

    private static Entry entry(EmiStack stack, boolean unlisted) {
        JsonElement json;
        try {
            json = EmiIngredientSerializers.serialize(stack);
        } catch (RuntimeException e) {
            return null;
        }
        if (json == null || json.isJsonNull()) {
            return null;
        }
        String id = json.isJsonPrimitive() ? json.getAsString() : json.toString();
        return new Entry(id, stack.getId().getNamespace(), StackSources.nameOf(stack::getName), unlisted,
                graphics -> stack.render(graphics, 0, 0, 0, EmiIngredient.RENDER_ICON));
    }
}
