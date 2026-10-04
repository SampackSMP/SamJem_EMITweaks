package com.carjem.sampackemitweaks.icondump.data;

import com.mojang.brigadier.context.CommandContext;
import com.carjem.sampackemitweaks.icondump.IconDump;
import com.carjem.sampackemitweaks.icondump.source.StackSources;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.io.IOException;
import java.util.List;

/**
 * <pre>
 * /icondump data [emi | chipped | tabs]
 * /icondump pack
 * </pre>
 *
 * Writes what InvIndexLedger reads from the game into &lt;minecraft&gt;/icondump/, all three files
 * or just the one named: emi_dump.json (EmiDump), chipped_recipes.json (ChippedRecipes) and
 * creative_tabs.json (CreativeTabs). These replace the KubeJS /emidump script, its datapack-load
 * recipe capture, and Recreative's `/recreative dump tabs`. pack is the KubeJS /packdump: see PackDump.
 */
public final class DataCommand {

    /** A part that cannot be written here and now, and why; the others still run. */
    static final class Skipped extends Exception {
        Skipped(String reason) {
            super(reason);
        }
    }

    private interface Part {
        String write() throws IOException, Skipped;
    }

    private record Named(String name, String file, Part part) {
    }

    private static final List<Named> PARTS = List.of(
            new Named("emi", EmiDump.FILE, DataCommand::emi),
            new Named("chipped", ChippedRecipes.FILE, ChippedRecipes::write),
            new Named("tabs", CreativeTabs.FILE, CreativeTabs::write));

    private DataCommand() {
    }

    public static void register(RegisterClientCommandsEvent event) {
        var data = Commands.literal("data").executes(c -> run(c, PARTS));
        for (Named part : PARTS) {
            data.then(Commands.literal(part.name()).executes(c -> run(c, List.of(part))));
        }
        event.getDispatcher().register(Commands.literal("icondump").then(data)
                .then(Commands.literal("pack").executes(PackDump::run)));
    }

    /** EmiDump names EMI's classes, so it is only touched once EMI is known to be loaded. */
    private static String emi() throws IOException, Skipped {
        if (!StackSources.emiPresent()) {
            throw new Skipped("EMI is not installed");
        }
        return EmiDump.write();
    }

    private static int run(CommandContext<CommandSourceStack> context, List<Named> parts) {
        CommandSourceStack source = context.getSource();
        int written = 0;
        for (Named part : parts) {
            try {
                String summary = part.part().write();
                source.sendSuccess(() -> Component.literal(part.file() + ": " + summary), false);
                written++;
            } catch (Skipped e) {
                source.sendFailure(Component.literal(part.file() + " skipped: " + e.getMessage()));
            } catch (IOException | RuntimeException e) {
                IconDump.LOG.error("Could not write {}", part.file(), e);
                source.sendFailure(Component.literal(part.file() + " failed: " + e.getMessage()));
            }
        }
        if (written > 0) {
            String dir = DataFiles.directory().toAbsolutePath().toString();
            Component done = Component.literal("Wrote " + written + " file(s) into ")
                    .append(Component.literal(DataFiles.DIRECTORY + "/").withStyle(Style.EMPTY
                            .withColor(ChatFormatting.GREEN).withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, dir))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Click to copy the path")))));
            source.sendSuccess(() -> done, false);
        }
        return written;
    }
}
