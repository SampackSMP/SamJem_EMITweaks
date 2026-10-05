package com.carjem.sampackemitweaks.mixin.creative;

import com.carjem.sampackemitweaks.creative.CreativeLayout;
import com.carjem.sampackemitweaks.mixin.itemgroups.ItemPickerMenuAccessor;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Sizes the creative screen to {@link CreativeLayout}: panel size, background, scrollbar,
 * search box, hotbar-slot click mapping, inventory-tab contents, and tabs per page, and narrows
 * the regular tabs so more fit on a page. Each hook swaps one constant or argument, so the vanilla
 * methods (and other mods' hooks on them) still run. With the vanilla 9x5 layout every hook but
 * the tab ones returns the vanilla value.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenLayoutMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private CreativeTabsScreenPage currentPage;
    @Shadow @Final private List<CreativeTabsScreenPage> pages;
    @Shadow @Final private boolean displayOperatorCreativeTab;
    @Shadow private float scrollOffs;
    @Shadow @Final private static ResourceLocation[] UNSELECTED_TOP_TABS;
    @Shadow @Final private static ResourceLocation[] SELECTED_TOP_TABS;
    @Shadow @Final private static ResourceLocation[] UNSELECTED_BOTTOM_TABS;
    @Shadow @Final private static ResourceLocation[] SELECTED_BOTTOM_TABS;

    protected CreativeModeInventoryScreenLayoutMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** The shared item container must hold the largest grid the config allows. */
    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 45))
    private static int sampack_emitweaks$containerSize(int size) {
        return CreativeLayout.MAX_COLUMNS * CreativeLayout.MAX_ROWS;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void sampack_emitweaks$resize(CallbackInfo ci) {
        CreativeLayout layout = CreativeLayout.get();
        imageWidth = layout.width();
        imageHeight = layout.height();
    }

    /** {@code 45 + hotbarSlot} is the hotbar's menu slot index, i.e. it follows the grid. */
    @ModifyConstant(method = "slotClicked", constant = @Constant(intValue = 45))
    private int sampack_emitweaks$hotbarSlotOffset(int offset) {
        return CreativeLayout.get().gridSize();
    }

    @ModifyConstant(method = "init", constant = @Constant(intValue = 10))
    private int sampack_emitweaks$tabsPerPage(int tabs) {
        return CreativeLayout.get().tabsPerPage();
    }

    /**
     * NeoForge only turns the page with its arrow buttons, so a tab selected some other way
     * (REMI's sidebar, a hotkey) could be on a page that isn't shown. Turn to its page.
     */
    @Inject(method = "selectTab", at = @At("HEAD"))
    private void sampack_emitweaks$showTabPage(CreativeModeTab tab, CallbackInfo ci) {
        if (currentPage.getVisibleTabs().contains(tab)) return;
        for (CreativeTabsScreenPage page : pages) {
            if (page.getVisibleTabs().contains(tab)) {
                currentPage = page;
                return;
            }
        }
    }

    /**
     * The grid's slots are fixed when the menu is built, so when a resize calls for a different
     * number of columns or rows, reopen the screen with a new menu instead. The selected tab is
     * static and the search lives in EMI, so both carry over; the scroll position is kept by
     * its first item.
     */
    @Inject(method = "resize", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$relayout(Minecraft minecraft, int width, int height, CallbackInfo ci) {
        CreativeLayout layout = CreativeLayout.get();
        if (minecraft.player == null || layout.sameSize(CreativeLayout.compute())) return;

        int firstItem = ((ItemPickerMenuAccessor) menu).sampack_emitweaks$getRowIndexForScroll(scrollOffs) * layout.columns;
        CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(
                minecraft.player, minecraft.player.connection.enabledFeatures(), displayOperatorCreativeTab);
        minecraft.setScreen(screen);
        if (minecraft.screen == screen) {
            CreativeModeInventoryScreenLayoutMixin mixin = (CreativeModeInventoryScreenLayoutMixin) (Object) screen;
            mixin.scrollOffs = ((ItemPickerMenuAccessor) mixin.menu).sampack_emitweaks$getScrollForRowIndex(firstItem / CreativeLayout.get().columns);
            mixin.menu.scrollTo(mixin.scrollOffs);
        }
        ci.cancel();
    }

    @ModifyConstant(method = {"renderBg", "insideScrollbar"}, constant = @Constant(intValue = 175))
    private int sampack_emitweaks$scrollbarX(int x) {
        return x + CreativeLayout.get().scrollbarShift();
    }

    @ModifyConstant(method = {"renderBg", "insideScrollbar", "mouseDragged"}, constant = @Constant(intValue = 112))
    private int sampack_emitweaks$scrollbarLength(int length) {
        return length + CreativeLayout.get().extraHeight();
    }

    @ModifyArg(method = "selectTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setX(I)V"))
    private int sampack_emitweaks$searchBoxX(int x) {
        return x + CreativeLayout.get().extraWidth();
    }

    // The inventory tab's contents (slots, player model, destroy slot) are left at their vanilla
    // top-left positions so widgets other mods anchor to them (e.g. curios) stay aligned.

    /**
     * Saved hotbars are 9 items per row; pad each to a full grid row so they don't wrap into
     * each other.
     */
    @Inject(method = "selectTab", at = @At("TAIL"))
    private void sampack_emitweaks$padSavedHotbars(CreativeModeTab tab, CallbackInfo ci) {
        CreativeLayout layout = CreativeLayout.get();
        if (layout.isVanilla() || tab.getType() != CreativeModeTab.Type.HOTBAR) return;

        NonNullList<ItemStack> items = menu.items;
        List<ItemStack> padded = new ArrayList<>(items.size() / 9 * layout.columns);
        for (int i = 0; i < items.size(); i++) {
            padded.add(items.get(i));
            if (i % 9 == 8) {
                for (int pad = 9; pad < layout.columns; pad++)
                    padded.add(ItemStack.EMPTY);
            }
        }
        items.clear();
        items.addAll(padded);
        menu.scrollTo(0.0F);
    }

    @WrapOperation(method = "renderBg", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V", ordinal = 0))
    private void sampack_emitweaks$renderBackground(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        CreativeLayout layout = CreativeLayout.get();
        if (layout.hasVanillaPanel()) {
            original.call(graphics, texture, x, y, u, v, width, height);
        } else {
            layout.blitBackground(graphics, texture, x, y, selectedTab.getType() == CreativeModeTab.Type.INVENTORY);
        }
    }

    /**
     * NeoForge places tabs by their position on the page, but some mods read the tab's own
     * {@code row()}/{@code column()} instead, such as owo to pick tab textures. Set them from the
     * page right before the tabs are drawn so both agree.
     */
    @Inject(method = "renderBg", at = @At("HEAD"))
    private void sampack_emitweaks$syncTabPositions(CallbackInfo ci) {
        for (CreativeModeTab tab : currentPage.getVisibleTabs()) {
            CreativeModeTabAccessor accessor = (CreativeModeTabAccessor) tab;
            accessor.sampack_emitweaks$setRow(currentPage.isTop(tab) ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM);
            accessor.sampack_emitweaks$setColumn(currentPage.getColumn(tab));
        }
    }

    /**
     * Tab sprites 1 and 7 are drawn flush with the panel's left and right edges, 2-6 are the
     * same middle tab. Vanilla indexes them by column (clamped past the end of the array, so a
     * column-7 tab would crash); give the first column sprite 1, the last spot in the row (which
     * ends at the right edge) sprite 7, and the rest a middle sprite.
     */
    @ModifyExpressionValue(method = "renderTabButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"))
    private int sampack_emitweaks$tabSprite(int index, @Local(argsOnly = true) CreativeModeTab tab) {
        return CreativeLayout.get().isLastTabColumn(currentPage.getColumn(tab)) ? 6 : Math.min(index, 5);
    }

    // All tabs are the same size and sit side by side, packed across the panel (see
    // CreativeLayout#tabX). Vanilla draws them 26px wide, 27px apart.

    @ModifyReturnValue(method = "getTabX", at = @At("RETURN"))
    private int sampack_emitweaks$tabX(int x, @Local(argsOnly = true) CreativeModeTab tab) {
        return tab.isAlignedRight()
                ? CreativeLayout.get().sideTabX(sampack_emitweaks$isLeftSide(tab))
                : CreativeLayout.get().tabX(currentPage.getColumn(tab));
    }

    // The tabs vanilla aligns right (search, inventory, hotbar, op) are side tabs instead, a pair
    // at the bottom of each side, so the top and bottom rows hold only item tabs. They are drawn,
    // clicked and hovered here; vanilla's code for those only handles top and bottom.

    /** Search and inventory go on the left, hotbar and op on the right. */
    @Unique
    private static boolean sampack_emitweaks$isLeftSide(CreativeModeTab tab) {
        return tab.getType() == CreativeModeTab.Type.SEARCH || tab.getType() == CreativeModeTab.Type.INVENTORY;
    }

    /** Top to bottom on a side: search above inventory, hotbar above the rest (op). */
    @Unique
    private static int sampack_emitweaks$sideRank(CreativeModeTab tab) {
        return switch (tab.getType()) {
            case SEARCH, HOTBAR -> 0;
            default -> 1;
        };
    }

    /** The tab's place, from the top, among the tabs on its side of this page. */
    @Unique
    private int sampack_emitweaks$sideIndex(CreativeModeTab tab) {
        boolean left = sampack_emitweaks$isLeftSide(tab);
        int rank = sampack_emitweaks$sideRank(tab);
        int index = 0;
        for (CreativeModeTab other : currentPage.getVisibleTabs()) {
            if (other != tab && other.isAlignedRight() && sampack_emitweaks$isLeftSide(other) == left
                    && sampack_emitweaks$sideRank(other) <= rank) index++;
        }
        return index;
    }

    /** How many tabs this page shows on the tab's side (the op tab only shows for operators). */
    @Unique
    private int sampack_emitweaks$sideCount(CreativeModeTab tab) {
        boolean left = sampack_emitweaks$isLeftSide(tab);
        int count = 0;
        for (CreativeModeTab other : currentPage.getVisibleTabs()) {
            if (other.isAlignedRight() && sampack_emitweaks$isLeftSide(other) == left) count++;
        }
        return count;
    }

    @Unique
    private boolean sampack_emitweaks$isOverSideTab(CreativeModeTab tab, double relativeX, double relativeY) {
        return CreativeLayout.get().isOverSideTab(sampack_emitweaks$isLeftSide(tab),
                sampack_emitweaks$sideIndex(tab), sampack_emitweaks$sideCount(tab), relativeX, relativeY);
    }

    @Inject(method = "renderTabButton", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$renderSideTab(GuiGraphics graphics, CreativeModeTab tab, CallbackInfo ci) {
        if (!tab.isAlignedRight()) return;
        ci.cancel();

        CreativeLayout layout = CreativeLayout.get();
        boolean left = sampack_emitweaks$isLeftSide(tab);
        boolean selected = tab == selectedTab;
        int index = sampack_emitweaks$sideIndex(tab);
        int count = sampack_emitweaks$sideCount(tab);
        ResourceLocation[] sprites = left
                ? (selected ? SELECTED_TOP_TABS : UNSELECTED_TOP_TABS)
                : (selected ? SELECTED_BOTTOM_TABS : UNSELECTED_BOTTOM_TABS);
        // A middle sprite: the side tabs sit clear of the panel's corners.
        ResourceLocation sprite = sprites[1];
        int x = leftPos + layout.sideTabX(left);
        int y = topPos + layout.sideTabY(index, count);
        layout.blitSideTab(graphics, sprite, x, y, left);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);
        ItemStack icon = tab.getIconItem();
        int iconX = x + layout.sideTabIconX(left);
        int iconY = y + layout.sideTabIconY();
        graphics.renderItem(icon, iconX, iconY);
        graphics.renderItemDecorations(font, icon, iconX, iconY);
        graphics.pose().popPose();
    }

    @Inject(method = "checkTabClicked", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$clickSideTab(CreativeModeTab tab, double relativeMouseX, double relativeMouseY, CallbackInfoReturnable<Boolean> cir) {
        if (tab.isAlignedRight()) {
            cir.setReturnValue(sampack_emitweaks$isOverSideTab(tab, relativeMouseX, relativeMouseY));
        }
    }

    @Inject(method = "checkTabHovering", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$hoverSideTab(GuiGraphics graphics, CreativeModeTab tab, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (!tab.isAlignedRight()) return;
        boolean over = sampack_emitweaks$isOverSideTab(tab, mouseX - leftPos, mouseY - topPos);
        if (over) {
            graphics.renderTooltip(font, tab.getDisplayName(), mouseX, mouseY);
        }
        cir.setReturnValue(over);
    }

    @ModifyConstant(method = "checkTabClicked", constant = @Constant(intValue = 26))
    private int sampack_emitweaks$tabClickWidth(int width) {
        return CreativeLayout.get().tabWidth();
    }

    /** Vanilla's hover area starts 3px into the tab and ends 2px before its right edge. */
    @ModifyConstant(method = "checkTabHovering", constant = @Constant(intValue = 21))
    private int sampack_emitweaks$tabHoverWidth(int width) {
        return CreativeLayout.get().tabWidth() - 5;
    }

    @WrapOperation(method = "renderTabButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void sampack_emitweaks$drawTab(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height,
                                           Operation<Void> original, @Local(argsOnly = true) CreativeModeTab tab) {
        CreativeLayout.get().blitTab(graphics, sprite, x, y, currentPage.isTop(tab));
    }

    // Tabs are TAB_HEIGHT_CUT shorter; top tabs keep their bottom edge on the panel, so they
    // start that much lower. Bottom tabs keep their top edge.

    @ModifyConstant(method = "renderTabButton", constant = @Constant(intValue = 28))
    private int sampack_emitweaks$topTabY(int offset) {
        return offset - CreativeLayout.TAB_HEIGHT_CUT;
    }

    @ModifyReturnValue(method = "getTabY", at = @At("RETURN"))
    private int sampack_emitweaks$tabY(int y, @Local(argsOnly = true) CreativeModeTab tab) {
        if (tab.isAlignedRight()) return CreativeLayout.get().sideTabY(sampack_emitweaks$sideIndex(tab), sampack_emitweaks$sideCount(tab));
        return currentPage.isTop(tab) ? y + CreativeLayout.TAB_HEIGHT_CUT : y;
    }

    @ModifyConstant(method = "checkTabClicked", constant = @Constant(intValue = 32))
    private int sampack_emitweaks$tabClickHeight(int height) {
        return height - CreativeLayout.TAB_HEIGHT_CUT;
    }

    @ModifyConstant(method = "checkTabHovering", constant = @Constant(intValue = 27))
    private int sampack_emitweaks$tabHoverHeight(int height) {
        return height - CreativeLayout.TAB_HEIGHT_CUT;
    }

    @ModifyArg(method = "renderTabButton", index = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V"))
    private int sampack_emitweaks$tabIconY(int y) {
        return y + CreativeLayout.TAB_ICON_SHIFT_Y;
    }

    @ModifyArg(method = "renderTabButton", index = 3, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))
    private int sampack_emitweaks$tabIconDecorationsY(int y) {
        return y + CreativeLayout.TAB_ICON_SHIFT_Y;
    }

    /** Centers the 16px icon between the tab's left border and the next tab (vanilla: 5px in, with an iinc). */
    @ModifyArg(method = "renderTabButton", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V"))
    private int sampack_emitweaks$tabIconX(int x) {
        return x + sampack_emitweaks$iconShift();
    }

    @ModifyArg(method = "renderTabButton", index = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))
    private int sampack_emitweaks$tabIconDecorationsX(int x) {
        return x + sampack_emitweaks$iconShift();
    }

    @Unique
    private static int sampack_emitweaks$iconShift() {
        return CreativeLayout.get().tabIconShiftX();
    }
}
