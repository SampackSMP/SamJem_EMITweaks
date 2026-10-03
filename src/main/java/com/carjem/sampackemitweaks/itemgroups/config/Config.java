package com.carjem.sampackemitweaks.itemgroups.config;

import com.carjem.sampackemitweaks.itemgroups.InventoryItemGroups;

import java.util.List;

public interface Config {
    static Config get() {
        return Holder.INSTANCE;
    }

    static void set(final Config config) {
        if (config != null) {
            Holder.INSTANCE = config;
        }
    }

    class Holder {
        private static Config INSTANCE = new Config() { };
    }

    default Sort sort() {
        return Sort.DEFAULT;
    }

    default List<ItemGroup> groups() {
        return InventoryItemGroups.getDefaultGroups();
    }
}
