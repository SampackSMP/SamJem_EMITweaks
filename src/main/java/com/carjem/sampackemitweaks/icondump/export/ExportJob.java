package com.carjem.sampackemitweaks.icondump.export;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.carjem.sampackemitweaks.icondump.IconDump;
import com.carjem.sampackemitweaks.icondump.source.StackSources.Collected;
import com.carjem.sampackemitweaks.icondump.source.StackSources.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Matrix4f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * One export or update: renders its tiles, a batch per frame, onto sheet-sized offscreen
 * targets, and writes each finished sheet and finally meta.json from a worker thread.
 *
 * Where IconExporter draws one item per frame onto the screen and screenshots the whole window,
 * this draws straight into a sheet, so the GUI scale and window size play no part and a frame
 * carries many icons. Transparency is recovered rather than colour-keyed: every tile is drawn
 * twice, over black and over white, and for standard alpha blending
 *
 *     onBlack = a * c          onWhite = a * c + (1 - a)
 *
 * so a = 1 - (onWhite - onBlack) and c = onBlack / a, exactly, translucent edges included.
 *
 * Every sheet is composited onto what is already on disk, copying in only the tiles drawn this
 * time, which is what lets an update redraw a handful of stacks in place. Sheets and meta.json
 * are each replaced by a rename. The worker runs its tasks in order and meta.json is the last,
 * written only if every sheet was: a fresh export deletes the old meta.json up front, so a
 * cancelled one leaves none, and an update keeps the old one until the new one is complete.
 */
public final class ExportJob {

    /** One icon to draw. slot is the tile's place in its sheet, row * columns + column. */
    private record Tile(Entry entry, int sheet, int slot) {
    }

