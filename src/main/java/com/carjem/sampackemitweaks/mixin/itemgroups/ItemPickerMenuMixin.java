package com.carjem.sampackemitweaks.mixin.itemgroups;

import com.carjem.sampackemitweaks.creative.CreativeGrid;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells {@link CreativeGrid} which row is at the top, so it can find each slot's list position. */
@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public abstract class ItemPickerMenuMixin {
    @Shadow protected abstract int getRowIndexForScroll(float scrollOffs);

    @Inject(method = "scrollTo", at = @At("HEAD"))
    private void sampack_emitweaks$recordTopRow(float pos, CallbackInfo ci) {
        CreativeGrid.setFirstRow(getRowIndexForScroll(pos));
    }
}
