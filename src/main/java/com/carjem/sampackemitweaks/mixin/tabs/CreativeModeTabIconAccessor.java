package com.carjem.sampackemitweaks.mixin.tabs;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Clears a tab's cached icon, so the next getIconItem() asks its icon generator again. */
@Mixin(CreativeModeTab.class)
public interface CreativeModeTabIconAccessor {
    @Accessor("iconItemStack")
    void sampack_emitweaks$setIconItemStack(ItemStack stack);
}
