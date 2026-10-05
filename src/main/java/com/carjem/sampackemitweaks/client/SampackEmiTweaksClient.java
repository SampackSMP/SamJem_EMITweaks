package com.carjem.sampackemitweaks.client;

import com.carjem.sampackemitweaks.compat.ModelLocationsCache;
import com.carjem.sampackemitweaks.config.EmiConfigSections;
import com.carjem.sampackemitweaks.tabs.CreativeTabReload;
import com.carjem.sampackemitweaks.tabs.CreativeTabRules;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Client-only event wiring; only referenced when running on the client. */
public final class SampackEmiTweaksClient {
    private SampackEmiTweaksClient() {
    }

    public static void init(IEventBus modBus, ModContainer container) {
        modBus.addListener(ModelEvent.BakingCompleted.class, event -> ModelLocationsCache.clear());

        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        CreativeTabRules.load();
        modBus.addListener(RegisterEvent.class, CreativeTabRules::register);
        CreativeTabReload.init(modBus);

        NeoForge.EVENT_BUS.addListener(EmiTweaksCommands::register);

        // Every setting is in EMI's config screen; the mod list's buttons for this mod and for
        // REMI open it at their settings. REMI registers its own button as it loads, so replace
        // that once every mod has.
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> EmiConfigs.open(parent, false));
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() ->
                ModList.get().getModContainerById("remi").ifPresent(remi -> remi.registerExtensionPoint(
                        IConfigScreenFactory.class, (IConfigScreenFactory) (mod, parent) -> EmiConfigs.open(parent, true)))));
    }

    /** Kept apart so EMI's classes load only when the button is pressed. */
    private static final class EmiConfigs {
        static Screen open(Screen parent, boolean remi) {
            return EmiConfigSections.open(parent, remi ? EmiConfigSections.REMI : EmiConfigSections.OWN);
        }
    }
}
