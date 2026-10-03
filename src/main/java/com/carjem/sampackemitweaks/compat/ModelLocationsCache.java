package com.carjem.sampackemitweaks.compat;

import java.util.Set;
import net.minecraft.client.resources.model.ModelResourceLocation;

/**
 * Puzzles Lib's top-level model location set, shared by every model handler in
 * one model bake. It holds ~2 million locations, so it is dropped as soon as the
 * bake completes instead of living for the whole session.
 */
public final class ModelLocationsCache {
    public static volatile Set<ModelResourceLocation> topLevelLocations;

    private ModelLocationsCache() {
    }

    public static void clear() {
        topLevelLocations = null;
    }
}
