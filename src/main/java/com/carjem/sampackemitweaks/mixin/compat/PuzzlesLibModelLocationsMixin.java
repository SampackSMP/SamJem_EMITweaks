package com.carjem.sampackemitweaks.mixin.compat;

import com.carjem.sampackemitweaks.compat.ModelLocationsCache;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Builds Puzzles Lib's top-level model location set once per model bake, in
 * parallel.
 *
 * Every ModifyUnbakedModel / ModifyBakedModel handler registered through Puzzles
 * Lib (Diagonal Fences registers two) calls getTopLevelModelLocations(), which
 * builds a ModelResourceLocation for every BlockState and Item in the game --
 * ~2.2 s on one thread with this pack, during the serial model bake event.
 *
 * The result depends only on the block and item registries, which are frozen
 * before any model bake, and both callers only iterate it, so it is computed
 * once per bake and dropped when the bake completes (see ModelLocationsCache).
 * The computation is the same as Puzzles Lib's -- the missing model, every block
 * state's location, every item's inventory location, and the trident and
 * spyglass in-hand models -- but the block states are mapped across all cores.
 * stateToModelLocation only reads immutable state, and the ModelResourceLocation
 * constructor's only hook in this pack (FerriteCore's string deduplication) uses
 * a ConcurrentHashMap.
 */
@Mixin(targets = "fuzs.puzzleslib.neoforge.impl.client.event.NeoForgeClientEventInvokers", remap = false)
public class PuzzlesLibModelLocationsMixin {

    @Inject(method = "getTopLevelModelLocations", at = @At("HEAD"), cancellable = true)
    private static void sampack_emitweaks$computeOnce(CallbackInfoReturnable<Set<ModelResourceLocation>> cir) {
        Set<ModelResourceLocation> locations = ModelLocationsCache.topLevelLocations;
        if (locations == null) {
            locations = BuiltInRegistries.BLOCK.stream()
                    .toList()
                    .parallelStream()
                    .flatMap(block -> block.getStateDefinition().getPossibleStates().stream())
                    .map(BlockModelShaper::stateToModelLocation)
                    .collect(Collectors.toCollection(HashSet::new));
            locations.add(ModelBakery.MISSING_MODEL_VARIANT);
            for (var id : BuiltInRegistries.ITEM.keySet()) {
                locations.add(ModelResourceLocation.inventory(id));
            }
            locations.add(ItemRenderer.TRIDENT_IN_HAND_MODEL);
            locations.add(ItemRenderer.SPYGLASS_IN_HAND_MODEL);
            ModelLocationsCache.topLevelLocations = locations;
        }
        cir.setReturnValue(locations);
    }
}
