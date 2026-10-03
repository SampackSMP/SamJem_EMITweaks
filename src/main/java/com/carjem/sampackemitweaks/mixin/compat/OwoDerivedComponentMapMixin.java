package com.carjem.sampackemitweaks.mixin.compat;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips owo-lib's per-ItemStack component derivation when it cannot change
 * anything.
 *
 * owo wraps every ItemStack's prototype components in a DerivedComponentMap and
 * calls derive() from the ItemStack constructor and on every component change.
 * derive() resets its patch (which copies the whole component map through
 * PatchedDataComponentMap.ensureMapOwnership), asks the item to derive
 * components, then applies the result. For an item that keeps owo's default
 * no-op deriveStackComponents, and a map with nothing derived yet, both steps
 * leave the map exactly as it was, but the copy still happens -- ~5.6 GB of
 * allocation per session in this pack, since EMI, tab builds and recipes create
 * stacks constantly.
 *
 * derive() now returns early only in that case. Items that override
 * deriveStackComponents, and maps holding an earlier derivation, run as before.
 */
@Mixin(targets = "io.wispforest.owo.ext.DerivedComponentMap", remap = false)
public class OwoDerivedComponentMapMixin {

    @Unique
    private static final ClassValue<Boolean> sampack_emitweaks$derives = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> itemClass) {
            try {
                return !itemClass
                        .getMethod("deriveStackComponents", DataComponentMap.class, DataComponentPatch.Builder.class)
                        .getDeclaringClass()
                        .getName()
                        .equals("io.wispforest.owo.ext.OwoItem");
            } catch (NoSuchMethodException e) {
                return true;
            }
        }
    };

    @Shadow
    @Final
    private PatchedDataComponentMap delegate;

    @Inject(method = "derive", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$skipNoOpDerive(ItemStack stack, CallbackInfo ci) {
        if (this.delegate.isPatchEmpty() && !sampack_emitweaks$derives.get(stack.getItem().getClass())) {
            ci.cancel();
        }
    }
}
