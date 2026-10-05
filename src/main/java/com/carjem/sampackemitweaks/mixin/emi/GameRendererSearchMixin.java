package com.carjem.sampackemitweaks.mixin.emi;

import com.carjem.sampackemitweaks.search.SearchOverlay;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Draws the screen as if the mouse were nowhere while it is over the open search history list
 * ({@link SearchOverlay}), so slots, EMI's stacks and REMI's tabs under the list neither highlight
 * nor show tooltips. NeoForge does the same for the layers under the top screen.
 */
@Mixin(GameRenderer.class)
public class GameRendererSearchMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/ClientHooks;drawScreen(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void sampack_emitweaks$hideMouseUnderHistory(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                                                         Operation<Void> original) {
        if (SearchOverlay.hidesMouse(mouseX, mouseY)) {
            mouseX = Integer.MAX_VALUE;
            mouseY = Integer.MAX_VALUE;
        }
        original.call(screen, graphics, mouseX, mouseY, partialTick);
    }
}
