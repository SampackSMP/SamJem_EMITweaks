package com.carjem.sampackemitweaks.mixin.itemgroups;

import com.carjem.sampackemitweaks.creative.CreativeContents;
import com.carjem.sampackemitweaks.creative.CreativeGrid;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/** Fills item tabs from {@link CreativeContents} and opens and closes their groups on click. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow private float scrollOffs;

    protected CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** Only item tabs are grouped; the grid lays one out again when it is filled. */
    @Inject(method = "selectTab", at = @At("HEAD"))
    private void sampack_emitweaks$clearGrid(CreativeModeTab tab, CallbackInfo ci) {
        CreativeGrid.clear();
    }

    @WrapOperation(method = "selectTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;getDisplayItems()Ljava/util/Collection;"))
    private Collection<ItemStack> sampack_emitweaks$tabContents(CreativeModeTab tab, Operation<Collection<ItemStack>> original) {
        return CreativeContents.tabItems(tab, original.call(tab));
    }

    /** A click on a group's header, with nothing carried, opens or closes the group instead. */
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$toggleGroup(@Nullable Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        int position = CreativeGrid.position(slot);
        if (!CreativeGrid.isHeader(position) || !menu.getCarried().isEmpty()
                || (type != ClickType.PICKUP && type != ClickType.QUICK_MOVE)) return;

        ItemPickerMenuAccessor accessor = (ItemPickerMenuAccessor) menu;
        int topRow = accessor.sampack_emitweaks$getRowIndexForScroll(scrollOffs);
        menu.items.clear();
        menu.items.addAll(CreativeGrid.toggle(position));
        scrollOffs = accessor.sampack_emitweaks$getScrollForRowIndex(topRow);
        menu.scrollTo(scrollOffs);
        ci.cancel();
    }
}
