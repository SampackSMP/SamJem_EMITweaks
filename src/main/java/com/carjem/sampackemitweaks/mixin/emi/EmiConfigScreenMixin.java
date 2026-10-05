package com.carjem.sampackemitweaks.mixin.emi;

import com.carjem.sampackemitweaks.SampackEmiTweaks;
import com.carjem.sampackemitweaks.config.EmiConfigSections;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.emi.screen.ConfigScreen;
import dev.emi.emi.screen.widget.config.ConfigJumpButton;
import dev.emi.emi.screen.widget.config.ConfigSearch;
import dev.emi.emi.screen.widget.config.ListWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Merges {@link EmiConfigSections} (this mod's settings, and REMI's) into EMI's config screen's
 * groups, with buttons in EMI's jump bar. They are searched, collapsed, counted by the revert
 * button and reverted by it like EMI's own, and saved when the screen closes, after EMI has
 * written its own config.
 */
@Mixin(value = ConfigScreen.class, remap = false)
public abstract class EmiConfigScreenMixin extends Screen {
    @Shadow private ConfigSearch search;
    @Shadow public ListWidget list;
    @Shadow public Button resetButton;

    @Unique private EmiConfigSections sampack_emitweaks$sections;

    private EmiConfigScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sampack_emitweaks$snapshot(Screen last, CallbackInfo ci) {
        try {
            sampack_emitweaks$sections = new EmiConfigSections();
        } catch (RuntimeException | LinkageError e) {
            SampackEmiTweaks.LOGGER.error("Couldn't add the EMI Tweaks and REMI settings to EMI's config screen", e);
        }
    }

    /** Right after EMI's own rows, before the list's scroll position is restored. */
    @Inject(method = "init", at = @At(value = "INVOKE",
            target = "Ldev/emi/emi/screen/ConfigScreen;addWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;",
            ordinal = 1))
    private void sampack_emitweaks$addSections(CallbackInfo ci, @Local Set<String> collapsed) {
        if (sampack_emitweaks$sections == null) return;
        try {
            sampack_emitweaks$sections.addEntries((ConfigScreen) (Object) this, list, search::getSearch, collapsed);
        } catch (RuntimeException e) {
            SampackEmiTweaks.LOGGER.error("Couldn't add the EMI Tweaks and REMI settings to EMI's config screen", e);
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sampack_emitweaks$jump(CallbackInfo ci) {
        EmiConfigSections.jump(list);
    }

    /** Our new subgroups get buttons in EMI's jump bar, under their parent group's. */
    @ModifyExpressionValue(method = "addJumpButtons", at = @At(value = "INVOKE",
            target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;"))
    private ArrayList<String> sampack_emitweaks$addJumps(ArrayList<String> jumps) {
        if (sampack_emitweaks$sections != null) sampack_emitweaks$sections.addJumpIds(jumps);
        return jumps;
    }

    /** When the bar runs out of room, our subgroup buttons go first, then EMI's as it drops them. */
    @ModifyExpressionValue(method = "addJumpButtons", at = @At(value = "INVOKE",
            target = "Ljava/util/List;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;"))
    private List<List<String>> sampack_emitweaks$dropOurJumpsFirst(List<List<String>> removes) {
        if (sampack_emitweaks$sections == null) return removes;
        List<List<String>> all = new ArrayList<>();
        all.add(sampack_emitweaks$sections.jumpRemovals());
        all.addAll(removes);
        return all;
    }

    /** Our ids get EMI's button drawing from our icon sheet, since EMI's has no icon for them. */
    @WrapOperation(method = "addJumpButtons", at = @At(value = "NEW", target = "dev/emi/emi/screen/widget/config/ConfigJumpButton"))
    private ConfigJumpButton sampack_emitweaks$ourJumpButton(int x, int y, int u, int v, Button.OnPress action, List<Component> text,
                                                             Operation<ConfigJumpButton> original, @Local String id) {
        if (sampack_emitweaks$sections != null
                && sampack_emitweaks$sections.jumpButton(id, x, y, action) instanceof ConfigJumpButton ours) {
            return ours;
        }
        return original.call(x, y, u, v, action, text);
    }

    @Inject(method = "updateChanges", at = @At("TAIL"))
    private void sampack_emitweaks$countChanges(CallbackInfo ci, @Local(ordinal = 0) int different) {
        int changes = sampack_emitweaks$sections != null ? sampack_emitweaks$sections.changes() : 0;
        if (changes == 0) return;
        resetButton.active = true;
        resetButton.setMessage(Component.translatable("screen.emi.config.reset", different + changes));
    }

    /** EMI's revert button: revert ours too, before EMI rebuilds the screen from the values. */
    @Inject(method = "lambda$init$0", at = @At(value = "INVOKE",
            target = "Ldev/emi/emi/config/EmiConfig;loadConfig(Ldev/emi/emi/com/unascribed/qdcss/QDCSS;)V", shift = At.Shift.AFTER),
            require = 0)
    private void sampack_emitweaks$revert(Button button, CallbackInfo ci) {
        if (sampack_emitweaks$sections != null) sampack_emitweaks$sections.revert();
    }

    @Inject(method = "onClose", at = @At(value = "INVOKE", target = "Ldev/emi/emi/config/EmiConfig;writeConfig()V", shift = At.Shift.AFTER))
    private void sampack_emitweaks$save(CallbackInfo ci) {
        if (sampack_emitweaks$sections != null) sampack_emitweaks$sections.save();
    }
}
