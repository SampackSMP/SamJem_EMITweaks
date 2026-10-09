package com.carjem.sampackemitweaks.creative;

import com.carjem.sampackemitweaks.client.ClientConfig;
import com.carjem.sampackemitweaks.creative.CreativeGrid.GroupKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What the creative grid shows: an item tab's items, or EMI's index on the search tab, filtered by
 * EMI's search bar and laid out in collapsible groups by {@link CreativeGrid}.
 *
 * <ul>
 *   <li>Searching: every item tab shows only the items EMI's panel shows for its current search:
 *   REMI's search results, which also hold the items of groups whose name matches. REMI searches
 *   its sidebar's tab, which its syncSelectedCreativeModeTab setting (on by default) keeps on the
 *   open tab. The search tab shows EMI's index (its items; fluids and other stacks are left out),
 *   in EMI's order, filtered the same way. No tab has its own search box (see
 *   {@link com.carjem.sampackemitweaks.mixin.creative.CreativeModeTabSearchMixin}).</li>
 *   <li>Groups: REMI's stack groups, exactly as it groups EMI's index, subgroups inside their
 *   groups ({@link GroupTree}).</li>
 * </ul>
 *
 * Each tab's items are matched to EMI's index and to their groups once per reload, so a search or
 * a group toggle is one pass over the tab. EMI and REMI are required on the client; their classes
 * are still only touched from the nested classes.
 */
public final class CreativeContents {
    /**
     * A tab's items, each with its EMI index stack and its group.
     *
     * @param source the collection the entries were built from, to notice a tab rebuild
     */
    private record Entries(Collection<ItemStack> source, List<ItemStack> items, Object[] stacks, GroupKey[] groups) {
    }

    private static final Map<CreativeModeTab, Entries> TABS = new IdentityHashMap<>();
    private static Entries index;
    // What the cached entries were built from; a change to any of it drops them.
    private static Object[] built = {};
    // What the open grid was last filled from.
    private static Object[] applied = {};

    private CreativeContents() {
    }

    private static boolean followsSearch() {
        return ClientConfig.get(ClientConfig.CREATIVE_FOLLOW_SEARCH);
    }

    private static boolean grouped() {
        return ClientConfig.get(ClientConfig.CREATIVE_GROUPS);
    }

    /** Puts the keyboard focus in EMI's search bar, if it is shown. */
    public static void focusEmiSearch(net.minecraft.client.gui.screens.Screen screen) {
        Emi.focusSearch(screen);
    }

    /** Clears EMI's search bar, keeping what it held in the search history. */
    public static void clearEmiSearch() {
        com.carjem.sampackemitweaks.search.EmiSearchBar.clear();
    }

    /** One of EMI's own screens (recipes, tree, config), which the creative screen comes back from. */
    public static boolean isEmiScreen(net.minecraft.client.gui.screens.Screen screen) {
        return screen != null && screen.getClass().getName().startsWith("dev.emi.emi.");
    }

    /** What an item tab's grid shows. */
    public static List<ItemStack> tabItems(CreativeModeTab tab, Collection<ItemStack> displayItems) {
        if (isEmiReloading()) {
            CreativeGrid.clear();
            return new ArrayList<>(displayItems);
        }
        dropOutdated();
        Entries entries = TABS.get(tab);
        if (entries == null || entries.source() != displayItems) {
            entries = tabEntries(tab, displayItems);
            TABS.put(tab, entries);
        }
        return show(entries, tab);
    }

    /** What the search tab's grid shows: EMI's index. */
    public static List<ItemStack> indexItems() {
        if (isEmiReloading()) return List.of();
        dropOutdated();
        if (index == null) {
            index = Emi.indexEntries();
        }
        return show(index, net.minecraft.world.item.CreativeModeTabs.searchTab());
    }

    /**
     * EMI rebuilds its index, search and REMI's groups on its reload thread, and reading them
     * meanwhile can crash (its index map is rehashed under the reader). Until it is done, item
     * tabs show their plain contents and the search tab is empty; the grid is refilled when EMI
     * finishes, because its loaded state is one of the {@link #sources()}.
     */
    private static boolean isEmiReloading() {
        return !Emi.isLoaded();
    }

