package com.carjem.sampackemitweaks.mixin.itemgroups;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The menu's scroll conversions, to keep the top row in place when a group opens or closes. */
@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public interface ItemPickerMenuAccessor {
    @Invoker("getRowIndexForScroll")
    int sampack_emitweaks$getRowIndexForScroll(float scrollOffs);

    @Invoker("getScrollForRowIndex")
    float sampack_emitweaks$getScrollForRowIndex(int rowIndex);
}
