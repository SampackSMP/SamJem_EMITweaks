package com.carjem.sampackemitweaks.mixin.tabs;

import com.carjem.sampackemitweaks.tabs.CreativeTabRules;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Applies the tab_order rule to {@code tabs()}, the list REMI's creative tab sidebar is built
 * from, and opens the creative screen on the first tab in that order rather than building
 * blocks, which a remove_tab rule may have hidden.
 */
@Mixin(CreativeModeTabs.class)
public class CreativeModeTabsRulesMixin {
    @Inject(method = "tabs", at = @At("RETURN"), cancellable = true)
    private static void sampack_emitweaks$orderTabs(CallbackInfoReturnable<List<CreativeModeTab>> cir) {
        CreativeTabRules rules = CreativeTabRules.get();
        if (!rules.isEmpty()) {
            cir.setReturnValue(rules.sort(cir.getReturnValue()));
        }
    }

    @Inject(method = "getDefaultTab", at = @At("HEAD"), cancellable = true)
    private static void sampack_emitweaks$firstTab(CallbackInfoReturnable<CreativeModeTab> cir) {
        if (CreativeTabRules.get().isEmpty()) return;
        // already ordered and filtered by CreativeModeTabRegistryRulesMixin; empty before the
        // first resource load, when vanilla's answer stands
        for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
            if (tab.getType() == CreativeModeTab.Type.CATEGORY && tab.shouldDisplay()) {
                cir.setReturnValue(tab);
                return;
            }
        }
    }
}