    /** True if EMI's search results, EMI's index or the groups changed since {@link #markApplied()}. */
    public static boolean isStale() {
        return !Arrays.equals(state(), applied, (a, b) -> a == b ? 0 : 1);
    }

    /** Records what the grid was just filled from. */
    public static void markApplied() {
        applied = state();
    }

    /** What the grid depends on: {@link #sources()} and EMI's search results. */
    private static Object[] state() {
        Object[] sources = sources();
        if (!followsSearch()) return sources;
        Object[] search = Emi.searchState();
        Object[] state = Arrays.copyOf(sources, sources.length + search.length);
        System.arraycopy(search, 0, state, sources.length, search.length);
        return state;
    }

    /** What the cached entries depend on; compared by identity (the booleans are the boxed constants). */
    private static Object[] sources() {
        Object[] settings = {grouped(), followsSearch()};
        Object[] sources = Emi.sources();
        Object[] all = Arrays.copyOf(sources, sources.length + settings.length);
        System.arraycopy(settings, 0, all, sources.length, settings.length);
        return all;
    }

    private static void dropOutdated() {
        Object[] now = sources();
        if (!Arrays.equals(now, built, (a, b) -> a == b ? 0 : 1)) {
            built = now;
            TABS.clear();
            index = null;
            Remi.clear();
        }
    }

    private static Entries tabEntries(CreativeModeTab tab, Collection<ItemStack> displayItems) {
        List<ItemStack> items = new ArrayList<>(displayItems);
        Object[] stacks = Emi.indexed(items);
        GroupKey[] groups = grouped() && Remi.isEnabled() ? Remi.groups(stacks) : new GroupKey[items.size()];
        return new Entries(displayItems, items, stacks, groups);
    }

    private static List<ItemStack> show(Entries entries, CreativeModeTab tab) {
        if (followsSearch() && Emi.isSearching()) {
            entries = Emi.filter(entries, tab);
        }
        return CreativeGrid.layout(entries.items(), entries.groups());
    }

    /**
     * The groups the search text names, by name or id, as REMI matches them, and every group inside
     * one of them.
     */
    private static Set<GroupKey> groupsNamed(String text, GroupKey[] groups) {
        Set<GroupKey> named = Collections.newSetFromMap(new IdentityHashMap<>());
        if (text.startsWith("%")) text = text.substring(1);
        if (text.isEmpty()) return named;
        String lower = text.toLowerCase(Locale.ROOT);

        Set<GroupKey> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (GroupKey key : groups) {
            for (GroupKey group : GroupTree.chain(key, GroupKey::parent)) {
                if (seen.add(group)
                        && (group.id().toString().toLowerCase(Locale.ROOT).contains(lower)
                        || group.name().getString().toLowerCase(Locale.ROOT).contains(lower))) {
                    named.add(group);
                }
            }
        }
        if (named.isEmpty()) return named;

        // the groups under a named one: every key whose chain passes through it
        for (GroupKey key : seen) {
            for (GroupKey group : GroupTree.chain(key, GroupKey::parent)) {
                if (named.contains(group)) {
                    named.add(key);
                    break;
                }
            }
        }
        return named;
    }

    /** EMI's side. */
    private static final class Emi {
        /** EMI's index, whether EMI (and so REMI) has finished loading, and REMI's groups. */
        static Object[] sources() {
            return new Object[]{dev.emi.emi.registry.EmiStackList.stacks, dev.emi.emi.registry.EmiStackList.filteredStacks,
                    dev.emi.emi.runtime.EmiReloadManager.isLoaded(), Remi.generation()};
        }

        // The last search results seen, and the items they were searched from. Results only count
        // for the items they were searched from: right after a tab switch, REMI is still searching
        // the new tab and the results are the old tab's.
        private static Object lastResults;
        private static Object lastResultsSource;
        private static Set<dev.emi.emi.api.stack.EmiStack> resultSet;

        static boolean isLoaded() {
            return dev.emi.emi.runtime.EmiReloadManager.isLoaded();
        }

        static void focusSearch(net.minecraft.client.gui.screens.Screen screen) {
            dev.emi.emi.screen.widget.EmiSearchWidget search = dev.emi.emi.screen.EmiScreenManager.search;
            if (search == null || !search.isVisible()) return;
            screen.setFocused(search);
            search.setFocused(true);
        }

