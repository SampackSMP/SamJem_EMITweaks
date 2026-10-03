package com.carjem.sampackemitweaks.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Indexes REMI's stack groups by item id before matching every EMI stack.
 *
 * buildGroupedEmiStacksAndStackGroupToContents walks all ~1,400 stack groups for
 * each of ~50,000 EMI stacks, skipping any group whose optimizedIds set exists
 * and lacks the stack's id. That skip check alone runs tens of millions of times
 * per EMI reload. The per-stack loop now iterates only the groups that could
 * pass it -- groups listing the id, plus groups with no optimizedIds -- in their
 * original order, so REMI's own checks and first-match choice are unchanged.
 */
@Mixin(targets = "com.evandev.remi.feature.stackgroup.StackGroupManager", remap = false)
public class RemiStackGroupManagerMixin {

    @Shadow
    @Final
    public static List<?> stackGroups;

    @Unique
    private static Map<Object, List<Integer>> sampack_emitweaks$groupsById;

    @Unique
    private static List<Integer> sampack_emitweaks$unindexedGroups;

    @Inject(method = "buildGroupedEmiStacksAndStackGroupToContents", at = @At("HEAD"))
    private static void sampack_emitweaks$buildIndex(List<?> stacks, CallbackInfo ci) {
        sampack_emitweaks$groupsById = null;
        sampack_emitweaks$unindexedGroups = null;
        if (stackGroups.isEmpty()) {
            return;
        }
        try {
            Method getOptimizedIds = stackGroups.get(0).getClass().getMethod("getOptimizedIds");
            Map<Object, List<Integer>> byId = new HashMap<>();
            List<Integer> unindexed = new ArrayList<>();
            for (int i = 0; i < stackGroups.size(); i++) {
                Object group = stackGroups.get(i);
                Method method = group.getClass() == getOptimizedIds.getDeclaringClass()
                        ? getOptimizedIds
                        : group.getClass().getMethod("getOptimizedIds");
                Collection<?> ids = (Collection<?>) method.invoke(group);
                if (ids == null || ids.isEmpty()) {
                    unindexed.add(i);
                } else {
                    for (Object id : ids) {
                        byId.computeIfAbsent(id, k -> new ArrayList<>()).add(i);
                    }
                }
            }
            sampack_emitweaks$groupsById = byId;
            sampack_emitweaks$unindexedGroups = unindexed;
        } catch (ReflectiveOperationException | ClassCastException e) {
            // Fall back to REMI's full scan.
        }
    }

    @Inject(method = "buildGroupedEmiStacksAndStackGroupToContents", at = @At("RETURN"))
    private static void sampack_emitweaks$dropIndex(List<?> stacks, CallbackInfo ci) {
        sampack_emitweaks$groupsById = null;
        sampack_emitweaks$unindexedGroups = null;
    }

    @WrapOperation(
            method = "buildGroupedEmiStacksAndStackGroupToContents",
            at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 2))
    private static Iterator<?> sampack_emitweaks$candidateGroups(
            List<?> list,
            Operation<Iterator<?>> original,
            @Local ResourceLocation id) {
        Map<Object, List<Integer>> byId = sampack_emitweaks$groupsById;
        if (list != stackGroups || byId == null) {
            return original.call(list);
        }
        List<Integer> indexed = byId.getOrDefault(id, List.of());
        List<Integer> unindexed = sampack_emitweaks$unindexedGroups;
        List<Object> candidates = new ArrayList<>(indexed.size() + unindexed.size());
        int a = 0;
        int b = 0;
        while (a < indexed.size() || b < unindexed.size()) {
            int next = b >= unindexed.size() || (a < indexed.size() && indexed.get(a) < unindexed.get(b))
                    ? indexed.get(a++)
                    : unindexed.get(b++);
            candidates.add(stackGroups.get(next));
        }
        return candidates.iterator();
    }
}
