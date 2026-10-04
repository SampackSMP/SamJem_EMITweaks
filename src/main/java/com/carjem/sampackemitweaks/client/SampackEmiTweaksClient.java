package com.carjem.sampackemitweaks.client;

import com.carjem.sampackemitweaks.compat.ModelLocationsCache;
import com.carjem.sampackemitweaks.icondump.IconDump;
import com.carjem.sampackemitweaks.itemgroups.InventoryItemGroups;
import com.carjem.sampackemitweaks.itemgroups.config.ConfigHelper;
import com.carjem.sampackemitweaks.tabs.CreativeTabReload;
import com.carjem.sampackemitweaks.tabs.CreativeTabRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
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

        IconDump.init();

        InventoryItemGroups.init();
        // The item groups' config screen (Cloth Config or Simple Config Lib) when one is
        // installed; otherwise NeoForge's own screen, which covers the client config.
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> ConfigHelper.isConfigLoaded()
                        ? ConfigHelper.getScreen(parent)
                        : new ConfigurationScreen(mod, parent));
    }
}
