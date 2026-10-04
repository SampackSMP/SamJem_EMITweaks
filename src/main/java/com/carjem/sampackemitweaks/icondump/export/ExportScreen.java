package com.carjem.sampackemitweaks.icondump.export;

import com.carjem.sampackemitweaks.icondump.IconDump;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

/**
 * Drives an ExportJob from its render() and shows how far it has got. Esc cancels.
 */
public final class ExportScreen extends Screen {
    private static final int BAR_WIDTH = 240;

    private final ExportJob job;
    private final String verb;
    private boolean closed;

    public ExportScreen(ExportJob job, String verb) {
        super(Component.literal("IconDump"));
        this.job = job;
        this.verb = verb;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        job.step(graphics);

        int total = Math.max(1, job.total());
        String line = job.rendering()
                ? String.format("%s icons: %,d / %,d", verb, job.rendered(), job.total())
                : "Writing sheets…";
        int x = (width - BAR_WIDTH) / 2, y = height / 2;
        graphics.drawCenteredString(font, line, width / 2, y - 16, 0xFFFFFF);
        graphics.fill(x, y, x + BAR_WIDTH, y + 6, 0xFF404040);
        graphics.fill(x, y, x + (int) ((long) BAR_WIDTH * job.rendered() / total), y + 6, 0xFF3FBF5F);
        graphics.drawCenteredString(font, "Esc to cancel", width / 2, y + 14, 0x808080);

        if (job.finished() && !closed) {
            closed = true;
            report();
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF101010);
    }

    /** Esc, or anything else that replaces this screen (a disconnect) before the job is done. */
    @Override
    public void removed() {
        if (!closed) {
            closed = true;
            job.cancel();
            String left = job.updating() ? " is unchanged" : " has no meta.json, so readers treat it as incomplete";
            message(Component.literal("IconDump: cancelled; " + job.dir().getFileName() + left)
                    .withStyle(ChatFormatting.YELLOW));
        }
        super.removed();
    }

    private void report() {
        Throwable error = job.error();
        if (error != null) {
            IconDump.LOG.error("Icon export failed", error);
            message(Component.literal("IconDump: failed writing " + job.dir().getFileName() + ": " + error.getMessage())
                    .withStyle(ChatFormatting.RED));
            return;
        }
        String path = job.dir().toAbsolutePath().toString();
        Component folder = Component.literal(job.dir().getFileName().toString()).withStyle(style -> style
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, path)));
        String summary = String.format(" — %,d icons on %d sheet(s) in %.1fs", job.total() - job.failedCount(),
                job.sheetCount(), job.seconds());
        Component done = Component.literal("IconDump: " + verb.toLowerCase() + " ").append(folder).append(summary);
        if (job.failedCount() > 0) {
            done = done.copy().append(Component.literal(String.format(" (%,d failed to render, see the log)", job.failedCount()))
                    .withStyle(ChatFormatting.YELLOW));
        }
        message(done);
    }

    private static void message(Component text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(text, false);
        }
    }
}
