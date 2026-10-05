package com.carjem.sampackemitweaks.config;

import com.carjem.sampackemitweaks.client.ClientConfig;
import com.carjem.sampackemitweaks.config.EmiConfigSections.Option;
import com.evandev.remi.config.ReliableEmiConfig;
import com.evandev.remi.config.ReliableEmiConfigScreen;
import com.evandev.remi.feature.creativemodetab.gui.CreativeModeTabConfigScreen;
import com.evandev.remi.feature.creativemodetab.gui.CreativeModeTabGui;
import com.evandev.remi.feature.stackgroup.gui.StackGroupConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * REMI's settings (as of REMI 4.7.1), with REMI's own names and tooltips, sorted by topic for
 * {@link EmiConfigSections}, next to this mod's changes to REMI. Only loaded with REMI. REMI saves
 * its config, and reapplies it, through ReliableEmiConfig.save().
 */
final class RemiConfigOptions {
    // REMI saves (and reapplies) everything of its own through one call.
    private static final Runnable SAVE_REMI = ReliableEmiConfig::save;

    private RemiConfigOptions() {
    }

    /** REMI's "Better Cheat Mode", right after EMI's own Cheat Mode. */
    static List<Option<?>> cheatMode() {
        return List.of(bool("dragCheatToInventory", () -> ReliableEmiConfig.dragCheatToInventory, v -> ReliableEmiConfig.dragCheatToInventory = v));
    }

    /** Added to EMI's own General → Search. */
    static List<Option<?>> search() {
        return List.of(
                bool("searchById", () -> ReliableEmiConfig.searchById, v -> ReliableEmiConfig.searchById = v),
                bool("searchModPrefix", () -> ReliableEmiConfig.searchModPrefix, v -> ReliableEmiConfig.searchModPrefix = v),
                bool("searchTagPrefix", () -> ReliableEmiConfig.searchTagPrefix, v -> ReliableEmiConfig.searchTagPrefix = v),
                bool("searchTooltipPrefix", () -> ReliableEmiConfig.searchTooltipPrefix, v -> ReliableEmiConfig.searchTooltipPrefix = v));
    }

    /** How the search bar looks and where it sits. */
    static List<Option<?>> searchBar() {
        return List.of(
                bool("searchWidgetAlignWithPanel", () -> ReliableEmiConfig.searchWidgetAlignWithPanel, v -> ReliableEmiConfig.searchWidgetAlignWithPanel = v),
                integer("searchWidgetWidth", () -> ReliableEmiConfig.searchWidgetWidth, v -> ReliableEmiConfig.searchWidgetWidth = v),
                pair("search_bar_offset", "left", "top",
                        () -> ReliableEmiConfig.searchWidgetLeftOffset, v -> ReliableEmiConfig.searchWidgetLeftOffset = v,
                        () -> ReliableEmiConfig.searchWidgetTopOffset, v -> ReliableEmiConfig.searchWidgetTopOffset = v),
                pair("search_bar_padding", "horizontal", "vertical",
                        () -> ReliableEmiConfig.searchWidgetHorizontalPadding, v -> ReliableEmiConfig.searchWidgetHorizontalPadding = v,
                        () -> ReliableEmiConfig.searchWidgetVerticalPadding, v -> ReliableEmiConfig.searchWidgetVerticalPadding = v),
                EmiConfigSections.colors(EmiConfigSections.text("search_bar_colors"), EmiConfigSections.text("search_bar_colors.tooltip"),
                        List.of("text", "suggestion"),
                        List.of(() -> ReliableEmiConfig.searchWidgetTextColor, () -> ReliableEmiConfig.searchWidgetSuggestionTextColor),
                        List.of(v -> ReliableEmiConfig.searchWidgetTextColor = v, v -> ReliableEmiConfig.searchWidgetSuggestionTextColor = v))
                        .savedBy(SAVE_REMI),
                bool("searchWidgetUseVanillaTexture", () -> ReliableEmiConfig.searchWidgetUseVanillaTexture, v -> ReliableEmiConfig.searchWidgetUseVanillaTexture = v));
    }

    static List<Option<?>> tags() {
        return List.of(
                bool("enableCategorizedTagPages", () -> ReliableEmiConfig.enableCategorizedTagPages, v -> ReliableEmiConfig.enableCategorizedTagPages = v),
                bool("enableEntityTags", () -> ReliableEmiConfig.enableEntityTags, v -> ReliableEmiConfig.enableEntityTags = v),
                bool("enableTagSearchEnhancements", () -> ReliableEmiConfig.enableTagSearchEnhancements, v -> ReliableEmiConfig.enableTagSearchEnhancements = v));
    }

