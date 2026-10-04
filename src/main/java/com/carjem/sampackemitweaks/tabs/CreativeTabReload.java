package com.carjem.sampackemitweaks.tabs;

import com.carjem.sampackemitweaks.SampackEmiTweaks;
import com.carjem.sampackemitweaks.mixin.tabs.CreativeModeTabIconAccessor;
import com.carjem.sampackemitweaks.mixin.tabs.CreativeModeTabsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/**
 * Re-reads the creative tab rules in a running game, so an InvIndexLedger build applies without a
 * restart. Client only.
 *
 * {@code /sampack_emitweaks reload_tabs} re-reads the file, rebuilds every tab, and reloads EMI so
 * REMI's tab sidebar and EMI's index pick the change up. F3+T re-reads the file and rebuilds the
 * tabs too, leaving EMI to its own resource reload. Reopen the creative inventory to see the new
 * pages. A custom tab id that was not registered at startup needs a restart; the command says so.
 */
public final class CreativeTabReload {
    private CreativeTabReload() {
    }

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event -> event.getDispatcher().register(
                Commands.literal(SampackEmiTweaks.MOD_ID).then(Commands.literal("reload_tabs").executes(context -> {
                    List<ResourceLocation> pending = reload();
                    if (ModList.get().isLoaded("emi")) {
                        EmiReload.reload();
                    }
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Reloaded creative tab rules from " + CreativeTabRules.path().getFileName()
                                    + "; reopen the creative inventory to see them"), false);
                    if (!pending.isEmpty()) {
                        context.getSource().sendFailure(Component.literal(
                                pending.size() + " new tab(s) need a restart to appear: " + pending));
                    }
                    return 1;
                }))));
        modBus.addListener(RegisterClientReloadListenersEvent.class, event ->
                event.registerReloadListener((ResourceManagerReloadListener) manager -> reload()));
    }

    /** Re-reads the rules and rebuilds the tabs; returns the custom tab ids that need a restart. */
    public static List<ResourceLocation> reload() {
        CreativeTabRules.load();
        List<ResourceLocation> pending = CreativeTabRules.get().unregisteredCustomTabs();
        if (!pending.isEmpty()) {
            SampackEmiTweaks.LOGGER.warn("Creative tabs {} were added since startup; restart the game to see them", pending);
        }

        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            if (CreativeTabRules.isOwnTab(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab))) {
                ((CreativeModeTabIconAccessor) tab).sampack_emitweaks$setIconItemStack(null);
            }
        }
        // null until the creative inventory has first been built; it builds from the new rules then
        CreativeModeTab.ItemDisplayParameters parameters = CreativeModeTabsAccessor.sampack_emitweaks$getCachedParameters();
        if (parameters != null) {
            CreativeModeTabsAccessor.sampack_emitweaks$buildAllTabContents(parameters);
        }
        return pending;
    }

    /** Kept apart so EMI's classes are only loaded when EMI is installed. */
    private static final class EmiReload {
        static void reload() {
            if (Minecraft.getInstance().level != null) {
                dev.emi.emi.runtime.EmiReloadManager.reload();
            }
        }
    }
}
