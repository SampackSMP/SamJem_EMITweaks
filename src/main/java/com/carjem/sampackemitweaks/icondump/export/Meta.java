package com.carjem.sampackemitweaks.icondump.export;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import com.carjem.sampackemitweaks.SampackEmiTweaks;
import net.neoforged.fml.ModList;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * meta.json, the index of one export directory:
 *
 * <pre>
 * format    1
 * source    "emi", or "creative" when EMI was not installed
 * size      icon size in pixels
 * columns   tiles per sheet row; a tile's slot is row * columns + column
 * sheets    [{file, width, height, count}], in sheet number order
 * icons     stack id -> [sheet, x, y], the tile's top-left pixel, in export order
 * names     stack id -> display name (optional)
 * unlisted  ids shown by neither EMI nor a creative tab: registry-only items
 * failed    ids whose render threw; they have no icons entry unless an earlier export drew them
 * </pre>
 *
 * Written last, through a temp file and a rename, so its presence means the sheets beside it
 * are complete.
 */
public final class Meta {
    public static final String FILE = "meta.json";
    public static final int FORMAT = 1;

    public record Sheet(String file, int width, int height, int count) {
    }

    public String source;
    public int size;
    public int columns;
    public final List<Sheet> sheets = new ArrayList<>();
    public final Map<String, int[]> icons = new LinkedHashMap<>();
    public final Map<String, String> names = new LinkedHashMap<>();
    public final Set<String> unlisted = new LinkedHashSet<>();
    public final Set<String> failed = new LinkedHashSet<>();

    public Meta(String source, int size, int columns) {
        this.source = source;
        this.size = size;
        this.columns = columns;
    }

    public static String sheetFile(int sheet) {
        return String.format("sheet_%03d.png", sheet);
    }

    public static Meta read(Path dir) throws IOException {
        JsonObject json;
        try (Reader reader = Files.newBufferedReader(dir.resolve(FILE), StandardCharsets.UTF_8)) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("unreadable " + FILE + ": " + e.getMessage(), e);
        }
        if (json.get("format").getAsInt() != FORMAT || !json.has("columns")) {
            throw new IOException(FILE + " is from a different version; export again instead");
        }
        Meta meta = new Meta(json.get("source").getAsString(), json.get("size").getAsInt(), json.get("columns").getAsInt());
        for (JsonElement sheet : json.getAsJsonArray("sheets")) {
            JsonObject s = sheet.getAsJsonObject();
            meta.sheets.add(new Sheet(s.get("file").getAsString(), s.get("width").getAsInt(),
                    s.get("height").getAsInt(), s.get("count").getAsInt()));
        }
        for (Map.Entry<String, JsonElement> icon : json.getAsJsonObject("icons").entrySet()) {
            var at = icon.getValue().getAsJsonArray();
            meta.icons.put(icon.getKey(), new int[]{at.get(0).getAsInt(), at.get(1).getAsInt(), at.get(2).getAsInt()});
        }
        if (json.has("names")) {
            json.getAsJsonObject("names").entrySet().forEach(e -> meta.names.put(e.getKey(), e.getValue().getAsString()));
        }
        if (json.has("unlisted")) {
            json.getAsJsonArray("unlisted").forEach(e -> meta.unlisted.add(e.getAsString()));
        }
        if (json.has("failed")) {
            json.getAsJsonArray("failed").forEach(e -> meta.failed.add(e.getAsString()));
        }
        return meta;
    }

    /**
     * Hand-written rather than Gson's pretty printer, which would spend five lines on every
     * [sheet, x, y]: one icon per line keeps a 30k-stack file readable and diffable.
     */
    public void write(Path dir) throws IOException {
        Gson gson = new Gson();
        StringBuilder out = new StringBuilder("{\n");
        out.append("  \"format\": ").append(FORMAT).append(",\n");
        out.append("  \"generator\": ").append(gson.toJson(generator())).append(",\n");
        out.append("  \"minecraft\": ").append(gson.toJson(SharedConstants.getCurrentVersion().getName())).append(",\n");
        out.append("  \"source\": ").append(gson.toJson(source)).append(",\n");
        out.append("  \"size\": ").append(size).append(",\n");
        out.append("  \"columns\": ").append(columns).append(",\n");
        out.append("  \"created\": ").append(gson.toJson(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString())).append(",\n");
        out.append("  \"sheets\": [");
        for (int i = 0; i < sheets.size(); i++) {
            Sheet s = sheets.get(i);
            out.append(i == 0 ? "\n" : ",\n").append("    {\"file\": ").append(gson.toJson(s.file()))
                    .append(", \"width\": ").append(s.width()).append(", \"height\": ").append(s.height())
                    .append(", \"count\": ").append(s.count()).append('}');
        }
        out.append(sheets.isEmpty() ? "],\n" : "\n  ],\n");
        out.append("  \"icons\": {");
        String sep = "\n";
        for (Map.Entry<String, int[]> icon : icons.entrySet()) {
            int[] at = icon.getValue();
            out.append(sep).append("    ").append(gson.toJson(icon.getKey()))
                    .append(": [").append(at[0]).append(", ").append(at[1]).append(", ").append(at[2]).append(']');
            sep = ",\n";
        }
        out.append(icons.isEmpty() ? "},\n" : "\n  },\n");
        out.append("  \"names\": {");
        sep = "\n";
        for (Map.Entry<String, String> name : names.entrySet()) {
            out.append(sep).append("    ").append(gson.toJson(name.getKey())).append(": ").append(gson.toJson(name.getValue()));
            sep = ",\n";
        }
        out.append(names.isEmpty() ? "},\n" : "\n  },\n");
        appendList(out, gson, "unlisted", unlisted).append(",\n");
        appendList(out, gson, "failed", failed).append("\n}\n");

        Path partial = dir.resolve(FILE + ".partial");
        Files.writeString(partial, out, StandardCharsets.UTF_8);
        Files.move(partial, dir.resolve(FILE), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static StringBuilder appendList(StringBuilder out, Gson gson, String key, Set<String> ids) {
        out.append("  ").append(gson.toJson(key)).append(": [");
        String sep = "\n";
        for (String id : ids) {
            out.append(sep).append("    ").append(gson.toJson(id));
            sep = ",\n";
        }
        return out.append(ids.isEmpty() ? "]" : "\n  ]");
    }

    private static String generator() {
        String version = ModList.get().getModContainerById(SampackEmiTweaks.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString()).orElse("?");
        return SampackEmiTweaks.MOD_ID + " " + version;
    }
}
