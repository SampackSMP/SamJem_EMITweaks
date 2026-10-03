package com.carjem.sampackemitweaks.mixin.compat;

import com.carjem.sampackemitweaks.compat.SablePhysicsState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips Sable's client-side physics property sync when it would only repeat
 * the integrated server's work.
 *
 * On join the server sends one ClientboundPhysicsPropertyPacket per definition,
 * and the client applies each to every matching BlockState on the Render
 * thread (~0.9 s). BlockStates are global, so in singleplayer the integrated
 * server has already written these exact values into the same objects.
 *
 * The packet is skipped only while hosting a local world and only when the
 * server has applied the current definitions. After a /reload the flag is
 * clear, so the packets still apply; on a remote server they always apply.
 */
@Mixin(targets = "dev.ryanhcode.sable.network.packets.tcp.ClientboundPhysicsPropertyPacket", remap = false)
public class SablePhysicsPropertyPacketMixin {

    @Inject(method = "lambda$handle$0", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$skipOnLocalServer(CallbackInfo ci) {
        if (SablePhysicsState.definitionsApplied && Minecraft.getInstance().hasSingleplayerServer()) {
            ci.cancel();
        }
    }
}
