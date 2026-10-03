package com.carjem.sampackemitweaks.mixin.compat;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets Enhanced Tooltips share vanilla's creative-tab build instead of doing
 * its own.
 *
 * On every world join, Enhanced Tooltips' CreativeModeTabCollector runs each
 * tab's display generator and BuildCreativeModeTabContentsEvent into private
 * builders, just to learn which tab each item belongs to. Opening the creative
 * inventory then makes vanilla build the exact same contents again. Each build
 * takes several seconds on the Render thread with this pack.
 *
 * This builds the tabs through CreativeModeTabs.tryRebuildTabContents with the
 * same parameters CreativeModeInventoryScreen uses, then reads each tab's
 * display items. Vanilla caches on those parameters, so the creative screen
 * finds the tabs already built and skips its rebuild. The collector skipped
 * SEARCH_TAB_ONLY entries and the hotbar and inventory tabs, and
 * getDisplayItems() leaves out the same entries.
 */
@Mixin(targets = "dev.ultimatchamp.enhancedtooltips.util.CreativeModeTabCollector", remap = false)
public class EnhancedTooltipsTabCollectorMixin {

    @Inject(method = "collectTabs", at = @At("HEAD"), cancellable = true)
    private static void sampack_emitweaks$useVanillaBuild(
            Level level,
            CallbackInfoReturnable<Map<CreativeModeTab, Collection<ItemStack>>> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        boolean hasPermissions = player.canUseGameMasterBlocks() && minecraft.options.operatorItemsTab().get();
        CreativeModeTabs.tryRebuildTabContents(player.connection.enabledFeatures(), hasPermissions, level.registryAccess());

        Map<CreativeModeTab, Collection<ItemStack>> tabs = new LinkedHashMap<>();
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() == CreativeModeTab.Type.HOTBAR || tab.getType() == CreativeModeTab.Type.INVENTORY) {
                continue;
            }
            tabs.put(tab, tab.getDisplayItems());
        }
        cir.setReturnValue(tabs);
    }
}