        static boolean isSearching() {
            return !dev.emi.emi.api.EmiApi.getSearchText().isEmpty();
        }

        /**
         * Whether a search is on and, while one is, the results EMI's panel shows and what they
         * were searched from. Unsearched, the grid ignores them, so they don't refill it.
         */
        static Object[] searchState() {
            observeResults();
            return isSearching()
                    ? new Object[]{true, lastResults, lastResultsSource, searchSource()}
                    : new Object[]{false};
        }

        private static List<? extends dev.emi.emi.api.stack.EmiIngredient> searchResults() {
            return Remi.searchResults();
        }

        private static Object searchSource() {
            return Remi.searchSource();
        }

        private static void observeResults() {
            // REMI updates its results under this lock, right after EMI applies its own
            synchronized (dev.emi.emi.search.EmiSearch.class) {
                Object results = searchResults();
                if (results != lastResults) {
                    lastResults = results;
                    lastResultsSource = searchSource();
                    resultSet = null;
                }
            }
        }

        /**
         * The stacks EMI's panel shows for the current search, when they were searched from items
         * that include the tab's; null when they were not, or are not in yet.
         */
        private static Set<dev.emi.emi.api.stack.EmiStack> resultsFor(CreativeModeTab tab) {
            observeResults();
            Object source = searchSource();
            boolean covers = Remi.searchCovers(tab);
            if (!covers || lastResultsSource != source) return null;

            if (resultSet == null) {
                Set<dev.emi.emi.api.stack.EmiStack> set = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(
                        new dev.emi.emi.registry.EmiStackList.ComparisonHashStrategy());
                for (Object result : (List<?>) lastResults) {
                    if (result instanceof dev.emi.emi.api.stack.EmiStack stack) set.add(stack);
                }
                resultSet = set;
            }
            return resultSet;
        }

        static Entries filter(Entries entries, CreativeModeTab tab) {
            Set<dev.emi.emi.api.stack.EmiStack> results = resultsFor(tab);
            if (results == null) return filterByQuery(entries);

            List<ItemStack> items = new ArrayList<>();
            List<GroupKey> groups = new ArrayList<>();
            for (int i = 0; i < entries.items().size(); i++) {
                if (results.contains((dev.emi.emi.api.stack.EmiStack) entries.stacks()[i])) {
                    items.add(entries.items().get(i));
                    groups.add(entries.groups()[i]);
                }
            }
            return new Entries(null, items, null, groups.toArray(new GroupKey[0]));
        }

        /**
         * Until EMI's panel has results for these items (it searches a different list, or has not
         * finished), tests them against EMI's query directly, adding the groups the search names
         * as REMI does.
         */
        private static Entries filterByQuery(Entries entries) {
            dev.emi.emi.search.EmiSearch.CompiledQuery query = dev.emi.emi.search.EmiSearch.compiledQuery;
            if (query == null || query.isEmpty() || dev.emi.emi.search.EmiSearch.bakedStacks == null) return entries;
            Set<GroupKey> named = groupsNamed(dev.emi.emi.api.EmiApi.getSearchText(), entries.groups());

            List<ItemStack> items = new ArrayList<>();
            List<GroupKey> groups = new ArrayList<>();
            for (int i = 0; i < entries.items().size(); i++) {
                GroupKey group = entries.groups()[i];
                if ((group != null && named.contains(group))
                        || query.test((dev.emi.emi.api.stack.EmiStack) entries.stacks()[i])) {
                    items.add(entries.items().get(i));
                    groups.add(group);
                }
            }
            return new Entries(null, items, null, groups.toArray(new GroupKey[0]));
        }

        /**
         * The index's own instance of each item where it has one. EMI's queries and REMI's groups
         * match index stacks by identity against what they found while baking; any other stack has
         * its name and tooltip worked out again, which is far slower.
         */
        static Object[] indexed(List<ItemStack> items) {
            List<dev.emi.emi.api.stack.EmiStack> stacks = dev.emi.emi.registry.EmiStackList.stacks;
            Object[] result = new Object[items.size()];
            for (int i = 0; i < result.length; i++) {
                dev.emi.emi.api.stack.EmiStack stack = dev.emi.emi.api.stack.EmiStack.of(items.get(i));
                int index = dev.emi.emi.registry.EmiStackList.getIndex(stack);
                if (index >= 0 && index < stacks.size() && stacks.get(index).isEqual(stack)) {
                    stack = stacks.get(index);
                }
                result[i] = stack;
            }
            return result;
        }

