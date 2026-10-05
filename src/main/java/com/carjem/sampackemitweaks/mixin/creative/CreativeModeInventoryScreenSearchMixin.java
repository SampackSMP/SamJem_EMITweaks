package com.carjem.sampackemitweaks.mixin.creative;

import com.carjem.sampackemitweaks.client.ClientConfig;
import com.carjem.sampackemitweaks.creative.CreativeContents;
import com.carjem.sampackemitweaks.mixin.itemgroups.ItemPickerMenuAccessor;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Fills the creative grid from {@link CreativeContents}. The item tabs are filled where vanilla
 * reads their contents ({@link com.carjem.sampackemitweaks.mixin.itemgroups.CreativeModeInventoryScreenMixin});
 * this fills the search tab with EMI's index and refills the grid whenever EMI's search, EMI's
 * index or the groups change.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenSearchMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private float scrollOffs;

    @Shadow
    private void selectTab(CreativeModeTab tab) {
        throw new AssertionError();
    }

    protected CreativeModeInventoryScreenSearchMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** The tabs {@link CreativeContents} fills: item tabs and the search tab. */
    @Unique
    private static boolean sampack_emitweaks$isGridTab(CreativeModeTab tab) {
        return tab.getType() == CreativeModeTab.Type.CATEGORY || tab.getType() == CreativeModeTab.Type.SEARCH;
    }

    @Unique private boolean sampack_emitweaks$focusSearch;
    // The tab this screen last showed; null until init selects the first one.
    @Unique private CreativeModeTab sampack_emitweaks$shownTab;

    /**
     * Switching to another tab clears EMI's search bar, if the config says so. Init's own
     * selectTab, and the refills below (which reselect the same tab), don't count.
     */
    @Inject(method = "selectTab", at = @At("HEAD"))
    private void sampack_emitweaks$clearSearchOnSwitch(CreativeModeTab tab, CallbackInfo ci) {
        if (sampack_emitweaks$shownTab != null && tab != sampack_emitweaks$shownTab
                && ClientConfig.get(ClientConfig.CLEAR_SEARCH_ON_TAB_SWITCH)) {
            CreativeContents.clearEmiSearch();
        }
        sampack_emitweaks$shownTab = tab;
    }

    /**
     * Opening the search tab focuses EMI's search bar, as vanilla does its own search box. EMI
     * sets up its widgets after the screen's init, which selects the tab, so wait a tick.
     */
    @Inject(method = "selectTab", at = @At("HEAD"))
    private void sampack_emitweaks$focusSearchOnOpen(CreativeModeTab tab, CallbackInfo ci) {
        if (tab != selectedTab && tab.getType() == CreativeModeTab.Type.SEARCH
                && ClientConfig.get(ClientConfig.CREATIVE_FOCUS_SEARCH)) {
            sampack_emitweaks$focusSearch = true;
        }
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void sampack_emitweaks$focusSearch(CallbackInfo ci) {
        if (!sampack_emitweaks$focusSearch) return;
        sampack_emitweaks$focusSearch = false;
        if (selectedTab.getType() == CreativeModeTab.Type.SEARCH) {
            CreativeContents.focusEmiSearch(this);
        }
    }

    /** Without a search bar, vanilla leaves the search tab empty; show EMI's index there. */
    @Inject(method = "selectTab", at = @At("TAIL"))
    private void sampack_emitweaks$fillSearchTab(CreativeModeTab tab, CallbackInfo ci) {
        if (tab.getType() == CreativeModeTab.Type.SEARCH) {
            menu.items.addAll(CreativeContents.indexItems());
            menu.scrollTo(0.0F);
        }
        CreativeContents.markApplied();
    }

    /** EMI searches on its own thread; pick up a new query, or a reloaded index, on the next tick. */
    @Inject(method = "containerTick", at = @At("TAIL"))
    private void sampack_emitweaks$followEmiSearch(CallbackInfo ci) {
        if (sampack_emitweaks$isGridTab(selectedTab) && CreativeContents.isStale()) {
            sampack_emitweaks$refill();
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
            sampack_emitweaks$refill();
            ci.cancel();
        }
    }

    /**
     * Refills the open tab. Reselecting it scrolls to the top, so if it shows the same items as
     * before, scroll back to where it was. EMI's results are often rebuilt unchanged, e.g. when
     * its recipe screen goes back to this one.
     */
    @Unique
    private void sampack_emitweaks$refill() {
        List<ItemStack> before = new ArrayList<>(menu.items);
        float scroll = scrollOffs;
        selectTab(selectedTab);
        if (sampack_emitweaks$sameItems(before, menu.items)) {
            ItemPickerMenuAccessor accessor = (ItemPickerMenuAccessor) menu;
            scrollOffs = accessor.sampack_emitweaks$getScrollForRowIndex(accessor.sampack_emitweaks$getRowIndexForScroll(scroll));
            menu.scrollTo(scrollOffs);
        }
    }

    @Unique
    private static boolean sampack_emitweaks$sameItems(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i) && !ItemStack.isSameItemSameComponents(a.get(i), b.get(i))) return false;
        }
        return true;
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
