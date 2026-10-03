package com.carjem.sampackemitweaks.mixin.creative;

import com.carjem.sampackemitweaks.creative.CreativeLayout;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Builds the creative item grid at {@link CreativeLayout}'s size instead of 9x5, and scrolls it
 * by that many columns and rows. The vanilla methods still run, with their 9/5/45 constants
 * swapped, so other mods' hooks on them (e.g. Sounds' scroll sound) keep working.
 */
@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public abstract class ItemPickerMenuLayoutMixin extends AbstractContainerMenu {
    private static final String ADD_SLOT = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$ItemPickerMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;";
    private static final String SCROLL_TO = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$ItemPickerMenu;scrollTo(F)V";

    protected ItemPickerMenuLayoutMixin(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = ADD_SLOT, ordinal = 0))
    private void sampack_emitweaks$readLayout(Player player, CallbackInfo ci) {
        CreativeLayout.refresh();
    }

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = ADD_SLOT))
    private Slot sampack_emitweaks$skipVanillaSlots(CreativeModeInventoryScreen.ItemPickerMenu menu, Slot slot, Operation<Slot> original) {
        return CreativeLayout.get().isVanilla() ? original.call(menu, slot) : slot;
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = SCROLL_TO))
    private void sampack_emitweaks$addLayoutSlots(Player player, CallbackInfo ci) {
        CreativeLayout layout = CreativeLayout.get();
        if (layout.isVanilla()) return;

        for (int row = 0; row < layout.rows; row++) {
            for (int column = 0; column < layout.columns; column++) {
                addSlot(new CreativeModeInventoryScreen.CustomCreativeSlot(CreativeModeInventoryScreen.CONTAINER,
                        row * layout.columns + column, 9 + column * 18, 18 + row * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(player.getInventory(), i, 9 + i * 18, layout.hotbarY()));
        }
    }

    @ModifyConstant(method = {"calculateRowCount", "scrollTo"}, constant = @Constant(intValue = 9))
    private int sampack_emitweaks$columns(int columns) {
        return CreativeLayout.get().columns;
    }

    @ModifyConstant(method = {"calculateRowCount", "scrollTo"}, constant = @Constant(intValue = 5))
    private int sampack_emitweaks$rows(int rows) {
        return CreativeLayout.get().rows;
    }

    @ModifyConstant(method = "canScroll", constant = @Constant(intValue = 45))
    private int sampack_emitweaks$gridSize(int gridSize) {
        return CreativeLayout.get().gridSize();
    }
}