    /** REMI's creative tab sidebar, and this mod's tab sync. */
    static List<Option<?>> creativeTabs() {
        return List.of(
                bool("enableCreativeModeTabs", () -> ReliableEmiConfig.enableCreativeModeTabs, v -> ReliableEmiConfig.enableCreativeModeTabs = v),
                choice("creativeTabSidebarTarget", ReliableEmiConfig.CreativeTabSidebarTarget.class,
                        () -> ReliableEmiConfig.creativeTabSidebarTarget, v -> ReliableEmiConfig.creativeTabSidebarTarget = v),
                choice("creativeTabTheme", ReliableEmiConfig.CreativeTabTheme.class,
                        () -> ReliableEmiConfig.creativeTabTheme, v -> ReliableEmiConfig.creativeTabTheme = v),
                choice("tabAlignment", CreativeModeTabGui.TabAlignment.class,
                        () -> ReliableEmiConfig.tabAlignment, v -> ReliableEmiConfig.tabAlignment = v),
                pair("vertical_tabs", "width", "height",
                        () -> ReliableEmiConfig.verticalTabsWidth, v -> ReliableEmiConfig.verticalTabsWidth = v,
                        () -> ReliableEmiConfig.verticalTabsHeight, v -> ReliableEmiConfig.verticalTabsHeight = v),
                pair("horizontal_tabs", "width", "height",
                        () -> ReliableEmiConfig.horizontalTabsWidth, v -> ReliableEmiConfig.horizontalTabsWidth = v,
                        () -> ReliableEmiConfig.horizontalTabsHeight, v -> ReliableEmiConfig.horizontalTabsHeight = v),
                pair("tab_icons", "size", "max_tabs",
                        () -> ReliableEmiConfig.tabIconSize, v -> ReliableEmiConfig.tabIconSize = v,
                        () -> ReliableEmiConfig.maxSidebarTabs, v -> ReliableEmiConfig.maxSidebarTabs = v),
                bool("syncSelectedCreativeModeTab", () -> ReliableEmiConfig.syncSelectedCreativeModeTab, v -> ReliableEmiConfig.syncSelectedCreativeModeTab = v),
                bool("showCreativeTabNameInSearchbar", () -> ReliableEmiConfig.showCreativeTabNameInSearchbar, v -> ReliableEmiConfig.showCreativeTabNameInSearchbar = v),
                EmiConfigSections.action(remi("configuration.disabledCreativeModeTabs.manage"),
                        remi("configuration.disabledCreativeModeTabs.tooltip"), Component.translatable("sampack_emitweaks.configuration.open"),
                        () -> true, screen -> Minecraft.getInstance().setScreen(new CreativeModeTabConfigScreen(screen))));
    }

    /** REMI's stack groups, and this mod's changes to them. */
    static List<Option<?>> stackGroups() {
        return List.of(
                bool("enableStackGroups", () -> ReliableEmiConfig.enableStackGroups, v -> ReliableEmiConfig.enableStackGroups = v),
                bool("stackGroupsIndex", () -> ReliableEmiConfig.stackGroupsIndex, v -> ReliableEmiConfig.stackGroupsIndex = v),
                bool("stackGroupsCraftables", () -> ReliableEmiConfig.stackGroupsCraftables, v -> ReliableEmiConfig.stackGroupsCraftables = v),
                bool("stackGroupsWorkstation", () -> ReliableEmiConfig.stackGroupsWorkstation, v -> ReliableEmiConfig.stackGroupsWorkstation = v),
                bool("stackGroupsFavorites", () -> ReliableEmiConfig.stackGroupsFavorites, v -> ReliableEmiConfig.stackGroupsFavorites = v),
                EmiConfigSections.iconStyle(ClientConfig.REMI_GROUP_ICON, "remi_group_icon"),
                bool("enableCreateStackGroupButton", () -> ReliableEmiConfig.enableCreateStackGroupButton, v -> ReliableEmiConfig.enableCreateStackGroupButton = v),
                EmiConfigSections.bool(ClientConfig.REMI_SKIP_BUILTIN_GROUPS, "skip_builtin_groups"),
                EmiConfigSections.action(remi("configuration.disabledStackGroups.manage"),
                        Minecraft.getInstance().level != null
                                ? remi("configuration.disabledStackGroups.tooltip")
                                : remi("configuration.disabledStackGroups.tooltip.unavailable"),
                        Component.translatable("sampack_emitweaks.configuration.open"),
                        () -> Minecraft.getInstance().level != null,
                        screen -> Minecraft.getInstance().setScreen(new StackGroupConfigScreen(screen))));
    }

