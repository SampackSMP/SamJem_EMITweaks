package com.carjem.sampackemitweaks.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Drops REMI's default stack groups: the {@code assets/<ns>/stack_groups/*.json} files in REMI's own
 * jar (minecraft:planks, c:dyes, ...). The pack's groups come from InvIndexLedger, which writes
 * them to config/remi/stack_groups/; REMI still loads those, and stack groups any other mod or
 * resource pack ships.
 *
 * StackGroupManager.reload() lists every stack_groups/ resource; this removes the ones from
 * REMI's mod resource pack, which NeoForge names "mod/remi".
 */
@Mixin(targets = "com.evandev.remi.feature.stackgroup.StackGroupManager", remap = false)
public class RemiDefaultStackGroupsMixin {

    @Unique
    private static final String REMI_PACK = "mod/remi";

    @WrapOperation(
            method = "reload",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/ResourceManager;listResources(Ljava/lang/String;Ljava/util/function/Predicate;)Ljava/util/Map;"))
    private static Map<ResourceLocation, Resource> sampack_emitweaks$withoutRemiDefaults(
            ResourceManager manager, String path, Predicate<ResourceLocation> filter,
            Operation<Map<ResourceLocation, Resource>> original) {
        Map<ResourceLocation, Resource> resources = new LinkedHashMap<>(original.call(manager, path, filter));
        resources.values().removeIf(resource -> REMI_PACK.equals(resource.sourcePackId()));
        return resources;
    }
}
