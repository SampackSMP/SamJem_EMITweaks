package com.carjem.sampackemitweaks.mixin.itemgroups;

import com.carjem.sampackemitweaks.itemgroups.Group;
import com.carjem.sampackemitweaks.itemgroups.InventoryItemGroups;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public abstract class ItemPickerMenuMixin {

    @Shadow @Final public NonNullList<@NotNull ItemStack> items;

    @Shadow public abstract void scrollTo(float scrollOffs);
    @Shadow protected abstract float getScrollForRowIndex(int rowIndex);

    @Inject(method = "getCarried", at = @At("HEAD"))
    private void sampack_emitweaks$toggleGroupVisibility(CallbackInfoReturnable<ItemStack> cir) {
        Group group = InventoryItemGroups.pendingGroup;
        if (group != null) {
            group.setVisibility(!group.isVisibility());

            int topRow = sampack_emitweaks$currentTopRow();
            int insertIndex = group.getIconIndex() + 1;

            List<ItemStack> groupItems = group.getItems();
            if (group.isVisibility()) {
                for (int i = groupItems.size() - 1; i >= 0; i--)
                    items.add(insertIndex, groupItems.get(i));
            }
            else
                groupItems.forEach(ignore -> items.remove(insertIndex));

            InventoryItemGroups.pendingGroup = null;
            scrollTo(getScrollForRowIndex(topRow));
            InventoryItemGroups.tempItemStacks = new ArrayList<>(items);
            InventoryItemGroups.setIndexes();
        }
    }

    @Unique
    private int sampack_emitweaks$currentTopRow() {
        List<Slot> slots = ((AbstractContainerMenu) (Object) this).slots;
        return Math.max(InventoryItemGroups.calculateIndex(slots, 0), 0) / 9;
    }
}
