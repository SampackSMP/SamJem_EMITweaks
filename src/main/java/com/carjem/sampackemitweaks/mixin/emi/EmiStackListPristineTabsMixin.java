package com.carjem.sampackemitweaks.mixin.emi;

import com.carjem.sampackemitweaks.pristine.PristineTabs;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.registry.EmiStackList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.List;

/**
 * Builds EMI's index from the creative tabs as their mods made them, not as Recreative rewrote them.
 *
 * EmiStackList.reload() builds every tab in CreativeModeTabs.allTabs() (lambda$reload$4, one tab
 * per lambda$reload$1), then walks allTabs() again reading each tab's search-tab items. Recreative
 * changes all of that: allTabs() comes back reordered, with its runtime tabs added and removed tabs
 * left out, and each tab's items are replaced by its rules. EMI's index, and so
 * `/icondump data`'s emi_dump.json, then reports the pack's own output instead of the game's.
 *
 * Both allTabs() calls now return the registry order vanilla's allTabs() has without Recreative,
 * and each tab's items come from {@link PristineTabs}, recorded during the buildContents call EMI
 * makes just before reading them. The live tabs are still built exactly as before, so the creative
 * screen is unchanged.
 *
 * The lambda names are EMI 1.1.x's; if they change, require = 0 leaves EMI's own behavior.
 */
@Mixin(value = EmiStackList.class, remap = false)
public class EmiStackListPristineTabsMixin {

    @WrapOperation(
            method = {"reload", "lambda$reload$4"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTabs;allTabs()Ljava/util/List;"),
            require = 0)
    private static List<CreativeModeTab> sampack_emitweaks$registryOrder(Operation<List<CreativeModeTab>> original) {
        return PristineTabs.registryOrder();
    }

    @WrapOperation(
            method = "lambda$reload$1",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;getSearchTabDisplayItems()Ljava/util/Collection;"),
            require = 0)
    private static Collection<ItemStack> sampack_emitweaks$pristineItems(CreativeModeTab tab, Operation<Collection<ItemStack>> original) {
        PristineTabs.Snapshot snapshot = PristineTabs.get(tab);
        return snapshot != null ? snapshot.searchItems() : original.call(tab);
    }
}
