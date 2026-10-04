package com.carjem.sampackemitweaks.icondump.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Where /icondump data writes, and how: pretty json, through a temp file and a rename. */
public final class DataFiles {
    public static final String DIRECTORY = "icondump";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private DataFiles() {
    }

    public static Path directory() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(DIRECTORY);
    }

    /**
     * file is relative to directory() and may name a subdirectory (pack/items.json). A reader
     * never sees half a file: either the old one or the whole new one.
     */
    static void write(String file, JsonElement json) throws IOException {
        Path target = directory().resolve(file);
        Files.createDirectories(target.getParent());
        Path partial = target.resolveSibling(target.getFileName() + ".partial");
        try (Writer writer = Files.newBufferedWriter(partial, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
            writer.write('\n');
        }
        Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
