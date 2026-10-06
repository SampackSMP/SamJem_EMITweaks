package com.carjem.sampackemitweaks.mixin.emi;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.emi.emi.search.EmiSearch;

/**
 * {@code EmiSearch.bakedStacks} stays null until the first search bake finishes, and EMI
 * Accelerator runs that bake on its own thread. Typing a search before it finishes left a compiled
 * query that {@code CompiledQuery.test} ran against the null set. That crashed the game from the
 * panel's slot highlighting and failed every search worker.
 * <p>
 * Until the bake is in, every stack counts as not baked, so the query uses EMI's own
 * {@code matchesUnbaked} path, which checks the stack directly.
 */
@Mixin(value = EmiSearch.CompiledQuery.class, remap = false)
public class EmiSearchCompiledQueryMixin {

    @Redirect(
        method = "test",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z")
    )
    private boolean sampack_emitweaks$bakedOrUnbaked(Set<?> bakedStacks, Object stack) {
        return bakedStacks != null && bakedStacks.contains(stack);
    }
}
