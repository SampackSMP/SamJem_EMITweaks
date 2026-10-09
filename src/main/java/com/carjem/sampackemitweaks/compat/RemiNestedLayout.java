package com.carjem.sampackemitweaks.compat;

import com.carjem.sampackemitweaks.creative.GroupTree;
import com.evandev.remi.feature.stackgroup.EmiGroupStack;
import com.evandev.remi.feature.stackgroup.GroupedEmiStack;
import com.evandev.remi.feature.stackgroup.StackGroupManager;
import com.evandev.remi.feature.stackgroup.data.StackGroup;
import com.evandev.remi.integration.emi.StackManager;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.config.SidebarType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * REMI's panels with {@link RemiNestedGroups subgroups}: what StackGroupManager.buildGroupedStacks,
 * StackManager.buildDisplayedStacks, StackGroupManager.buildGroupedIngredients, Layout.getGroup
 * and StackGroupManager.appendStacksForMatchingGroups do, laid out by {@link GroupTree}. The mixins
 * in {@code RemiNested*Mixin} call these only while a group has subgroups, so a pack without any
 * runs REMI's own code.
 *
 * <p>An outer group's {@link EmiGroupStack} holds every stack under it, its subgroups' included,
 * so its icon, its tooltip's count and a search by its name cover all of them; what it shows when
 * expanded, its own stacks and its subgroups' headers, is kept beside it.
 */
public final class RemiNestedLayout {
    /** A stack (or ingredient) and the group REMI matched it to; null for none. */
    private record Entry(EmiIngredient ingredient, @Nullable GroupedEmiStack<EmiStack> grouped) {
        @Nullable
        StackGroup group() {
            return grouped != null ? grouped.stackGroup : null;
        }
    }

    // What an outer group shows when expanded: stacks and its subgroups' EmiGroupStacks.
    private static final Map<EmiGroupStack, List<EmiStack>> CHILDREN = Collections.synchronizedMap(new WeakHashMap<>());

    private RemiNestedLayout() {
    }

    @Nullable
    private static StackGroup parent(StackGroup group) {
        return RemiNestedGroups.parent(group, g -> g.isEnabled);
    }

    /**
     * The group REMI matched the stack to, looked up the way buildGroupedStacks does. REMI rebuilds
     * these lists off-thread after a reload, so this reads a copy rather than iterating them live.
     */
    @Nullable
    public static GroupedEmiStack<EmiStack> grouped(EmiStack stack) {
        List<GroupedEmiStack<EmiStack>> variants = StackGroupManager.getStackToGroupedStacks().get(stack);
        if (variants == null) {
            variants = StackGroupManager.getItemToGroupedStacks().get(stack.getId());
            if (variants == null) return null;
            for (GroupedEmiStack<EmiStack> variant : snapshot(variants)) {
                if (variant.stackGroup.isEnabled && variant.realStack.isEqual(stack, Comparison.compareComponents())) {
                    return variant;
                }
            }
            return null;
        }
        for (GroupedEmiStack<EmiStack> variant : snapshot(variants)) {
            if (variant.stackGroup.isEnabled) return variant;
        }
        return null;
    }

    // toArray copies without a modification check; a slot cleared mid-copy reads as null.
    @SuppressWarnings("unchecked")
    private static List<GroupedEmiStack<EmiStack>> snapshot(List<GroupedEmiStack<EmiStack>> list) {
        List<GroupedEmiStack<EmiStack>> copy = new ArrayList<>();
        for (Object variant : list.toArray()) {
            if (variant != null) copy.add((GroupedEmiStack<EmiStack>) variant);
        }
        return copy;
    }

    private static List<Object> tree(List<Entry> entries) {
        return GroupTree.build(entries, Entry::group, RemiNestedLayout::parent);
    }

    @SuppressWarnings("unchecked")
    private static GroupTree.Node<Entry, StackGroup> node(Object node) {
        return (GroupTree.Node<Entry, StackGroup>) node;
    }

