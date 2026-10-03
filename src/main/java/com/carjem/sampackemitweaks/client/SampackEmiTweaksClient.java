package com.carjem.sampackemitweaks.client;

import com.carjem.sampackemitweaks.compat.ModelLocationsCache;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Client-only event wiring; only referenced when running on the client. */
public final class SampackEmiTweaksClient {
    private SampackEmiTweaksClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ModelEvent.BakingCompleted.class, event -> ModelLocationsCache.clear());
    }
}