        static Entries indexEntries() {
            List<ItemStack> items = new ArrayList<>();
            List<Object> stacks = new ArrayList<>();
            for (dev.emi.emi.api.stack.EmiStack stack : dev.emi.emi.registry.EmiStackList.filteredStacks) {
                ItemStack item = stack.getItemStack();
                if (item.isEmpty()) continue;
                items.add(item.copyWithCount(1));
                stacks.add(stack);
            }
            Object[] stackArray = stacks.toArray();
            GroupKey[] groups = grouped() && Remi.isEnabled() ? Remi.groups(stackArray) : new GroupKey[items.size()];
            return new Entries(null, items, stackArray, groups);
        }
    }

    /** REMI's side. */
    private static final class Remi {
        // One key per stack group, so the grid can tell groups apart by identity.
        private static final Map<com.evandev.remi.feature.stackgroup.data.StackGroup, GroupKey> KEYS = new IdentityHashMap<>();

        // REMI publishes new maps each time it rebuilds its groups
        static Object generation() {
            return com.evandev.remi.feature.stackgroup.StackGroupManager.getStackToGroupedStacks();
        }

        /** REMI's search results, before it groups them. */
        static List<? extends dev.emi.emi.api.stack.EmiIngredient> searchResults() {
            return com.evandev.remi.integration.emi.StackManager.searchedStacks;
        }

        /** What REMI searches: its sidebar tab's items, or EMI's index. */
        static Object searchSource() {
            return com.evandev.remi.integration.emi.StackManager.sourceStacks;
        }

        static boolean searchCovers(CreativeModeTab tab) {
            return com.evandev.remi.integration.emi.StackManager.sourceStacks == com.evandev.remi.integration.emi.StackManager.indexStacks
                    || com.evandev.remi.feature.creativemodetab.CreativeModeTabManager.getCurrentTab() == tab;
        }

        static void clear() {
            KEYS.clear();
        }

        /** REMI fills its group tables on EMI's reload thread; they are only read once EMI has loaded. */
        static boolean isEnabled() {
            return com.evandev.remi.config.ReliableEmiConfig.enableStackGroups
                    && dev.emi.emi.runtime.EmiReloadManager.isLoaded();
        }

        static GroupKey[] groups(Object[] stacks) {
            GroupKey[] groups = new GroupKey[stacks.length];
            for (int i = 0; i < stacks.length; i++) {
                com.evandev.remi.feature.stackgroup.data.StackGroup group = groupOf((dev.emi.emi.api.stack.EmiStack) stacks[i]);
                if (group != null) {
                    groups[i] = key(group);
                }
            }
            return groups;
        }

        /** The stack's first enabled group, looked up the way REMI's buildGroupedStacks does. */
        private static com.evandev.remi.feature.stackgroup.data.StackGroup groupOf(dev.emi.emi.api.stack.EmiStack stack) {
            var grouped = com.carjem.sampackemitweaks.compat.RemiNestedLayout.grouped(stack);
            return grouped != null ? grouped.stackGroup : null;
        }

        /** The group's key, its outer group's made first; REMI's subgroups never form a cycle. */
        private static GroupKey key(com.evandev.remi.feature.stackgroup.data.StackGroup group) {
            GroupKey key = KEYS.get(group);
            if (key != null) return key;
            com.evandev.remi.feature.stackgroup.data.StackGroup outer =
                    com.carjem.sampackemitweaks.compat.RemiNestedGroups.parent(group, g -> g.isEnabled);
            GroupKey parent = outer != null ? key(outer) : null;

            var groupStack = com.evandev.remi.feature.stackgroup.StackGroupManager.getGroupStack(group);
            Component name = groupStack != null ? groupStack.getName()
                    : group.name != null ? group.name
                    : Component.literal(group.getId().toString());
            key = new GroupKey(group.getId(), name, com.carjem.sampackemitweaks.compat.RemiGroupIcons.get(group), parent);
            KEYS.put(group, key);
            return key;
        }
    }
}
