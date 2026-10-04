package com.carjem.sampackemitweaks.mixin.tabs;

import com.carjem.sampackemitweaks.tabs.CreativeTabRules;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Applies the tab_order and remove_tab rules to NeoForge's sorted tab list, which the creative
 * screen splits into pages. NeoForge's own list (SORTED_TABS) is untouched, so
 * {@link com.carjem.sampackemitweaks.pristine.PristineTabs#displayOrder()} still reads the order
 * the game made.
 */
@Mixin(value = CreativeModeTabRegistry.class, remap = false)
public class CreativeModeTabRegistryRulesMixin {
    @Inject(method = "getSortedCreativeModeTabs", at = @At("RETURN"), cancellable = true)
    private static void sampack_emitweaks$applyRules(CallbackInfoReturnable<List<CreativeModeTab>> cir) {
        CreativeTabRules rules = CreativeTabRules.get();
        if (!rules.isEmpty()) {
            cir.setReturnValue(rules.sort(cir.getReturnValue()));
        }
    }
}
