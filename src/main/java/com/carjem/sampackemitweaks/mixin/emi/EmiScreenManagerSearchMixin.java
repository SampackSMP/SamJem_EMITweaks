package com.carjem.sampackemitweaks.mixin.emi;

import com.carjem.sampackemitweaks.search.EmiSearchBar;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The search bar's buttons and history list ({@link EmiSearchBar}) get input before anything else
 * EMI shows, and before the screen: EMI routes raw mouse and key input here ahead of the screen's
 * own handlers, and returning true keeps it from the screen. The list is drawn over everything EMI
 * draws.
 */
@Mixin(value = EmiScreenManager.class, remap = false)
public class EmiScreenManagerSearchMixin {
    @Inject(method = "drawForeground", at = @At("HEAD"))
    private static void sampack_emitweaks$renderHistory(EmiDrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EmiSearchBar.renderList(context.raw());
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private static void sampack_emitweaks$click(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (EmiSearchBar.mouseClicked(mouseX, mouseY, button)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private static void sampack_emitweaks$release(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (EmiSearchBar.mouseReleased(mouseX, mouseY)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private static void sampack_emitweaks$scroll(double mouseX, double mouseY, double amount, CallbackInfoReturnable<Boolean> cir) {
        if (EmiSearchBar.mouseScrolled(mouseX, mouseY, amount)) cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private static void sampack_emitweaks$key(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (EmiSearchBar.keyPressed(keyCode)) cir.setReturnValue(true);
    }
}
