package com.carjem.sampackemitweaks.mixin.compat;

import com.carjem.sampackemitweaks.compat.RemiGroupIcons;
import com.carjem.sampackemitweaks.creative.GroupIcon;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BiFunction;

/**
 * Reads an {@code icon} key from each REMI stack group json, of any group type, into
 * {@link RemiGroupIcons}. StackGroupManager.loadGroup builds every group, from resource packs and
 * from config/remi/stack_groups/ alike, through its type's factory.
 */
@Mixin(targets = "com.evandev.remi.feature.stackgroup.StackGroupManager", remap = false)
public class RemiStackGroupIconMixin {
    @WrapOperation(
            method = "loadGroup",
            at = @At(value = "INVOKE", target = "Ljava/util/function/BiFunction;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object sampack_emitweaks$readIcon(BiFunction<Object, Object, Object> factory, Object id, Object json,
                                                     Operation<Object> original) {
        Object group = original.call(factory, id, json);
        if (group != null && json instanceof JsonObject object) {
            JsonElement icon = object.get("icon");
            String spec = icon != null && icon.isJsonPrimitive() ? icon.getAsString() : null;
            RemiGroupIcons.put(group, GroupIcon.parse(spec, id));
        }
        return group;
    }
}