    private final Meta meta;
    private final List<Tile> tiles;
    private final boolean update;
    private final Set<String> drawnBefore;   // ids an update redraws; they keep their entry if the redraw fails
    private final Map<Integer, int[]> sheetSizes = new HashMap<>();
    private final int perFrame;
    private final boolean names;
    private final Path dir;
    private final long started = System.nanoTime();

    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "IconDump sheet writer");
        thread.setDaemon(true);
        return thread;
    });
    private final List<Future<?>> writes = new ArrayList<>();
    private final Set<String> failed = new LinkedHashSet<>();

    private int cursor;              // the next tile to render
    private int sheetStart;          // the first tile of the sheet being rendered
    private TextureTarget onBlack, onWhite;
    private Future<?> done;
    private volatile boolean cancelled;

    private ExportJob(Meta meta, List<Tile> tiles, boolean update, Set<String> drawnBefore, int perFrame, boolean names,
                      Path dir) {
        this.meta = meta;
        this.update = update;
        this.tiles = tiles;
        this.drawnBefore = drawnBefore;
        this.perFrame = perFrame;
        this.names = names;
        this.dir = dir;

        // every sheet is as wide as its rightmost tile and as tall as its last row, and an
        // update never shrinks one: the tiles it does not redraw are still in it
        for (int i = 0; i < meta.sheets.size(); i++) {
            Meta.Sheet sheet = meta.sheets.get(i);
            sheetSizes.put(i, new int[]{sheet.width(), sheet.height()});
        }
        for (Tile tile : tiles) {
            int[] size = sheetSizes.computeIfAbsent(tile.sheet(), s -> new int[2]);
            size[0] = Math.max(size[0], (Math.min(tile.slot(), meta.columns - 1) + 1) * meta.size);
            size[1] = Math.max(size[1], (tile.slot() / meta.columns + 1) * meta.size);
        }
    }

    public static int columnsFor(int size, int maxSheetSize) {
        return Math.max(1, Math.min(maxSheetSize, RenderSystem.maxSupportedTextureSize()) / size);
    }

    /** A fresh export of everything collected, replacing whatever the directory held. */
    public static ExportJob export(Collected collected, int size, int maxSheetSize, int perFrame, boolean names, Path dir)
            throws IOException {
        Files.createDirectories(dir);
        // meta.json first, so an export interrupted mid-clear already reads as incomplete
        Files.deleteIfExists(dir.resolve(Meta.FILE));
        try (Stream<Path> old = Files.list(dir)) {
            for (Path path : (Iterable<Path>) old::iterator) {
                String name = path.getFileName().toString();
                if (name.startsWith("sheet_") && name.endsWith(".png")) {
                    Files.delete(path);
                }
            }
        }
        int columns = columnsFor(size, maxSheetSize);
        int perSheet = columns * columns;
        List<Tile> tiles = new ArrayList<>();
        for (Entry entry : collected.entries()) {
            tiles.add(new Tile(entry, tiles.size() / perSheet, tiles.size() % perSheet));
        }
        return new ExportJob(new Meta(collected.source(), size, columns), tiles, false, Set.of(), perFrame, names, dir);
    }

    /**
     * Redraws what pattern matches in an existing export. A stack it already has is drawn over
     * its own tile; a new one goes after the last tile in use; a stack the pattern matches that
     * no longer exists loses its entry (its pixels stay, unreferenced). collected must already be
     * narrowed to the pattern.
     */
    public static ExportJob update(Collected collected, Pattern pattern, int size, int perFrame, boolean names, Path dir)
            throws IOException {
        if (!Files.exists(dir.resolve(Meta.FILE))) {
            throw new IOException("there is no export at " + dir + " to update; run /icondump export " + size + " first");
        }
        Meta meta = Meta.read(dir);
        if (meta.size != size) {
            throw new IOException(dir + " holds " + meta.size + "px icons, not " + size + "px");
        }
        int perSheet = meta.columns * meta.columns;
        Map<String, int[]> old = new HashMap<>(meta.icons);
        int next = 0;   // the first free slot, counted across sheets
        for (int[] at : meta.icons.values()) {
            next = Math.max(next, at[0] * perSheet + at[2] / size * meta.columns + at[1] / size + 1);
        }
        meta.icons.keySet().removeIf(id -> pattern.matcher(id).find());
        meta.names.keySet().removeIf(id -> pattern.matcher(id).find());
        meta.unlisted.removeIf(id -> pattern.matcher(id).find());
        meta.failed.removeIf(id -> pattern.matcher(id).find());

        List<Tile> tiles = new ArrayList<>();
        Set<String> drawnBefore = new HashSet<>();
        for (Entry entry : collected.entries()) {
            int[] at = old.get(entry.id());
            if (at != null) {
                drawnBefore.add(entry.id());
                tiles.add(new Tile(entry, at[0], at[2] / size * meta.columns + at[1] / size));
            } else {
                tiles.add(new Tile(entry, next / perSheet, next % perSheet));
                next++;
            }
        }
        tiles.sort(Comparator.comparingInt(Tile::sheet).thenComparingInt(Tile::slot));
        return new ExportJob(meta, tiles, true, drawnBefore, perFrame, names, dir);
    }

    public Path dir() {
        return dir;
    }

    /** An update keeps the export it started from usable throughout; a fresh export does not. */
    public boolean updating() {
        return update;
    }

    public int total() {
        return tiles.size();
    }

    public int rendered() {
        return cursor;
    }

    public int failedCount() {
        return failed.size();
    }

    public double seconds() {
        return (System.nanoTime() - started) / 1e9;
    }

    public boolean rendering() {
        return cursor < tiles.size();
    }

    /** True once meta.json is written, or the worker gave up; error() tells which. */
    public boolean finished() {
        return done != null && done.isDone();
    }

    /** Why the export failed, or null if it completed. Only meaningful once finished(). */
    public Throwable error() {
        try {
            done.get();
            return null;
        } catch (Exception e) {
            return e.getCause() != null ? e.getCause() : e;
        }
    }

    public int sheetCount() {
        return meta.sheets.size();
    }

    /** Called every frame from the progress screen's render, on the render thread. */
    public void step(GuiGraphics graphics) {
        if (cancelled || !rendering()) {
            return;
        }
        int sheet = tiles.get(cursor).sheet();
        if (onBlack == null) {
            int[] size = sheetSizes.get(sheet);
            onBlack = target(size[0], size[1], 0f);
            onWhite = target(size[0], size[1], 1f);
            sheetStart = cursor;
        }
        int end = cursor;
        while (end < tiles.size() && end - cursor < perFrame && tiles.get(end).sheet() == sheet) {
            end++;
        }
        draw(graphics, cursor, end);
        cursor = end;
        if (!rendering() || tiles.get(cursor).sheet() != sheet) {
            finishSheet(sheet, tiles.subList(sheetStart, cursor));
        }
        if (!rendering()) {
            List<Future<?>> sheetWrites = List.copyOf(writes);
            done = writer.submit(() -> {
                for (Future<?> write : sheetWrites) {
                    write.get();   // rethrows a failed sheet, so no meta.json is written over it
                }
                if (!cancelled) {
                    finishMeta();
                    meta.write(dir);
                }
                return null;
            });
            writer.shutdown();
        }
    }

    private static TextureTarget target(int width, int height, float background) {
        TextureTarget target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        target.setClearColor(background, background, background, 1f);
        target.clear(Minecraft.ON_OSX);
        return target;
    }

    private void draw(GuiGraphics graphics, int from, int to) {
        graphics.flush();   // whatever the screen queued belongs on the main target
        int width = onBlack.width, height = onBlack.height;
        RenderSystem.backupProjectionMatrix();
        // the GUI's own projection, sized to the sheet in pixels instead of the scaled window
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, width, height, 0, 1000f, ClientHooks.getGuiFarPlane()),
                VertexSorting.ORTHOGRAPHIC_Z);
        try {
            for (TextureTarget target : new TextureTarget[]{onBlack, onWhite}) {
                target.bindWrite(true);
                for (int i = from; i < to; i++) {
                    drawTile(graphics, tiles.get(i), height);
                }
            }
        } finally {
            RenderSystem.restoreProjectionMatrix();
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        }
    }

    private void drawTile(GuiGraphics graphics, Tile tile, int height) {
        int size = meta.size;
        int x = tile.slot() % meta.columns * size, y = tile.slot() / meta.columns * size;
        // GL's scissor box counts from the bottom; the projection puts row 0 at the top
        RenderSystem.enableScissor(x, height - y - size, size, size);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        // scale x and y on the matrix alone: PoseStack.scale would also rescale the normals
        // (squashing the lighting for any non-uniform scale), and leaving z at 1 keeps every
        // icon inside the GUI's depth range at any size
        graphics.pose().last().pose().scale(size / 16f, size / 16f, 1f);
        try {
            tile.entry().renderer().render(graphics);
            graphics.flush();
        } catch (Throwable t) {
            if (failed.add(tile.entry().id())) {
                IconDump.LOG.warn("Failed to render {}", tile.entry().id(), t);
            }
            try {
                graphics.flush();
            } catch (Throwable ignored) {
                // the buffer is left however the failed render left it; the tile is dropped anyway
            }
        } finally {
            graphics.pose().popPose();
            RenderSystem.disableScissor();
        }
    }

    private void finishSheet(int sheet, List<Tile> drawn) {
        NativeImage black = download(onBlack), white = download(onWhite);
        onBlack.destroyBuffers();
        onWhite.destroyBuffers();
        onBlack = onWhite = null;
        // decided here, on the render thread, which is the only one that touches `failed`
        List<Tile> good = drawn.stream().filter(t -> !failed.contains(t.entry().id())).toList();
        Path path = dir.resolve(Meta.sheetFile(sheet));
        int size = meta.size, columns = meta.columns;
        writes.add(writer.submit(() -> {
            try (black; white; NativeImage rendered = recoverAlpha(black, white);
                 NativeImage out = new NativeImage(rendered.getWidth(), rendered.getHeight(), true)) {
                if (cancelled) {
                    return null;
                }
                if (Files.exists(path)) {
                    try (InputStream in = Files.newInputStream(path); NativeImage old = NativeImage.read(in)) {
                        copy(old, out, 0, 0, Math.min(old.getWidth(), out.getWidth()), Math.min(old.getHeight(), out.getHeight()));
                    }
                }
                for (Tile tile : good) {
                    copy(rendered, out, tile.slot() % columns * size, tile.slot() / columns * size, size, size);
                }
                Path partial = path.resolveSibling(path.getFileName() + ".partial");
                out.writeToFile(partial);
                Files.move(partial, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            return null;
        }));
    }

    private static void copy(NativeImage from, NativeImage to, int x0, int y0, int width, int height) {
        for (int y = y0; y < y0 + height; y++) {
            for (int x = x0; x < x0 + width; x++) {
                to.setPixelRGBA(x, y, from.getPixelRGBA(x, y));
            }
        }
    }

    private static NativeImage download(TextureTarget target) {
        NativeImage image = new NativeImage(target.width, target.height, false);
        RenderSystem.bindTexture(target.getColorTextureId());
        image.downloadTexture(0, false);
        image.flipY();   // GL rows run bottom-up
        return image;
    }

    /** See the class comment. NativeImage pixels are ABGR. */
    static NativeImage recoverAlpha(NativeImage black, NativeImage white) {
        NativeImage out = new NativeImage(black.getWidth(), black.getHeight(), true);
        for (int y = 0; y < black.getHeight(); y++) {
            for (int x = 0; x < black.getWidth(); x++) {
                int b = black.getPixelRGBA(x, y), w = white.getPixelRGBA(x, y);
                int br = b & 0xFF, bg = b >> 8 & 0xFF, bb = b >> 16 & 0xFF;
                int spread = (w & 0xFF) - br + (w >> 8 & 0xFF) - bg + (w >> 16 & 0xFF) - bb;
                int alpha = Math.clamp(255 - Math.round(spread / 3f), 0, 255);
                out.setPixelRGBA(x, y, alpha == 0 ? 0
                        : alpha << 24 | unpremultiply(bb, alpha) << 16 | unpremultiply(bg, alpha) << 8 | unpremultiply(br, alpha));
            }
        }
        return out;
    }

    private static int unpremultiply(int channel, int alpha) {
        return Math.min(255, (channel * 255 + alpha / 2) / alpha);
    }

    /** Runs on the worker once every sheet is written, so `failed` is final by then. */
    private void finishMeta() {
        int size = meta.size;
        for (Tile tile : tiles) {
            Entry entry = tile.entry();
            if (failed.contains(entry.id())) {
                meta.failed.add(entry.id());
                if (!drawnBefore.contains(entry.id())) {
                    continue;   // never drawn; an earlier export's tile still holds a redraw that failed
                }
            }
            meta.icons.put(entry.id(), new int[]{tile.sheet(), tile.slot() % meta.columns * size, tile.slot() / meta.columns * size});
            if (names && !entry.name().isEmpty()) {
                meta.names.put(entry.id(), entry.name());
            }
            if (entry.unlisted()) {
                meta.unlisted.add(entry.id());
            }
        }
        int[] counts = new int[sheetSizes.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1];
        for (int[] at : meta.icons.values()) {
            counts[at[0]]++;
        }
        meta.sheets.clear();
        for (int sheet = 0; sheet < counts.length; sheet++) {
            int[] dims = sheetSizes.get(sheet);
            meta.sheets.add(new Meta.Sheet(Meta.sheetFile(sheet), dims[0], dims[1], counts[sheet]));
        }
    }

    /** Esc on the progress screen. A sheet already being written finishes; no other file is touched. */
    public void cancel() {
        cancelled = true;
        if (onBlack != null) {
            onBlack.destroyBuffers();
            onWhite.destroyBuffers();
            onBlack = onWhite = null;
        }
        writer.shutdown();
    }
}
