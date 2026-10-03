package com.carjem.sampackemitweaks.mixin.creative;

import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreativeModeTab.class)
public interface CreativeModeTabAccessor {
    @Mutable
    @Accessor("row")
    void sampack_emitweaks$setRow(CreativeModeTab.Row row);

    @Mutable
    @Accessor("column")
    void sampack_emitweaks$setColumn(int column);
}
