package com.carjem.sampackemitweaks.mixin.compat;

import com.carjem.sampackemitweaks.compat.SablePhysicsState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes Sable apply its block physics properties once per reload instead of
 * once per level.
 *
 * Sable.defaultSubLevelContainerInitializer calls applyAll() for every
 * ServerLevel it sees: each dimension, plus the fake levels Supplementaries
 * creates through Moonlight. applyAll() walks every definition, resolves its
 * block tag, and writes the properties onto every matching BlockState. None of
 * that depends on the level -- the definitions come from the datapack reload
 * (apply) and the targets are the global BlockStates -- so every call after the
 * first rewrites identical values. With this pack's block count that costs
 * ~0.5 s per level, ~4 s per world load.
 *
 * applyAll() now runs only when apply() has loaded new definitions since the
 * last run, so the first level after a world load or /reload still applies
 * them.
 */
@Mixin(targets = "dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertiesDefinitionLoader", remap = false)
public class SablePhysicsPropertiesLoaderMixin {

    @Inject(method = "apply", at = @At("TAIL"))
    private void sampack_emitweaks$markReloaded(CallbackInfo ci) {
        SablePhysicsState.definitionsApplied = false;
    }

    @Inject(method = "applyAll", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$skipIfApplied(CallbackInfo ci) {
        if (SablePhysicsState.definitionsApplied) {
            ci.cancel();
            return;
        }
        SablePhysicsState.definitionsApplied = true;
    }
}
