package com.carjem.sampackemitweaks.creative;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

/**
 * Tells EMI the creative inventory reaches out to its side tabs (see
 * {@link CreativeLayout#sideTabX}), which stick out of the panel on both sides. EMI lays out its
 * side panels from these bounds, and REMI its creative tab bars from EMI's panels, so both stay
 * clear of the tabs.
 */
@EmiEntrypoint
public class CreativeEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addScreenBoundsProvider(CreativeModeInventoryScreen.class, screen -> new Bounds(
                screen.getGuiLeft() - CreativeLayout.SIDE_TAB_DEPTH, screen.getGuiTop(),
                screen.getXSize() + 2 * CreativeLayout.SIDE_TAB_DEPTH, screen.getYSize()));
    }
}
