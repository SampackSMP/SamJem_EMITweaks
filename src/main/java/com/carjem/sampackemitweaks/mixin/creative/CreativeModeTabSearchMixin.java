package com.carjem.sampackemitweaks.mixin.creative;

import com.carjem.sampackemitweaks.creative.CreativeContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * With EMI, no creative tab has its own search box; EMI's search bar filters them all instead
 * (see {@link CreativeContents}). Without a search bar, the creative screen hides the box and
 * vanilla skips rebuilding the creative search trees, which work out every item's tooltip.
 */
@Mixin(CreativeModeTab.class)
public class CreativeModeTabSearchMixin {
    @Unique
    private static final ResourceLocation sampack_emitweaks$SEARCH_BACKGROUND = CreativeModeTab.createTextureLocation("item_search");
    @Unique
    private static final ResourceLocation sampack_emitweaks$ITEMS_BACKGROUND = CreativeModeTab.createTextureLocation("items");

    @Inject(method = "hasSearchBar", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$noSearchBar(CallbackInfoReturnable<Boolean> cir) {
        if (CreativeContents.isEmiSearch()) {
            cir.setReturnValue(false);
        }
    }

    /** The search background draws an empty search box; tabs that use it get the plain one instead. */
    @Inject(method = "getBackgroundTexture", at = @At("RETURN"), cancellable = true)
    private void sampack_emitweaks$noSearchBackground(CallbackInfoReturnable<ResourceLocation> cir) {
        if (CreativeContents.isEmiSearch() && sampack_emitweaks$SEARCH_BACKGROUND.equals(cir.getReturnValue())) {
            cir.setReturnValue(sampack_emitweaks$ITEMS_BACKGROUND);
        }
    }
}
