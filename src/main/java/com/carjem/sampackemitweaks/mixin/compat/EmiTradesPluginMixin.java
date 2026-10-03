package com.carjem.sampackemitweaks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops EMI Trades from rolling trades with the client level's random.
 *
 * EMI Trades builds its recipes inside EMI's reload worker thread and passes
 * Minecraft.level.random to every ItemListing.getOffer call. That random is a
 * LegacyRandomSource guarded by a ThreadingDetector, and the Render thread uses
 * it every tick in ClientLevel.animateTick. When the two overlap, the game
 * crashes with "Accessing LegacyRandomSource from multiple threads".
 *
 * The offers only feed EMI's recipe display, so any random will do. Handing
 * getOffer a per-thread source of its own keeps the level's random on the
 * Render thread.
 */
@Mixin(targets = "moe.prwk.emitrades.EMITradesPlugin", remap = false)
public class EmiTradesPluginMixin {

    @Unique
    private static final ThreadLocal<RandomSource> sampack_emitweaks$offerRandom = ThreadLocal.withInitial(RandomSource::create);

    @ModifyExpressionValue(
            method = {"lambda$register$3", "lambda$register$5"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/ClientLevel;random:Lnet/minecraft/util/RandomSource;"))
    private static RandomSource sampack_emitweaks$useOwnRandom(RandomSource levelRandom) {
        return sampack_emitweaks$offerRandom.get();
    }
}
