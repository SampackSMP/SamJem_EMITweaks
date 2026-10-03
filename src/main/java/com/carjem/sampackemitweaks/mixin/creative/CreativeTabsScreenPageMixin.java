package com.carjem.sampackemitweaks.mixin.creative;

import com.carjem.sampackemitweaks.creative.CreativeLayout;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Splits each page's tabs into top and bottom rows of {@link CreativeLayout#tabsPerRow()} instead of 5. */
@Mixin(CreativeTabsScreenPage.class)
public class CreativeTabsScreenPageMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 10))
    private int sampack_emitweaks$tabsPerPage(int tabs) {
        return CreativeLayout.get().tabsPerPage();
    }
}
