package com.carjem.sampackemitweaks;

import com.carjem.sampackemitweaks.client.SampackEmiTweaksClient;
import com.carjem.sampackemitweaks.compat.ItemAbilityItemsCache;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EMI reload and general load-time patches, almost all of them mixins; see the
 * classes under {@link com.carjem.sampackemitweaks.mixin}. Also carries the
 * creative-inventory item groups under {@link com.carjem.sampackemitweaks.itemgroups}.
 */
@Mod(SampackEmiTweaks.MOD_ID)
public class SampackEmiTweaks {
    public static final String MOD_ID = "sampack_emitweaks";
    public static final Logger LOGGER = LoggerFactory.getLogger("Sampack EMI Tweaks");

    public SampackEmiTweaks(IEventBus modBus, ModContainer container) {
        NeoForge.EVENT_BUS.addListener(TagsUpdatedEvent.class, event -> ItemAbilityItemsCache.clear());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SampackEmiTweaksClient.init(modBus, container);
        }
    }
}
