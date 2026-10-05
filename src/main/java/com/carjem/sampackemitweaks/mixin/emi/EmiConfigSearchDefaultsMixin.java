package com.carjem.sampackemitweaks.mixin.emi;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.emi.config.EmiConfig;

/**
 * EMI's plain search matches tooltips by default, and EMI matches by substring, so tooltip lines
 * pull in unrelated items ("light" finds wooden buttons). Matching mod names by default does the
 * same ("light" finds all of Twilight Forest). Both default to off here; {@code $} and {@code @}
 * still search them. These are only defaults: {@code emi.css} overrides them once it has the keys.
 */
@Mixin(value = EmiConfig.class, remap = false)
public class EmiConfigSearchDefaultsMixin {

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void sampack_emitweaks$searchDefaults(CallbackInfo ci) {
        EmiConfig.searchTooltipByDefault = false;
        EmiConfig.searchModNameByDefault = false;
    }
}
