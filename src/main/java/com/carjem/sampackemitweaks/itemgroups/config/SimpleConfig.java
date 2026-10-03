package com.carjem.sampackemitweaks.itemgroups.config;

import com.carjem.sampackemitweaks.itemgroups.InventoryItemGroups;
import io.github.bizcub.simpleConfigLib.autoconfig.ConfigHolder;
import io.github.bizcub.simpleConfigLib.autoconfig.ConfigSide;
import io.github.bizcub.simpleConfigLib.autoconfig.annotation.*;
import net.minecraft.network.chat.Component;

import java.util.List;

@AutoConfig(name = InventoryItemGroups.NAMESPACE, fileName = InventoryItemGroups.NAMESPACE + "_scl", side = ConfigSide.CLIENT, translate = true, snakeCaseKeys = true)
public class SimpleConfig implements Config {
    public static ConfigHolder<SimpleConfig> getInstance() {
        return ConfigHolder.register(SimpleConfig.class);
    }

    @EnumConfig(translate = true)
    public Sort sort = Config.super.sort();

    @ListConfig(addToFront = true, translateElements = true)
    public List<ItemGroup> groups = Config.super.groups();

    @ListConfig(editable = false)
    public List<Component> idOfMenuTabs = InventoryItemGroups.getTabIds();

    @Override
    public Sort sort() {
        return this.sort;
    }

    @Override
    public List<ItemGroup> groups() {
        return this.groups;
    }
}
