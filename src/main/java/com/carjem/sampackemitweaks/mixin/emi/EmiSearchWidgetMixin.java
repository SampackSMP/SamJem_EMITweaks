package com.carjem.sampackemitweaks.mixin.emi;

import com.carjem.sampackemitweaks.search.EmiSearchBar;
import com.carjem.sampackemitweaks.search.SearchHistory;
import dev.emi.emi.screen.widget.EmiSearchWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * EMI's search bar: its history is {@link SearchHistory}'s (saved across restarts, and listed by
 * the arrow at the bar's right end), and an x next to the arrow clears it. EMI still adds to the
 * history itself, when the bar loses focus with text that isn't already the newest entry. Clicks
 * on the buttons are taken before they reach the bar ({@link EmiSearchBar#mouseClicked}).
 */
@Mixin(value = EmiSearchWidget.class, remap = false)
public abstract class EmiSearchWidgetMixin extends EditBox {
    @Shadow @Mutable private List<String> searchHistory;

    private EmiSearchWidgetMixin(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sampack_emitweaks$sharedHistory(CallbackInfo ci) {
        searchHistory = SearchHistory.entries();
    }

    /** EMI has just added the search to the history (or not); apply our size and save it. */
    @Inject(method = "setFocused", at = @At("TAIL"))
    private void sampack_emitweaks$saveHistory(boolean focused, CallbackInfo ci) {
        if (!focused) SearchHistory.changed();
    }

    @Inject(method = "renderWidget", at = @At("TAIL"))
    private void sampack_emitweaks$renderButtons(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EmiSearchBar.renderButtons(graphics, (EmiSearchWidget) (Object) this, mouseX, mouseY);
    }

    /** Keeps the text clear of the buttons. */
    @Override
    public int getInnerWidth() {
        return Math.max(0, super.getInnerWidth() - EmiSearchBar.reservedWidth());
    }
}