    private static List<GroupedEmiStack<EmiStack>> members(GroupTree.Node<Entry, StackGroup> node) {
        List<GroupedEmiStack<EmiStack>> members = new ArrayList<>(node.items().size());
        for (Entry entry : node.items()) members.add(entry.grouped());
        return members;
    }

    /** StackGroupManager.buildGroupedStacks: the index, each group (outermost) as one stack. */
    public static List<EmiStack> groupedStacks(List<EmiStack> source) {
        List<Entry> entries = new ArrayList<>(source.size());
        for (EmiStack stack : source) entries.add(new Entry(stack, grouped(stack)));
        List<Object> tree = tree(entries);

        List<EmiStack> result = new ArrayList<>(tree.size());
        for (Object node : tree) {
            result.add(node instanceof GroupTree.Node<?, ?> ? groupStack(node(node)) : (EmiStack) ((Entry) node).ingredient());
        }
        return result;
    }

    private static EmiGroupStack groupStack(GroupTree.Node<Entry, StackGroup> node) {
        List<GroupedEmiStack<EmiStack>> members = members(node);
        if (!node.hasSubgroups()) {
            // REMI reuses its cached stack for a group whose matches are all here
            EmiGroupStack cached = StackGroupManager.getGroupStack(node.group());
            if (cached != null && cached.itemsNew.size() == members.size() && ownOnly(node)) return cached;
            return new EmiGroupStack(node.group(), new ArrayList<>(members));
        }

        EmiGroupStack stack = new EmiGroupStack(node.group(), new ArrayList<>(members));
        List<EmiStack> children = new ArrayList<>(node.children().size());
        for (Object child : node.children()) {
            children.add(child instanceof GroupTree.Node<?, ?> ? groupStack(node(child)) : ((Entry) child).grouped().realStack);
        }
        CHILDREN.put(stack, children);
        return stack;
    }

    /** True if every stack under the group is its own, none a dissolved subgroup's. */
    private static boolean ownOnly(GroupTree.Node<Entry, StackGroup> node) {
        for (Entry entry : node.items()) {
            if (entry.group() != node.group()) return false;
        }
        return true;
    }

    /** StackManager.buildDisplayedStacks: the index panel, expanded groups opened in place. */
    public static List<EmiStack> displayedStacks(List<EmiStack> grouped) {
        List<EmiStack> result = new ArrayList<>(grouped.size());
        display(grouped, result);
        return result;
    }

    private static void display(List<EmiStack> stacks, List<EmiStack> result) {
        for (EmiStack stack : stacks) {
            if (!(stack instanceof EmiGroupStack group)) {
                result.add(stack);
                continue;
            }
            List<GroupedEmiStack<EmiStack>> items = group.getItems();
            if (items.isEmpty()) continue;
            if (items.size() == 1) {
                result.add(items.getFirst().realStack);
                continue;
            }
            // REMI sets this on the outermost groups only
            group.isExpanded = StackManager.isGroupExpanded(SidebarType.INDEX, group.group.getId());
            result.add(group);
            if (!group.isExpanded) continue;
            List<EmiStack> children = CHILDREN.get(group);
            if (children != null) {
                display(children, result);
            } else {
                for (GroupedEmiStack<EmiStack> item : items) result.add(item.realStack);
            }
        }
    }

    /** StackGroupManager.buildGroupedIngredients: the other sidebars, expanded groups opened in place. */
    public static List<EmiIngredient> groupedIngredients(List<? extends EmiIngredient> source, SidebarType type) {
        if (source == null || source.isEmpty()) return List.of();
        List<Entry> entries = new ArrayList<>(source.size());
        for (EmiIngredient ingredient : source) {
            if (ingredient == null) continue;
            EmiStack primary = ingredient instanceof EmiStack stack ? stack
                    : !ingredient.getEmiStacks().isEmpty() ? ingredient.getEmiStacks().getFirst() : null;
            GroupedEmiStack<EmiStack> grouped = primary != null ? grouped(primary) : null;
            entries.add(new Entry(ingredient, grouped != null ? new GroupedEmiStack<>(primary, ingredient, grouped.stackGroup) : null));
        }

        List<EmiIngredient> result = new ArrayList<>(entries.size());
        ingredients(tree(entries), type, result);
        return result;
    }

