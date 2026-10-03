package com.carjem.sampackemitweaks.mixin.itemgroups;

import com.carjem.sampackemitweaks.itemgroups.InventoryItemGroups;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Unique Screen newScreen;

    @Inject(method = "tick", at = @At("TAIL"))
    private void sampack_emitweaks$clearGroups(CallbackInfo ci) {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen != null) {
            if (newScreen != null && !currentScreen.equals(newScreen) && !(currentScreen instanceof CreativeModeInventoryScreen)) {
                InventoryItemGroups.groups.clear();
            }
            newScreen = currentScreen;
        } else {
            InventoryItemGroups.groups.clear();
        }
    }
}
