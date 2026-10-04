package com.carjem.sampackemitweaks.mixin.itemgroups;

import com.carjem.sampackemitweaks.creative.CreativeGrid;
import com.carjem.sampackemitweaks.itemgroups.InventoryItemGroups;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Draws the creative grid's groups: an open group's items on a highlighted slot, its header on
 * another, and a plus or minus over every header. A header's tooltip is the group's name. Only
 * the creative item grid has list positions ({@link CreativeGrid#position}), so other screens
 * are untouched.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Unique private static final ResourceLocation sampack_emitweaks$ICON_SLOT = sampack_emitweaks$sprite("icon_slot");
    @Unique private static final ResourceLocation sampack_emitweaks$ITEM_SLOT = sampack_emitweaks$sprite("item_slot");
    @Unique private static final ResourceLocation sampack_emitweaks$PLUS = sampack_emitweaks$sprite("plus");
    @Unique private static final ResourceLocation sampack_emitweaks$MINUS = sampack_emitweaks$sprite("minus");

    @Shadow protected Slot hoveredSlot;

    @Unique
    private static ResourceLocation sampack_emitweaks$sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(InventoryItemGroups.NAMESPACE, "container/" + name);
    }

    @Unique
    private static void sampack_emitweaks$blit(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int size) {
        RenderSystem.disableDepthTest();
        graphics.blitSprite(sprite, x, y, size, size);
        RenderSystem.enableDepthTest();
    }

    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void sampack_emitweaks$renderGroupSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        int position = CreativeGrid.position(slot);
        if (CreativeGrid.isInExpandedGroup(position)) {
            sampack_emitweaks$blit(graphics, CreativeGrid.isHeader(position) ? sampack_emitweaks$ICON_SLOT : sampack_emitweaks$ITEM_SLOT,
                    slot.x - 1, slot.y - 1, 18);
        }
    }

    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void sampack_emitweaks$renderGroupToggle(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        int position = CreativeGrid.position(slot);
        if (CreativeGrid.isHeader(position)) {
            sampack_emitweaks$blit(graphics, CreativeGrid.isExpanded(position) ? sampack_emitweaks$MINUS : sampack_emitweaks$PLUS,
                    slot.x, slot.y, 16);
        }
    }

    @WrapOperation(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;getTooltipFromContainerItem(Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;"))
    private List<Component> sampack_emitweaks$groupTooltip(AbstractContainerScreen<?> screen, ItemStack stack, Operation<List<Component>> original) {
        int position = CreativeGrid.position(hoveredSlot);
        return CreativeGrid.isHeader(position) ? CreativeGrid.tooltip(position) : original.call(screen, stack);
    }
}
