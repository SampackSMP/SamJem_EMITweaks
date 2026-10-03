package com.carjem.sampackemitweaks.compat;

/**
 * Whether Sable's current physics property definitions are already applied to
 * the global BlockStates. Shared by the server-side loader mixin and the
 * client-side packet mixin, which run in the same JVM in singleplayer.
 */
public final class SablePhysicsState {
    /** Set by applyAll(); cleared whenever the datapack reload loads new definitions. */
    public static volatile boolean definitionsApplied = false;

    private SablePhysicsState() {
    }
}
