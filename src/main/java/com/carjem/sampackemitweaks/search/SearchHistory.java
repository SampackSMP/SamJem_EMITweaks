package com.carjem.sampackemitweaks.search;

import com.carjem.sampackemitweaks.SampackEmiTweaks;
import com.carjem.sampackemitweaks.client.ClientConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * EMI's search history, newest first. EMI's search bar keeps its history in a plain list: it adds
 * the text when the bar loses focus (moving a repeat to the front) and cycles through it with the
 * arrow keys. {@link com.carjem.sampackemitweaks.mixin.emi.EmiSearchWidgetMixin} hands it this
 * list instead, so the history survives restarts and the dropdown shows the same entries.
 */
public final class SearchHistory {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve(SampackEmiTweaks.MOD_ID).resolve("search_history.json");

    private static final List<String> ENTRIES = load();
    // What was last written, to skip writes that change nothing.
    private static List<String> saved = List.copyOf(ENTRIES);

    private SearchHistory() {
    }

    /** The live list EMI's search bar reads and writes. */
    public static List<String> entries() {
        return ENTRIES;
    }

    /** Adds a search the way EMI does when the bar loses focus, then trims and saves. */
    public static void record(String text) {
        if (text.isBlank()) return;
        ENTRIES.removeIf(String::isBlank);
        ENTRIES.remove(text);
        ENTRIES.add(0, text);
        changed();
    }

    public static void remove(String text) {
        ENTRIES.remove(text);
        changed();
    }

    public static void clearAll() {
        ENTRIES.clear();
        changed();
    }

    /** Trims the list to the configured size and saves it if it changed. */
    public static void changed() {
        trim();
        if (ENTRIES.equals(saved)) return;
        saved = List.copyOf(ENTRIES);
        if (ClientConfig.get(ClientConfig.SAVE_SEARCH_HISTORY)) save();
    }

    /**
     * After the history settings change: trims to the new size, and writes the file, or deletes it
     * once the history is no longer to be kept.
     */
    public static void settingsChanged() {
        trim();
        saved = List.copyOf(ENTRIES);
        if (ClientConfig.get(ClientConfig.SAVE_SEARCH_HISTORY)) {
            save();
        } else {
            try {
                Files.deleteIfExists(FILE);
            } catch (IOException e) {
                SampackEmiTweaks.LOGGER.warn("Couldn't delete the search history at {}", FILE, e);
            }
        }
    }

    private static void trim() {
        int size = ClientConfig.get(ClientConfig.SEARCH_HISTORY_SIZE);
        while (ENTRIES.size() > size) {
            ENTRIES.removeLast();
        }
    }

    private static List<String> load() {
        List<String> entries = new ArrayList<>();
        if (!Files.isRegularFile(FILE)) return entries;
        try (Reader reader = Files.newBufferedReader(FILE)) {
            for (JsonElement element : JsonParser.parseReader(reader).getAsJsonArray()) {
                String text = element.getAsString();
                if (!text.isBlank() && !entries.contains(text)) entries.add(text);
            }
        } catch (IOException | RuntimeException e) {
            SampackEmiTweaks.LOGGER.warn("Couldn't read the search history from {}", FILE, e);
        }
        return entries;
    }

    private static void save() {
        JsonArray array = new JsonArray();
        ENTRIES.forEach(array::add);
        try {
            Files.createDirectories(FILE.getParent());
            Path temp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp)) {
                GSON.toJson(array, writer);
            }
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            SampackEmiTweaks.LOGGER.warn("Couldn't save the search history to {}", FILE, e);
        }
    }
}
