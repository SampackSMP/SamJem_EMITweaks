package com.carjem.sampackemitweaks.itemgroups;

import com.carjem.sampackemitweaks.itemgroups.config.Config;
import com.carjem.sampackemitweaks.itemgroups.config.Sort;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class Group {
    private boolean visibility;
    private final CreativeModeTab tab;
    private IndexedItemStack icon;
    private final Component name;
    private final ArrayList<IndexedItemStack> itemStacks = new ArrayList<>();
    // Plain view of the stacks, kept in sync with itemStacks so getItems() doesn't have to rebuild it.
    private final List<ItemStack> stacks = new ArrayList<>();
    private final List<ItemStack> stacksView = Collections.unmodifiableList(stacks);

    public Group(Component name, CreativeModeTab tab, ArrayList<ItemStack> itemStacks) {
        this.name = name;
        this.tab = tab;
        this.visibility = false;
        removeDuplicates(itemStacks);
        sort(itemStacks);

        if (!itemStacks.isEmpty()) {
            this.icon = new IndexedItemStack(itemStacks.get(0), -1);

            for (ItemStack itemStack : itemStacks) {
                this.itemStacks.add(new IndexedItemStack(itemStack, -1));
                this.stacks.add(itemStack);
            }
        }
    }

    private void sort(ArrayList<ItemStack> itemStacks) {
        if (Config.get().sort() == Sort.ALPHABETICALLY) {
            // Build each name once instead of on every comparison.
            Map<ItemStack, String> names = new IdentityHashMap<>(itemStacks.size() * 2);
            for (ItemStack stack : itemStacks)
                names.put(stack, stack.getItem().toString());
            itemStacks.sort(Comparator.comparing(names::get));
        }
    }

    private void removeDuplicates(ArrayList<ItemStack> list) {
        ArrayList<Group> groupsOnSelectedTab = InventoryItemGroups.groupsOnSelectedTab(tab);
        if (groupsOnSelectedTab.isEmpty()) return;

        Set<ItemStack> taken = Collections.newSetFromMap(new IdentityHashMap<>());
        groupsOnSelectedTab.forEach(group -> taken.addAll(group.stacks));
        list.removeIf(taken::contains);
    }

    public Component getName() {
        return name;
    }

    public CreativeModeTab getTab() {
        return tab;
    }

    /** Read-only view of this group's stacks. Copy it before modifying. */
    public List<ItemStack> getItems() {
        return stacksView;
    }

    public ArrayList<IndexedItemStack> getItemsWithIndexes() {
        return this.itemStacks;
    }

    public void setItemWithIndex(ItemStack item, int index) {
        this.itemStacks.forEach(entry -> {
            if (entry.getItemStack().equals(item)) {
                entry.setIndex(index);
            }
        });
    }

    public void resetItemIndexes() {
        this.itemStacks.forEach(entry -> entry.setIndex(-1));
    }

    public boolean isVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public ItemStack getIcon() {
        return icon.getItemStack();
    }

    public int getIconIndex() {
        return icon.getIndex();
    }

    public void setIconIndex(int index) {
        this.icon.setIndex(index);
    }
}
