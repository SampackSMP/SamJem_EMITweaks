package com.carjem.sampackemitweaks.mixin.compat;

import com.carjem.sampackemitweaks.compat.ItemAbilityItemsCache;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shares Farmer's Delight's ItemAbilityIngredient item lists between recipes.
 *
 * getItems() builds an ItemStack for every registered item and keeps the ones
 * that can perform the ability. Every cutting board recipe has its own
 * ingredient instance, so EMI's reload repeats that full registry scan once per
 * recipe -- ~14 GB of allocation per reload in this pack. The matching items
 * only depend on the ability, so they are now found once per ability and each
 * caller gets fresh copies.
 */
@Mixin(targets = "vectorwing.farmersdelight.common.crafting.ingredient.ItemAbilityIngredient", remap = false)
public class FarmersDelightItemAbilityIngredientMixin {

    @Shadow
    @Final
    protected ItemAbility itemAbility;

    @Inject(method = "getItems", at = @At("HEAD"), cancellable = true)
    private void sampack_emitweaks$useSharedItems(CallbackInfoReturnable<Stream<ItemStack>> cir) {
        cir.setReturnValue(ItemAbilityItemsCache.itemsFor(this.itemAbility).stream().map(ItemStack::copy));
    }
}
