package com.carjem.sampackemitweaks.mixin.pristine;

import com.carjem.sampackemitweaks.pristine.PristineTabs;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.Set;

/**
 * Records each tab's freshly built contents into {@link PristineTabs}.
 *
 * buildContents ends by storing the builder's two collections into displayItems and then
 * displayItemsSearchTab. This wraps the second store, so it sees both, and it runs in place of
 * that instruction -- ahead of every TAIL injection, Recreative's postBuildContents included,
 * whatever order the mixins are applied in.
 */
@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabPristineMixin {

    @Shadow
    private Collection<ItemStack> displayItems;

    @WrapOperation(
            method = "buildContents",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/item/CreativeModeTab;displayItemsSearchTab:Ljava/util/Set;",
                    opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    private void sampack_emitweaks$recordPristine(
            CreativeModeTab tab,
            Set<ItemStack> searchItems,
            Operation<Void> original,
            @Local(argsOnly = true) CreativeModeTab.ItemDisplayParameters parameters) {
        original.call(tab, searchItems);
        PristineTabs.record(tab, parameters, displayItems, searchItems);
    }
}
