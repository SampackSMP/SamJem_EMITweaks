package com.carjem.sampackemitweaks.mixin.compat;

import com.carjem.sampackemitweaks.client.ClientConfig;
import com.carjem.sampackemitweaks.compat.RemiGroupIcons;
import com.carjem.sampackemitweaks.creative.GroupIcon;
import com.evandev.remi.feature.stackgroup.EmiGroupStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws a REMI stack group with the icon its json names ({@link RemiGroupIcons}), or the
 * configured default, in place of REMI's fanned-out first three items, keeping REMI's expanded background and its indicator.
 * Such a group skips EMI's batched rendering, which only knows REMI's own drawing.
 */
@Mixin(targets = "com.evandev.remi.feature.stackgroup.EmiGroupStack", remap = false)
public class RemiGroupStackIconMixin {
    @Shadow @Final private static ResourceLocation EXPANDED_TEXTURE;
    @Shadow @Final private static ResourceLocation EXPANDED_INDICATOR_TEXTURE;
    @Shadow @Final private static ResourceLocation COLLAPSED_INDICATOR_TEXTURE;

    @Unique
    private GroupIcon sampack_emitweaks$icon() {
        GroupIcon icon = RemiGroupIcons.get(((EmiGroupStack) (Object) this).group);
        if (icon == null) icon = ClientConfig.get(ClientConfig.REMI_GROUP_ICON).icon();
        return icon.kind == GroupIcon.Kind.STACKED ? null : icon;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$renderIcon(GuiGraphics graphics, int x, int y, float delta, int flags, CallbackInfo ci) {
        GroupIcon icon = sampack_emitweaks$icon();
        if (icon == null) return;
        ci.cancel();

        EmiGroupStack self = (EmiGroupStack) (Object) this;
        if (self.isExpanded) {
            RenderSystem.enableBlend();
            graphics.blit(EXPANDED_TEXTURE, x - 1, y - 1, 0, 0, 0, 18, 18, 18, 18);
            RenderSystem.disableBlend();
        }
        if (icon.kind == GroupIcon.Kind.FIRST) {
            var items = self.getItems();
            if (!items.isEmpty()) items.getFirst().render(graphics, x, y, delta, flags);
        } else {
            icon.render(graphics, x, y);
        }
        RenderSystem.enableBlend();
        graphics.blit(self.isExpanded ? EXPANDED_INDICATOR_TEXTURE : COLLAPSED_INDICATOR_TEXTURE, x - 1, y - 1, 200, 0, 0, 18, 18, 18, 18);
        RenderSystem.disableBlend();
    }

    @Inject(method = "isUnbatchable", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$unbatchableWithIcon(CallbackInfoReturnable<Boolean> cir) {
        if (sampack_emitweaks$icon() != null) cir.setReturnValue(true);
    }
}
