package com.carjem.sampackemitweaks.mixin;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.InsertableLinkedOpenCustomHashSet;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes a duplicate creative-tab entry a no-op instead of a hard error.
 *
 * Several Create add-ons (create_power_loader, trading_floor, createdeco and
 * friends) put the same stack into their tab twice once the tab's contents are
 * built a second time -- once from the tab's own displayItemsGenerator and once
 * from Registrate's BuildCreativeModeTabContentsEvent listener. NeoForge's
 * assertNewEntryDoesNotAlreadyExists then throws, which aborts the whole tab
 * build. That is fatal for anything that rebuilds tabs: Recreative's
 * reloadTabs(), and ComputerCraft's onServerStarted -> tryRebuildTabContents.
 *
 * The assertion exists only to report the double-add. The collection behind it
 * is a set, so skipping the insert costs nothing: accept() runs
 * assertNewEntryDoesNotAlreadyExists and then add(), and add() on an element
 * the set already holds is a no-op. Cancelling the assertion when the entry is
 * already present therefore keeps the tab exactly as it would have been, and
 * lets the rest of the build finish.
 */
@Mixin(BuildCreativeModeTabContentsEvent.class)
public class BuildCreativeModeTabContentsEventMixin {

    @Inject(method = "assertNewEntryDoesNotAlreadyExists", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$ignoreDuplicateEntry(
            InsertableLinkedOpenCustomHashSet<ItemStack> entries,
            ItemStack stack,
            CallbackInfo ci) {
        if (entries.contains(stack)) {
            ci.cancel();
        }
    }
}
