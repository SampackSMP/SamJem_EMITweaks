package com.carjem.sampackemitweaks.client;

import com.carjem.sampackemitweaks.icondump.command.ExportCommand;
import com.carjem.sampackemitweaks.tabs.CreativeTabReload;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * The mod's client commands, all under one root:
 *
 * <pre>
 * /emitweaks reload_tabs
 * /emitweaks export [size] [mod &lt;id&gt; | modRegex &lt;regex&gt; | match &lt;regex&gt;]
 * /emitweaks export data [emi | chipped | tabs]
 * /emitweaks export gamedata
 * /emitweaks update_icons [size] &lt;regex&gt;
 * </pre>
 *
 * See {@link CreativeTabReload} and {@link ExportCommand}.
 */
public final class EmiTweaksCommands {
    public static final String ROOT = "emitweaks";

    private EmiTweaksCommands() {
    }

    public static void register(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT)
                .then(Commands.literal("reload_tabs").executes(CreativeTabReload::run));
        ExportCommand.addTo(root);
        event.getDispatcher().register(root);
    }
}