    private static void ingredients(List<Object> nodes, SidebarType type, List<EmiIngredient> result) {
        for (Object node : nodes) {
            if (!(node instanceof GroupTree.Node<?, ?>)) {
                result.add(((Entry) node).ingredient());
                continue;
            }
            GroupTree.Node<Entry, StackGroup> group = node(node);
            EmiGroupStack stack = new EmiGroupStack(group.group(), members(group));
            stack.isExpanded = StackManager.isGroupExpanded(type, group.group().getId());
            result.add(stack);
            if (stack.isExpanded) ingredients(group.children(), type, result);
        }
    }

    /**
     * Layout.getGroup: the expanded group whose background a panel slot is drawn on. That is the
     * innermost expanded group around it: a closed subgroup's header, inside an open group, is
     * drawn on the open group's.
     */
    @Nullable
    public static StackGroup backgroundGroup(@Nullable EmiStack stack, SidebarType type) {
        if (stack == null) return null;
        StackGroup group;
        if (stack instanceof EmiGroupStack groupStack) {
            group = groupStack.group;
        } else if (stack instanceof GroupedEmiStack<?> grouped) {
            group = grouped.stackGroup;
        } else {
            GroupedEmiStack<EmiStack> grouped = grouped(stack);
            group = grouped != null ? grouped.stackGroup : null;
        }
        for (StackGroup g : GroupTree.chain(group, RemiNestedLayout::parent)) {
            if (StackManager.isGroupExpanded(type, g.getId())) return g;
        }
        return null;
    }

    /**
     * StackGroupManager.appendStacksForMatchingGroups: adds the stacks of every group the search
     * names, by id or name, to the results; a group's subgroups' stacks come with its own.
     */
    public static void appendStacksForMatchingGroups(String query, List<EmiStack> results) {
        String lower = query.toLowerCase(Locale.ROOT);
        Set<EmiStack> existing = new HashSet<>(results);
        Set<ResourceLocation> allowedIds = null;
        List<EmiStack> tabSource = StackManager.sourceStacks;
        if (tabSource != null && tabSource != StackManager.indexStacks) {
            allowedIds = new HashSet<>(tabSource.size());
            for (EmiStack stack : tabSource) allowedIds.add(stack.getId());
        }

        for (StackGroup group : StackGroupManager.stackGroups) {
            if (!group.isEnabled) continue;
            EmiGroupStack groupStack = StackGroupManager.getGroupStack(group);
            if (groupStack == null) continue;
            if (!group.getId().toString().toLowerCase(Locale.ROOT).contains(lower)
                    && !groupStack.getName().getString().toLowerCase(Locale.ROOT).contains(lower)) continue;
            append(group, allowedIds, existing, results, 0);
        }
    }

    private static void append(StackGroup group, @Nullable Set<ResourceLocation> allowedIds, Set<EmiStack> existing,
                               List<EmiStack> results, int depth) {
        EmiGroupStack groupStack = StackGroupManager.getGroupStack(group);
        if (groupStack != null) {
            for (GroupedEmiStack<EmiStack> item : groupStack.getItems()) {
                if ((allowedIds == null || allowedIds.contains(item.realStack.getId())) && existing.add(item.realStack)) {
                    results.add(item.realStack);
                }
            }
        }
        if (depth >= 16) return;
        for (Object subgroup : RemiNestedGroups.subgroups(group)) {
            append((StackGroup) subgroup, allowedIds, existing, results, depth + 1);
        }
    }
}
