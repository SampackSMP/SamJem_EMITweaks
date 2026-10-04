package com.carjem.sampackemitweaks.mixin.tabs;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The parameters the tabs were last built with, and the method that builds them all. */
@Mixin(CreativeModeTabs.class)
public interface CreativeModeTabsAccessor {
    @Accessor("CACHED_PARAMETERS")
    static CreativeModeTab.ItemDisplayParameters sampack_emitweaks$getCachedParameters() {
        throw new AssertionError();
    }

    @Invoker("buildAllTabContents")
    static void sampack_emitweaks$buildAllTabContents(CreativeModeTab.ItemDisplayParameters parameters) {
        throw new AssertionError();
    }
}
