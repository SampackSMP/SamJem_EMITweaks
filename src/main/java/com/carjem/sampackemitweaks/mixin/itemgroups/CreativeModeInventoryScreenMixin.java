package com.carjem.sampackemitweaks.mixin.itemgroups;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.carjem.sampackemitweaks.itemgroups.Group;
import com.carjem.sampackemitweaks.itemgroups.InventoryItemGroups;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {

    @Shadow private static CreativeModeTab selectedTab;

    @WrapOperation(method = "selectTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;getDisplayItems()Ljava/util/Collection;"))
    private Collection<ItemStack> sampack_emitweaks$groupsImplementation(CreativeModeTab selectedTab, Operation<Collection<ItemStack>> original) {
        InventoryItemGroups.selectedTab = selectedTab;
        InventoryItemGroups.createGroups();
        return InventoryItemGroups.buildTabItems(original.call(selectedTab));
    }

    @WrapWithCondition(method = "slotClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$ItemPickerMenu;setCarried(Lnet/minecraft/world/item/ItemStack;)V"))
    private boolean sampack_emitweaks$toggleInsteadOfCarry(CreativeModeInventoryScreen.ItemPickerMenu instance, ItemStack itemStack, @Local(argsOnly = true) Slot slot) {
        if (slot == null) return true;

        int index = InventoryItemGroups.calculateIndex(instance.slots, slot.index);
        Group group = InventoryItemGroups.findGroupByIndex(index);
        if (group != null && selectedTab.equals(group.getTab()) && group.getIconIndex() == index) {
            instance.setCarried(ItemStack.EMPTY);
            InventoryItemGroups.pendingGroup = group;
            return false;
        }
        return true;
    }

    @Inject(method = "selectTab", at = @At("HEAD"))
    private void sampack_emitweaks$updateSelectedTab(CreativeModeTab tab, CallbackInfo ci) {
        InventoryItemGroups.selectedTab = tab;
    }
}
