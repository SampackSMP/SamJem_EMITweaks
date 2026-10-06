package com.carjem.sampackemitweaks.compat;

import com.carjem.sampackemitweaks.SampackEmiTweaks;
import com.carjem.sampackemitweaks.creative.GroupTree;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Stack groups inside other stack groups. A REMI stack group json (any type) can name the groups
 * that sit inside it:
 *
 * <pre>{"id": "sampack:aspen_logs", "type": "remi:group", "contents": [..],
 *  "subgroups": ["everycomp:ch/atmospheric/aspen_log"]}</pre>
 *
 * The key is on the outer group because REMI's tag-page toggle rewrites a remi:tag file from
 * scratch, which would drop it from the inner one. Each stack still belongs to the one group REMI
 * matched it to; the outer group then holds its own stacks and, folded inside it, each subgroup.
 * A group named by two outer groups stays in the first; a cycle is cut where it closes. Either is
 * logged.
 *
 * <p>{@link com.carjem.sampackemitweaks.mixin.compat.RemiStackGroupIconMixin} reads the key as a
 * group loads, and {@link #resolve} matches the ids to groups once REMI has loaded them all, on
 * EMI's reload thread; the render thread reads the result. Keyed by group object, like
 * {@link RemiGroupIcons}.
 */
public final class RemiNestedGroups {
    private static final Map<Object, List<String>> SUBGROUP_IDS = Collections.synchronizedMap(new WeakHashMap<>());

    // Replaced whole by resolve(), never changed after.
    private static volatile Map<Object, Object> parents = Map.of();
    private static volatile Map<Object, List<Object>> subgroups = Map.of();

    private RemiNestedGroups() {
    }

    /** Records the {@code subgroups} a group's json names, if any. */
    public static void read(Object group, Object id, JsonObject json) {
        JsonElement element = json.get("subgroups");
        if (element == null) {
            SUBGROUP_IDS.remove(group);
            return;
        }
        List<String> ids = new ArrayList<>();
        if (element instanceof JsonArray array) {
            for (JsonElement entry : array) {
                if (entry.isJsonPrimitive()) ids.add(entry.getAsString());
            }
        } else if (element.isJsonPrimitive()) {
            ids.add(element.getAsString());
        } else {
            SampackEmiTweaks.LOGGER.warn("Stack group {}: \"subgroups\" should be a list of group ids", id);
        }
        SUBGROUP_IDS.put(group, ids);
    }

    /**
     * Matches every group's subgroup ids to the loaded groups.
     *
     * @param groups REMI's groups, in its order
     * @param idOf   a group's id
     */
    public static void resolve(List<?> groups, java.util.function.Function<Object, Object> idOf) {
        Map<String, Object> byId = new HashMap<>();
        for (Object group : groups) byId.putIfAbsent(idOf.apply(group).toString(), group);

        Map<Object, Object> parentOf = new IdentityHashMap<>();
        Map<Object, List<Object>> childrenOf = new IdentityHashMap<>();
        for (Object group : groups) {
            List<String> ids = SUBGROUP_IDS.get(group);
            if (ids == null) continue;
            for (String childId : ids) {
                Object child = byId.get(childId);
                if (child == null) continue; // a set with no items in this instance; not worth a line
                Object outer = parentOf.get(child);
                if (outer != null) {
                    SampackEmiTweaks.LOGGER.warn("Stack group {} is a subgroup of both {} and {}; keeping {}",
                            childId, idOf.apply(outer), idOf.apply(group), idOf.apply(outer));
                    continue;
                }
                if (GroupTree.chain(group, parentOf::get).contains(child)) {
                    SampackEmiTweaks.LOGGER.warn("Stack group {} would sit inside itself through {}; ignoring",
                            childId, idOf.apply(group));
                    continue;
                }
                parentOf.put(child, group);
                childrenOf.computeIfAbsent(group, k -> new ArrayList<>()).add(child);
            }
        }
        parents = parentOf;
        subgroups = childrenOf;
    }

    /** True if any loaded group has subgroups; until then REMI lays groups out itself. */
    public static boolean isActive() {
        return !parents.isEmpty();
    }

    /**
     * The nearest enabled group the group sits inside, or null. A disabled outer group lets its
     * subgroups stand on their own.
     *
     * @param enabled whether a group is enabled
     */
    @Nullable
    public static <G> G parent(G group, java.util.function.Predicate<G> enabled) {
        Map<Object, Object> map = parents;
        Object outer = map.get(group);
        for (int depth = 0; outer != null && depth < 16; depth++) {
            @SuppressWarnings("unchecked") G typed = (G) outer;
            if (enabled.test(typed)) return typed;
            outer = map.get(outer);
        }
        return null;
    }

    /** The groups directly inside the group. */
    public static List<Object> subgroups(Object group) {
        return subgroups.getOrDefault(group, List.of());
    }
}
