package com.carjem.sampackemitweaks.mixin.compat;

import com.carjem.sampackemitweaks.compat.RemiNestedGroups;
import com.carjem.sampackemitweaks.compat.RemiNestedLayout;
import com.evandev.remi.feature.stackgroup.data.StackGroup;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.config.SidebarType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws an expanded group's background under its subgroups' closed headers too, and an expanded
 * subgroup's as its own region inside it ({@link RemiNestedLayout#backgroundGroup}).
 */
@Mixin(targets = "com.evandev.remi.integration.emi.Layout", remap = false)
public class RemiNestedLayoutMixin {
    @Inject(method = "getGroup", at = @At("HEAD"), cancellable = true)
    private static void sampack_emitweaks$nestedGroup(EmiStack stack, SidebarType type, CallbackInfoReturnable<StackGroup> cir) {
        if (RemiNestedGroups.isActive()) cir.setReturnValue(RemiNestedLayout.backgroundGroup(stack, type));
    }
}
