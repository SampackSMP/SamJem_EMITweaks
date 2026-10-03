package com.carjem.sampackemitweaks.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Comparator;
import java.util.stream.Stream;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Feeds BCLib's AnvilRecipe.getIngredients only the items in the hammer tag.
 *
 * getIngredients() streams every registered item, builds each one's default
 * ItemStack and keeps those tagged as hammers (isHammer), then filters by the
 * recipe's tool level. Zeta's recipe crawl calls it for every anvil recipe when
 * the server starts, so that full-registry scan runs once per recipe on the
 * Server thread before the player can join. Only hammer-tagged items can pass
 * isHammer, so streaming just the tag's members -- in registry order, with every
 * original filter still applied -- gives the same ingredient.
 *
 * The tag is read from WorldWeaver's CommonItemTags.HAMMERS, the same field
 * isHammer uses; if that lookup fails the original full scan runs.
 */
@Mixin(targets = "org.betterx.bclib.recipes.AnvilRecipe", remap = false)
public class BclibAnvilRecipeMixin {

    @Unique
    private static volatile TagKey<Item> sampack_emitweaks$hammers;

    @Unique
    private static volatile boolean sampack_emitweaks$lookupFailed;

    @Unique
    @SuppressWarnings("unchecked")
    private static TagKey<Item> sampack_emitweaks$hammerTag() {
        TagKey<Item> tag = sampack_emitweaks$hammers;
        if (tag == null && !sampack_emitweaks$lookupFailed) {
            try {
                tag = (TagKey<Item>) Class.forName("org.betterx.wover.tag.api.predefined.CommonItemTags")
                        .getField("HAMMERS")
                        .get(null);
                sampack_emitweaks$hammers = tag;
            } catch (ReflectiveOperationException | ClassCastException e) {
                sampack_emitweaks$lookupFailed = true;
            }
        }
        return tag;
    }

    @WrapOperation(
            method = "getIngredients",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/DefaultedRegistry;stream()Ljava/util/stream/Stream;"))
    private Stream<?> sampack_emitweaks$onlyHammers(DefaultedRegistry<?> registry, Operation<Stream<?>> original) {
        TagKey<Item> hammers = registry == BuiltInRegistries.ITEM ? sampack_emitweaks$hammerTag() : null;
        if (hammers == null) {
            return original.call(registry);
        }
        return BuiltInRegistries.ITEM.getTag(hammers)
                .map(tag -> tag.stream()
                        .map(Holder::value)
                        .sorted(Comparator.comparingInt(BuiltInRegistries.ITEM::getId)))
                .orElseGet(Stream::<Item>empty);
    }
}
