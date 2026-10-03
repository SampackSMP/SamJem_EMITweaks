package com.carjem.sampackemitweaks.mixin.emi;

import com.carjem.sampackemitweaks.pristine.PristineEmiIndex;
import dev.emi.emi.registry.EmiStackList;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records EMI's index into {@link PristineEmiIndex} just before bake() applies resource-pack index
 * data: after the plugin-invalidated stacks are dropped, at the first read of EmiData.stackData.
 */
@Mixin(value = EmiStackList.class, remap = false)
public class EmiStackListPristineIndexMixin {

    @Inject(
            method = "bake",
            at = @At(value = "FIELD", target = "Ldev/emi/emi/data/EmiData;stackData:Ljava/util/List;",
                    opcode = Opcodes.GETSTATIC, ordinal = 0))
    private static void sampack_emitweaks$recordPristineIndex(CallbackInfo ci) {
        PristineEmiIndex.record(EmiStackList.stacks);
    }
}
