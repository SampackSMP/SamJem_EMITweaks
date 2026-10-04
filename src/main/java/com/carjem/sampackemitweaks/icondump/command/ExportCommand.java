package com.carjem.sampackemitweaks.icondump.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.carjem.sampackemitweaks.client.ClientConfig;
import com.carjem.sampackemitweaks.icondump.IconDump;
import com.carjem.sampackemitweaks.icondump.export.ExportJob;
import com.carjem.sampackemitweaks.icondump.export.ExportScreen;
import com.carjem.sampackemitweaks.icondump.source.StackSources;
import com.carjem.sampackemitweaks.icondump.source.StackSources.Collected;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforgespi.language.IModInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * <pre>
 * /icondump export [size] [mod &lt;id&gt; | modRegex &lt;regex&gt; | match &lt;regex&gt;]
 * /icondump update [size] &lt;regex&gt;
 * </pre>
 *
 * export writes a fresh icon-sheets-x&lt;size&gt;, of everything or of what the filter picks, as
 * IconExporter's export does. modRegex is matched against the namespace, match against the
 * stack id meta.json keys it by (item:minecraft:oak_log, item:minecraft:potion{...}), both
 * anywhere in it: anchor with ^ and $ for a whole match.
 *
 * update redraws only the stacks whose id matches, in place in an existing export, and adds the
 * matching stacks it lacks; everything else in it is left as it was.
 */
public final class ExportCommand {

    private static final SuggestionProvider<CommandSourceStack> MOD_IDS = (context, builder) ->
            SharedSuggestionProvider.suggest(ModList.get().getMods().stream().map(IModInfo::getModId), builder);

    private ExportCommand() {
    }

    public static void register(RegisterClientCommandsEvent event) {
        ToIntFunction<CommandContext<CommandSourceStack>> sizeArgument = ExportCommand::size;
        ToIntFunction<CommandContext<CommandSourceStack>> sizeDefault = c -> defaultSize();
        event.getDispatcher().register(Commands.literal("icondump")
                .then(withFilters(Commands.literal("export"), sizeDefault)
                        .then(withFilters(Commands.argument("size", IntegerArgumentType.integer(1, 512)), sizeArgument)))
                .then(Commands.literal("update")
                        .then(Commands.argument("size", IntegerArgumentType.integer(1, 512))
                                .then(Commands.argument("pattern", StringArgumentType.greedyString())
                                        .executes(c -> withPattern(c, p -> update(c, size(c), p)))))
                        .then(Commands.argument("pattern", StringArgumentType.greedyString())
                                .executes(c -> withPattern(c, p -> update(c, defaultSize(), p))))));
    }

    /**
     * The export forms that follow `export` and `export &lt;size&gt;` alike: none (everything), or one
     * filter. size reads the size from wherever this node hangs, the argument or the config.
     */
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T withFilters(
            T node, ToIntFunction<CommandContext<CommandSourceStack>> size) {
        return node
                .executes(c -> export(c, size.applyAsInt(c), ns -> true, id -> true))
                .then(Commands.literal("mod")
                        .then(Commands.argument("mod", StringArgumentType.word())
                                .suggests(MOD_IDS)
                                .executes(c -> export(c, size.applyAsInt(c), StringArgumentType.getString(c, "mod")::equals,
                                        id -> true))))
                .then(Commands.literal("modRegex")
                        .then(Commands.argument("pattern", StringArgumentType.greedyString())
                                .executes(c -> withPattern(c, p -> export(c, size.applyAsInt(c), ns -> p.matcher(ns).find(),
                                        id -> true)))))
                .then(Commands.literal("match")
                        .then(Commands.argument("pattern", StringArgumentType.greedyString())
                                .executes(c -> withPattern(c, p -> export(c, size.applyAsInt(c), ns -> true,
                                        id -> p.matcher(id).find())))));
    }

    private static int defaultSize() {
        return ClientConfig.ICON_SIZE.get();
    }

    private static int size(CommandContext<CommandSourceStack> context) {
        return IntegerArgumentType.getInteger(context, "size");
    }

    private interface PatternCommand {
        int run(Pattern pattern);
    }

    private static int withPattern(CommandContext<CommandSourceStack> context, PatternCommand command) {
        String regex = StringArgumentType.getString(context, "pattern");
        try {
            return command.run(Pattern.compile(regex));
        } catch (PatternSyntaxException e) {
            context.getSource().sendFailure(Component.literal("Not a valid regex: " + e.getDescription()));
            return 0;
        }
    }

    private static Path directory(int size) {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("icon-sheets-x" + size);
    }

    private static Collected collect(CommandContext<CommandSourceStack> context, Predicate<String> wantNamespace,
                                     Predicate<String> wantId) {
        String notReady = StackSources.notReady();
        if (notReady != null) {
            context.getSource().sendFailure(Component.literal(notReady));
            return null;
        }
        Collected collected = StackSources.collect(wantNamespace, wantId);
        if (collected.entries().isEmpty()) {
            context.getSource().sendFailure(Component.literal("Nothing matches; no icons to export"));
            return null;
        }
        return collected;
    }

    private static int export(CommandContext<CommandSourceStack> context, int size, Predicate<String> wantNamespace,
                              Predicate<String> wantId) {
        int maxSheetSize = ClientConfig.MAX_SHEET_SIZE.get();
        if (size > maxSheetSize) {
            context.getSource().sendFailure(Component.literal("Icons of " + size + "px do not fit a " + maxSheetSize
                    + "px sheet; raise icon_export.max_sheet_size in the config"));
            return 0;
        }
        Collected collected = collect(context, wantNamespace, wantId);
        if (collected == null) {
            return 0;
        }
        return start(context, collected, "Exporting", () -> ExportJob.export(collected, size, maxSheetSize,
                ClientConfig.ICONS_PER_FRAME.get(), ClientConfig.INCLUDE_NAMES.get(), directory(size)));
    }

    private static int update(CommandContext<CommandSourceStack> context, int size, Pattern pattern) {
        Collected collected = collect(context, ns -> true, id -> pattern.matcher(id).find());
        if (collected == null) {
            return 0;
        }
        return start(context, collected, "Updating", () -> ExportJob.update(collected, pattern, size,
                ClientConfig.ICONS_PER_FRAME.get(), ClientConfig.INCLUDE_NAMES.get(), directory(size)));
    }

    private interface JobFactory {
        ExportJob create() throws IOException;
    }

    private static int start(CommandContext<CommandSourceStack> context, Collected collected, String verb, JobFactory factory) {
        ExportJob job;
        try {
            job = factory.create();
        } catch (IOException e) {
            IconDump.LOG.error("Could not start the icon export", e);
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
        int count = collected.entries().size();
        context.getSource().sendSuccess(() -> Component.literal(String.format("%s %,d icons from %s into %s",
                verb, count, collected.source(), job.dir().getFileName())), false);
        // the chat screen this command was typed into closes after it returns; open ours after that
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> mc.setScreen(new ExportScreen(job, verb)));
        return count;
    }
}
