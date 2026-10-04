package com.carjem.sampackemitweaks.mixin.creative;

import com.carjem.sampackemitweaks.creative.CreativeContents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/**
 * Fills the creative grid from {@link CreativeContents}. The item tabs are filled where vanilla
 * reads their contents ({@link com.carjem.sampackemitweaks.mixin.itemgroups.CreativeModeInventoryScreenMixin});
 * this fills the search tab with EMI's index and refills the grid whenever EMI's search, EMI's
 * index or the groups change.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenSearchMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow private static CreativeModeTab selectedTab;

    @Shadow
    private void selectTab(CreativeModeTab tab) {
        throw new AssertionError();
    }

    protected CreativeModeInventoryScreenSearchMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** The tabs {@link CreativeContents} fills: item tabs, and with EMI the search tab. */
    @Unique
    private static boolean sampack_emitweaks$isGridTab(CreativeModeTab tab) {
        return tab.getType() == CreativeModeTab.Type.CATEGORY
                || (tab.getType() == CreativeModeTab.Type.SEARCH && CreativeContents.isEmiSearch());
    }

    /** Without a search bar, vanilla leaves the search tab empty; show EMI's index there. */
    @Inject(method = "selectTab", at = @At("TAIL"))
    private void sampack_emitweaks$fillSearchTab(CreativeModeTab tab, CallbackInfo ci) {
        if (CreativeContents.isEmiSearch() && tab.getType() == CreativeModeTab.Type.SEARCH) {
            menu.items.addAll(CreativeContents.indexItems());
            menu.scrollTo(0.0F);
        }
        CreativeContents.markApplied();
    }

    /** EMI searches on its own thread; pick up a new query, or a reloaded index, on the next tick. */
    @Inject(method = "containerTick", at = @At("TAIL"))
    private void sampack_emitweaks$followEmiSearch(CallbackInfo ci) {
        if (sampack_emitweaks$isGridTab(selectedTab) && CreativeContents.isStale()) {
            selectTab(selectedTab);
        }
    }

    /**
     * When the tabs are rebuilt, vanilla puts the open tab's raw contents back into the grid,
     * unfiltered and ungrouped, and the search tab's are the vanilla search tab's. Refill it the
     * usual way.
     */
    @Inject(method = "refreshCurrentTabContents", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$refillGrid(Collection<ItemStack> items, CallbackInfo ci) {
        if (sampack_emitweaks$isGridTab(selectedTab)) {
            selectTab(selectedTab);
            ci.cancel();
        }
    }

    /**
     * Item tooltips outside the item tabs list the tabs that hold the item, skipping tabs with a
     * search bar so the search tab isn't listed. No tab has one any more, so skip it by type.
     */
    @WrapOperation(method = "getTooltipFromContainerItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;hasSearchBar()Z"))
    private boolean sampack_emitweaks$searchTabBySearchType(CreativeModeTab tab, Operation<Boolean> original) {
        return original.call(tab) || tab.getType() == CreativeModeTab.Type.SEARCH;
    }
}
