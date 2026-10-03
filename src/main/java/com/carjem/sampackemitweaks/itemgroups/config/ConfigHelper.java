package com.carjem.sampackemitweaks.itemgroups.config;

import com.carjem.sampackemitweaks.itemgroups.InventoryItemGroups;
import io.github.bizcub.simpleConfigLib.autoconfig.gui.ConfigScreens;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;

public class ConfigHelper {
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static boolean isClothConfigLoaded() {
        return isModLoaded("cloth_config");
    }

    public static boolean isSimpleConfigLoaded() {
        return isModLoaded("simple_config_lib");
    }

    public static boolean isConfigLoaded() {
        return isClothConfigLoaded() || isSimpleConfigLoaded();
    }

    public static Screen getScreen(Screen parent) {
        if (isSimpleConfigLoaded()) {
            return ConfigScreens.open(InventoryItemGroups.NAMESPACE, parent);
        }
        if (isClothConfigLoaded()) {
            return ClothConfig.getConfigScreen(parent);
        }
        return parent;
    }
}
