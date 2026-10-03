package com.carjem.sampackemitweaks.mixin.pristine;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** NeoForge's own sorted tab list, which Recreative's getSortedCreativeModeTabs() hook never touches. */
@Mixin(value = CreativeModeTabRegistry.class, remap = false)
public interface CreativeModeTabRegistryAccessor {
    @Accessor("SORTED_TABS")
    static List<CreativeModeTab> sampack_emitweaks$getSortedTabs() {
        throw new AssertionError();
    }
}
