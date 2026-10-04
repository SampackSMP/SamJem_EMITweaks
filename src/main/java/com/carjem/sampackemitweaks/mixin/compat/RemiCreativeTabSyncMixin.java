package com.carjem.sampackemitweaks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Always keeps REMI's creative tab sidebar on the same tab as the creative inventory, both ways,
 * whatever REMI's syncSelectedCreativeModeTab says. Both checks of it only run while the creative
 * inventory is open.
 *
 * The creative grid shows the results of EMI's search bar (see
 * {@link com.carjem.sampackemitweaks.creative.CreativeContents}), and with REMI that search runs
 * over the sidebar's selected tab. On different tabs, EMI's panel and the grid would search
 * different items.
 */
@Mixin(targets = "com.evandev.remi.feature.creativemodetab.CreativeModeTabManager", remap = false)
public class RemiCreativeTabSyncMixin {
    @ModifyExpressionValue(
            method = {"onTabSelected", "onCreativeModeInventoryScreenTabSelected"},
            at = @At(value = "FIELD", target = "Lcom/evandev/remi/config/ReliableEmiConfig;syncSelectedCreativeModeTab:Z"))
    private static boolean sampack_emitweaks$alwaysSync(boolean sync) {
        return true;
    }
}
