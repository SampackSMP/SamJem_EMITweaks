package com.carjem.sampackemitweaks.mixin.creative;

import com.carjem.sampackemitweaks.creative.CreativeLayout;
import com.carjem.sampackemitweaks.mixin.itemgroups.ItemPickerMenuAccessor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Keeps the open tab, tab page and scroll position when EMI's recipe screen goes back to the
 * creative screen. EMI reopens the same screen, and its {@code init} turns to the selected tab's
 * page and reselects the tab, which scrolls to the top. Both are put back as they were when the
 * screen was left.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenStateMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private CreativeTabsScreenPage currentPage;
    @Shadow @Final private List<CreativeTabsScreenPage> pages;
    @Shadow private float scrollOffs;

    @Shadow
    private void selectTab(CreativeModeTab tab) {
        throw new AssertionError();
    }

    @Unique private CreativeModeTab sampack_emitweaks$savedTab;
    @Unique private int sampack_emitweaks$savedPage;
    @Unique private int sampack_emitweaks$savedFirstItem;

    protected CreativeModeInventoryScreenStateMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void sampack_emitweaks$saveState(CallbackInfo ci) {
        sampack_emitweaks$savedTab = selectedTab;
        sampack_emitweaks$savedPage = pages.indexOf(currentPage);
        sampack_emitweaks$savedFirstItem = ((ItemPickerMenuAccessor) menu).sampack_emitweaks$getRowIndexForScroll(scrollOffs)
                * CreativeLayout.get().columns;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sampack_emitweaks$restoreState(CallbackInfo ci) {
        CreativeModeTab tab = sampack_emitweaks$savedTab;
        sampack_emitweaks$savedTab = null;
        if (tab == null || minecraft.screen != (Object) this) return;

        if (selectedTab != tab) {
            if (!tab.shouldDisplay() || pages.stream().noneMatch(page -> page.getVisibleTabs().contains(tab))) return;
            selectTab(tab);
        }
        if (sampack_emitweaks$savedPage >= 0 && sampack_emitweaks$savedPage < pages.size()) {
            currentPage = pages.get(sampack_emitweaks$savedPage);
        }
        scrollOffs = ((ItemPickerMenuAccessor) menu).sampack_emitweaks$getScrollForRowIndex(
                sampack_emitweaks$savedFirstItem / CreativeLayout.get().columns);
        menu.scrollTo(scrollOffs);
    }
}
