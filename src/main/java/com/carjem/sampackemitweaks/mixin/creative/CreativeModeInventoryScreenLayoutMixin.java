package com.carjem.sampackemitweaks.mixin.creative;

import com.carjem.sampackemitweaks.creative.CreativeLayout;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Sizes the creative screen to {@link CreativeLayout}: panel size, background, scrollbar,
 * search box, hotbar-slot click mapping, inventory-tab contents, and tabs per page. Each hook
 * swaps one constant or argument, so the vanilla methods (and other mods' hooks on them) still
 * run. With the vanilla 9x5 layout every hook returns the vanilla value.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenLayoutMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private CreativeTabsScreenPage currentPage;

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

    @ModifyConstant(method = {"renderBg", "insideScrollbar"}, constant = @Constant(intValue = 175))
    private int sampack_emitweaks$scrollbarX(int x) {
        return x + CreativeLayout.get().extraWidth();
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
        if (layout.isVanilla()) {
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
        if (CreativeLayout.get().isVanilla()) return;

        for (CreativeModeTab tab : currentPage.getVisibleTabs()) {
            CreativeModeTabAccessor accessor = (CreativeModeTabAccessor) tab;
            accessor.sampack_emitweaks$setRow(currentPage.isTop(tab) ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM);
            accessor.sampack_emitweaks$setColumn(currentPage.getColumn(tab));
        }
    }

    /**
     * Tab sprites 1 and 7 are drawn flush with the panel's left and right edges, 2-6 are the
     * same middle tab. Vanilla indexes them by column (clamped past the end of the array, so a
     * column-7 tab would crash); with more than 5 tabs per row, give every regular tab past the
     * first a middle sprite.
     */
    @ModifyExpressionValue(method = "renderTabButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"))
    private int sampack_emitweaks$tabSprite(int index, @Local(argsOnly = true) CreativeModeTab tab) {
        if (CreativeLayout.get().isVanilla()) return index;
        return tab.isAlignedRight() ? Math.min(index, 6) : Math.min(index, 5);
    }
}
