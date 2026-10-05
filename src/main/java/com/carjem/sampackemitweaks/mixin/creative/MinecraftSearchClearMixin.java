package com.carjem.sampackemitweaks.mixin.creative;

import com.carjem.sampackemitweaks.client.ClientConfig;
import com.carjem.sampackemitweaks.creative.CreativeContents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leaving an inventory screen (the creative inventory, a chest, any container screen) clears EMI's
 * search bar, if the config says so. Going to one of EMI's screens (a recipe, say) and back keeps
 * it, as does the same kind of screen opening in its place (the creative screen reopening itself
 * at a new size) or a switch between the survival and creative inventories.
 */
@Mixin(Minecraft.class)
public class MinecraftSearchClearMixin {
    @Shadow @Nullable public Screen screen;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void sampack_emitweaks$clearSearchOnClose(@Nullable Screen next, CallbackInfo ci) {
        if (screen instanceof AbstractContainerScreen<?> && next != screen
                && (next == null || next.getClass() != screen.getClass())
                && !(sampack_emitweaks$isPlayerInventory(screen) && sampack_emitweaks$isPlayerInventory(next))
                && !CreativeContents.isEmiScreen(next)
                && ClientConfig.get(ClientConfig.CLEAR_SEARCH_ON_CLOSE)) {
            CreativeContents.clearEmiSearch();
        }
    }

    @Unique
    private static boolean sampack_emitweaks$isPlayerInventory(@Nullable Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }
}
