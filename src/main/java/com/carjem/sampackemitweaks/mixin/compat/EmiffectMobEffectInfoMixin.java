package com.carjem.sampackemitweaks.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hands EMIffect's per-effect scans only the registry entries that can match.
 *
 * MobEffectInfo is built once per mob effect during EMI's reload, and each one
 * walks every block (looking for FlowerBlocks with that suspicious-stew effect)
 * and every item (looking for food with that effect). With this pack's
 * registries that is tens of millions of checks, ~10% of the whole EMI reload.
 * The loops ignore everything except FlowerBlocks and items with a FOOD
 * component, so iterating just those -- in registry order -- gives the same
 * result.
 */
@Mixin(targets = "moe.prwk.emiffect.recipes.MobEffectInfo", remap = false)
public class EmiffectMobEffectInfoMixin {

    @Unique
    private static volatile List<Block> sampack_emitweaks$flowers;

    @Unique
    private static volatile List<Item> sampack_emitweaks$foods;

    @WrapOperation(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/DefaultedRegistry;iterator()Ljava/util/Iterator;"))
    private Iterator<?> sampack_emitweaks$onlyCandidates(DefaultedRegistry<?> registry, Operation<Iterator<?>> original) {
        if (registry == BuiltInRegistries.BLOCK) {
            List<Block> flowers = sampack_emitweaks$flowers;
            if (flowers == null) {
                flowers = BuiltInRegistries.BLOCK.stream().filter(block -> block instanceof FlowerBlock).toList();
                sampack_emitweaks$flowers = flowers;
            }
            return flowers.iterator();
        }
        if (registry == BuiltInRegistries.ITEM) {
            List<Item> foods = sampack_emitweaks$foods;
            if (foods == null) {
                foods = BuiltInRegistries.ITEM.stream().filter(item -> item.components().get(DataComponents.FOOD) != null).toList();
                sampack_emitweaks$foods = foods;
            }
            return foods.iterator();
        }
        return original.call(registry);
    }
}
