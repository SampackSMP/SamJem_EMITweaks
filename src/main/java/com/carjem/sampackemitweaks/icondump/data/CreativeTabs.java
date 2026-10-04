package com.carjem.sampackemitweaks.icondump.data;

import com.carjem.sampackemitweaks.pristine.PristineTabs;
import com.google.gson.JsonArray;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.List;

/**
 * creative_tabs.json: a flat array of every registered creative tab id, the ids `/recreative dump
 * tabs` used to write to config/recreative_exports/tabs.json.
 *
 * They come in NeoForge's creative-screen order as it was before the pack's tab rules reordered
 * it, then the tabs that order leaves out (see {@link PristineTabs#order()}), so the list does not
 * echo the pack's own tab_order back. The pack's custom tabs are left out.
 */
final class CreativeTabs {
    static final String FILE = "creative_tabs.json";

    private CreativeTabs() {
    }

    static String write() throws IOException {
        JsonArray json = new JsonArray();
        List<ResourceLocation> ids = PristineTabs.ids(PristineTabs.order());
        ids.forEach(id -> json.add(id.toString()));
        DataFiles.write(FILE, json);
        return String.format("%,d creative tabs", ids.size());
    }
}
