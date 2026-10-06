package com.carjem.sampackemitweaks.creative;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Lays out a list whose items each belong to at most one group, where a group can sit inside
 * another (a stack group's {@code subgroups}). Shared by the creative grid and REMI's panels, so
 * both nest the same way.
 *
 * <p>A group counts every item under it, its subgroups' included. One with fewer than two is
 * dissolved: its item goes to the nearest group above it that has two, or stands on its own. Each
 * group is placed once, where its first item would have been, as REMI places a group; inside it,
 * its items and subgroups keep the list's order.
 */
public final class GroupTree {
    /** Nesting deeper than this is treated as a cycle and cut off. */
    private static final int MAX_DEPTH = 16;

    /**
     * A group as laid out.
     *
     * @param children its items and subgroups ({@link Node}s), in list order
     * @param items    every item under it, subgroups' included, in list order
     */
    public record Node<T, G>(G group, List<Object> children, List<T> items) {
        Node(G group) {
            this(group, new ArrayList<>(), new ArrayList<>());
        }

        /** True if a subgroup was kept inside this one. */
        public boolean hasSubgroups() {
            for (Object child : children) {
                if (child instanceof Node<?, ?>) return true;
            }
            return false;
        }
    }

    private GroupTree() {
    }

    /**
     * @param groupOf each item's own group, or null
     * @param parent  the group a group sits inside, or null for none
     * @return the top level: items and {@link Node}s, in list order
     */
    public static <T, G> List<Object> build(List<T> items, Function<T, G> groupOf, UnaryOperator<G> parent) {
        List<List<G>> chains = new ArrayList<>(items.size());
        Map<G, Integer> counts = new IdentityHashMap<>();
        for (T item : items) {
            List<G> chain = chain(groupOf.apply(item), parent);
            chains.add(chain);
            for (G group : chain) counts.merge(group, 1, Integer::sum);
        }

        List<Object> top = new ArrayList<>(items.size());
        Map<G, Node<T, G>> nodes = new IdentityHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            List<G> chain = chains.get(i);
            Node<T, G> enclosing = null;
            // outermost first, so a group is attached to the one above it before its items arrive
            for (int depth = chain.size() - 1; depth >= 0; depth--) {
                G group = chain.get(depth);
                if (counts.get(group) < 2) continue;
                Node<T, G> node = nodes.get(group);
                if (node == null) {
                    node = new Node<>(group);
                    nodes.put(group, node);
                    (enclosing == null ? top : enclosing.children()).add(node);
                }
                node.items().add(item);
                enclosing = node;
            }
            (enclosing == null ? top : enclosing.children()).add(item);
        }
        return top;
    }

    /** The group and every group above it, innermost first. */
    public static <G> List<G> chain(G group, UnaryOperator<G> parent) {
        if (group == null) return List.of();
        List<G> chain = new ArrayList<>(2);
        for (G g = group; g != null && chain.size() < MAX_DEPTH && !containsIdentity(chain, g); g = parent.apply(g)) {
            chain.add(g);
        }
        return chain;
    }

    private static boolean containsIdentity(List<?> list, Object value) {
        for (Object element : list) {
            if (element == value) return true;
        }
        return false;
    }
}
