package com.carjem.sampackemitweaks.mixin.tabs;

import com.carjem.sampackemitweaks.tabs.CreativeTabRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides the tabs a remove_tab rule names, and gives a custom tab the name its rule has now
 * rather than the one it was registered with; see {@link CreativeTabRules}.
 */
@Mixin(CreativeModeTab.class)
public class CreativeModeTabRulesMixin {
    @Inject(method = "shouldDisplay", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$hideRemovedTab(CallbackInfoReturnable<Boolean> cir) {
        if (CreativeTabRules.get().isRemoved((CreativeModeTab) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$currentName(CallbackInfoReturnable<Component> cir) {
        ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey((CreativeModeTab) (Object) this);
        if (!CreativeTabRules.isOwnTab(id)) return;
        CreativeTabRules.CustomTab custom = CreativeTabRules.get().customTab(id);
        if (custom != null) {
            cir.setReturnValue(Component.translatable(custom.name()));
        }
    }
}
