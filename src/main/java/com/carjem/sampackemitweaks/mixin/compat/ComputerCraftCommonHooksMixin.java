package com.carjem.sampackemitweaks.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Skips ComputerCraft's server-start creative-tab rebuild in singleplayer.
 *
 * CC's onServerStarted calls CreativeModeTabs.tryRebuildTabContents so that
 * getItemDetail can report which creative tabs an item is in. With this pack's
 * item count a full rebuild takes ~13 s, and it runs on the server thread in
 * the middle of world load.
 *
 * On an integrated server that rebuild is redundant: tab contents live on the
 * shared CreativeModeTab objects, and the client rebuilds them itself right
 * after joining, so CC sees the same contents moments later. A dedicated server
 * has no client to do that, so it keeps CC's original behaviour.
 */
@Mixin(targets = "dan200.computercraft.shared.CommonHooks", remap = false)
public class ComputerCraftCommonHooksMixin {

    @WrapOperation(
            method = "onServerStarted",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTabs;tryRebuildTabContents(Lnet/minecraft/world/flag/FeatureFlagSet;ZLnet/minecraft/core/HolderLookup$Provider;)Z"))
    private static boolean sampack_emitweaks$skipOnIntegratedServer(
            FeatureFlagSet flags,
            boolean hasPermissions,
            HolderLookup.Provider lookup,
            Operation<Boolean> original,
            @Local(argsOnly = true) MinecraftServer server) {
        if (!server.isDedicatedServer()) {
            return false;
        }
        return original.call(flags, hasPermissions, lookup);
    }
}
