package com.carjem.sampackemitweaks.mixin.compat;

import com.carjem.sampackemitweaks.compat.RemiNestedGroups;
import com.carjem.sampackemitweaks.compat.RemiNestedLayout;
import dev.emi.emi.api.stack.EmiStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Opens expanded groups in EMI's index panel with their subgroups inside them ({@link RemiNestedLayout}). */
@Mixin(targets = "com.evandev.remi.integration.emi.StackManager", remap = false)
public class RemiNestedStackManagerMixin {
    @Inject(method = "buildDisplayedStacks", at = @At("HEAD"), cancellable = true)
    private static void sampack_emitweaks$nestedDisplayedStacks(List<EmiStack> grouped, CallbackInfoReturnable<List<EmiStack>> cir) {
        if (RemiNestedGroups.isActive()) cir.setReturnValue(RemiNestedLayout.displayedStacks(grouped));
    }
}
