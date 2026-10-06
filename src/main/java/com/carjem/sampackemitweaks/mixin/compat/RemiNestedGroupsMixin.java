package com.carjem.sampackemitweaks.mixin.compat;

import com.carjem.sampackemitweaks.compat.RemiNestedGroups;
import com.carjem.sampackemitweaks.compat.RemiNestedLayout;
import com.evandev.remi.feature.stackgroup.StackGroupManager;
import com.evandev.remi.feature.stackgroup.data.StackGroup;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.config.SidebarType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Stack groups inside other stack groups ({@link RemiNestedGroups}): matches each group's
 * subgroups once REMI has loaded every group, then, while any group has some, groups the index
 * and the other sidebars, and finds a group's stacks by its name, with {@link RemiNestedLayout}.
 */
@Mixin(targets = "com.evandev.remi.feature.stackgroup.StackGroupManager", remap = false)
public class RemiNestedGroupsMixin {
    @Inject(method = "reload", at = @At("RETURN"))
    private static void sampack_emitweaks$resolveSubgroups(CallbackInfo ci) {
        RemiNestedGroups.resolve(StackGroupManager.stackGroups, group -> ((StackGroup) group).getId());
    }

    @Inject(method = "buildGroupedStacks", at = @At("HEAD"), cancellable = true)
    private static void sampack_emitweaks$nestedGroupedStacks(List<EmiStack> source, CallbackInfoReturnable<List<EmiStack>> cir) {
        if (RemiNestedGroups.isActive()) cir.setReturnValue(RemiNestedLayout.groupedStacks(source));
    }

    @Inject(method = "buildGroupedIngredients", at = @At("HEAD"), cancellable = true)
    private static void sampack_emitweaks$nestedGroupedIngredients(List<? extends EmiIngredient> source, SidebarType type,
                                                                   CallbackInfoReturnable<List<EmiIngredient>> cir) {
        if (RemiNestedGroups.isActive()) cir.setReturnValue(RemiNestedLayout.groupedIngredients(source, type));
    }

    @Inject(method = "appendStacksForMatchingGroups", at = @At("HEAD"), cancellable = true)
    private static void sampack_emitweaks$nestedMatchingGroups(String query, List<EmiStack> results, CallbackInfo ci) {
        if (!RemiNestedGroups.isActive()) return;
        RemiNestedLayout.appendStacksForMatchingGroups(query, results);
        ci.cancel();
    }
}