    /**
     * REMI's "miscellaneous" settings, nearly all about sidebar pages (Better Cheat Mode goes next to
     * EMI's Cheat Mode instead). As in REMI's screen,
     * scrolling instead of pages always shows titles instead of page numbers.
     */
    static List<Option<?>> sidebarPages() {
        return List.of(
                bool("scrollInsteadOfPagination", () -> ReliableEmiConfig.scrollInsteadOfPagination, v -> ReliableEmiConfig.scrollInsteadOfPagination = v),
                bool("showTitleInsteadOfPageNumbers",
                        () -> ReliableEmiConfig.showTitleInsteadOfPageNumbers || ReliableEmiConfig.scrollInsteadOfPagination,
                        v -> ReliableEmiConfig.showTitleInsteadOfPageNumbers = v || ReliableEmiConfig.scrollInsteadOfPagination),
                bool("verticalScrollbar", () -> ReliableEmiConfig.verticalScrollbar, v -> ReliableEmiConfig.verticalScrollbar = v),
                bool("incrementalScrollbarFill", () -> ReliableEmiConfig.incrementalScrollbarFill, v -> ReliableEmiConfig.incrementalScrollbarFill = v),
                bool("disablePaginationWrapping", () -> ReliableEmiConfig.disablePaginationWrapping, v -> ReliableEmiConfig.disablePaginationWrapping = v),
                bool("hidePageButtonWhenOnePage", () -> ReliableEmiConfig.hidePageButtonWhenOnePage, v -> ReliableEmiConfig.hidePageButtonWhenOnePage = v),
                bool("emiOnlyInRecipeBook", () -> ReliableEmiConfig.emiOnlyInRecipeBook, v -> ReliableEmiConfig.emiOnlyInRecipeBook = v),
                bool("disableEmiGlobalConfig", () -> ReliableEmiConfig.disableEmiGlobalConfig, v -> ReliableEmiConfig.disableEmiGlobalConfig = v),
                EmiConfigSections.action(Component.translatable("sampack_emitweaks.configuration.remi_screen"),
                        Component.translatable("sampack_emitweaks.configuration.remi_screen.tooltip"),
                        Component.translatable("sampack_emitweaks.configuration.open"), () -> true,
                        screen -> Minecraft.getInstance().setScreen(ReliableEmiConfigScreen.createScreen(screen))));
    }

    private static Component remi(String key) {
        return Component.translatable("remi." + key);
    }

    private static Option<Boolean> bool(String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return EmiConfigSections.bool(remi("configuration." + key), remi("configuration." + key + ".tooltip"), getter, setter)
                .savedBy(SAVE_REMI);
    }

    private static Option<Integer> integer(String key, Supplier<Integer> getter, Consumer<Integer> setter) {
        return EmiConfigSections.integer(remi("configuration." + key), remi("configuration." + key + ".tooltip"),
                Integer.MIN_VALUE, Integer.MAX_VALUE, getter, setter).savedBy(SAVE_REMI);
    }

    /** Two of REMI's numbers in one row, under this mod's name for the pair; REMI has no minimums, so neither does this. */
    private static Option<List<Integer>> pair(String key, String first, String second,
                                              Supplier<Integer> get1, Consumer<Integer> set1,
                                              Supplier<Integer> get2, Consumer<Integer> set2) {
        return EmiConfigSections.pair(EmiConfigSections.text(key), EmiConfigSections.text(key + ".tooltip"), first, second,
                Integer.MIN_VALUE, Integer.MAX_VALUE, get1, set1, Integer.MIN_VALUE, Integer.MAX_VALUE, get2, set2).savedBy(SAVE_REMI);
    }

    private static <E extends Enum<E>> Option<E> choice(String key, Class<E> type, Supplier<E> getter, Consumer<E> setter) {
        return EmiConfigSections.choice(remi("configuration." + key), remi("configuration." + key + ".tooltip"), type,
                value -> remi("enum." + key + "." + value.name().toLowerCase(Locale.ROOT)), getter, setter).savedBy(SAVE_REMI);
    }
}
