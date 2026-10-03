package com.carjem.sampackemitweaks.mixin.itemgroups;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.carjem.sampackemitweaks.creative.CreativeLayout;
import com.carjem.sampackemitweaks.itemgroups.Group;
import com.carjem.sampackemitweaks.itemgroups.InventoryItemGroups;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> {

    @Shadow @Final protected T menu;
    @Shadow protected Slot hoveredSlot;

    @Unique private static boolean sampack_emitweaks$onScreen(int index) {
        return index < CreativeLayout.get().gridSize();
    }

    @Unique
    private ResourceLocation sampack_emitweaks$getSprite(String location) {
        String id = InventoryItemGroups.NAMESPACE;
        String path = "container/" + location;
        return ResourceLocation.fromNamespaceAndPath(id, path);
    }

    @Unique
    private void sampack_emitweaks$renderSprite(GuiGraphics graphics, String location, int x, int y, int size) {
        RenderSystem.disableDepthTest();
        graphics.blitSprite(sampack_emitweaks$getSprite(location), x, y, size, size);
        RenderSystem.enableDepthTest();
    }

    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void sampack_emitweaks$renderSlotSprites(CallbackInfo ci, @Local(argsOnly = true) GuiGraphics graphics, @Local(argsOnly = true) Slot slot) {
        if (!InventoryItemGroups.hasGroups() || !sampack_emitweaks$onScreen(slot.index)) return;

        int index = InventoryItemGroups.calculateIndex(menu.slots, slot.index);
        Group group = InventoryItemGroups.findGroupByIndex(index);
        if (group != null && group.isVisibility() && InventoryItemGroups.selectedTab.equals(group.getTab())) {
            if (group.getIconIndex() == index)
                sampack_emitweaks$renderSprite(graphics, "icon_slot", slot.x-1, slot.y-1, 18);
            else
                sampack_emitweaks$renderSprite(graphics, "item_slot", slot.x-1, slot.y-1, 18);
        }
    }

    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void sampack_emitweaks$renderVisibilitySprites(CallbackInfo ci, @Local(argsOnly = true) GuiGraphics graphics, @Local(argsOnly = true) Slot slot) {
        if (!InventoryItemGroups.hasGroups() || !sampack_emitweaks$onScreen(slot.index)) return;

        int index = InventoryItemGroups.calculateIndex(menu.slots, slot.index);
        Group group = InventoryItemGroups.findGroupByIndex(index);
        if (group != null && InventoryItemGroups.selectedTab.equals(group.getTab()) && group.getIconIndex() == index) {
            if (group.isVisibility())
                sampack_emitweaks$renderSprite(graphics, "minus", slot.x, slot.y, 16);
            else
                sampack_emitweaks$renderSprite(graphics, "plus", slot.x, slot.y, 16);
        }
    }

    @WrapOperation(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;getTooltipFromContainerItem(Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;"))
    private List<Component> sampack_emitweaks$renderGroupName(AbstractContainerScreen instance, ItemStack itemStack, Operation<List<Component>> original) {
        int index = InventoryItemGroups.calculateIndex(menu.slots, hoveredSlot.index);
        Group group = InventoryItemGroups.findGroupByIndex(index);

        return (group != null && InventoryItemGroups.selectedTab.equals(group.getTab()) && index == group.getIconIndex() && sampack_emitweaks$onScreen(hoveredSlot.index))
                ? List.of(group.getName())
                : original.call(instance, itemStack);
    }
}
