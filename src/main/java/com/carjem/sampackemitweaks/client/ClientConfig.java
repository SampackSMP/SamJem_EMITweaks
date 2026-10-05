package com.carjem.sampackemitweaks.client;

import com.carjem.sampackemitweaks.creative.GroupIcon;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config ({@code config/sampack_emitweaks-client.toml}): EMI's search bar, the creative
 * inventory, the REMI tweaks and the icon export. Also edited from EMI's config screen ({@link com.carjem.sampackemitweaks.config.EmiConfigSections}).
 */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CLEAR_SEARCH_ON_TAB_SWITCH;
    public static final ModConfigSpec.BooleanValue CLEAR_SEARCH_ON_CLOSE;
    public static final ModConfigSpec.BooleanValue CLEAR_SEARCH_BUTTON;
    public static final ModConfigSpec.BooleanValue SEARCH_HISTORY_DROPDOWN;
    public static final ModConfigSpec.IntValue SEARCH_HISTORY_ROWS;
    public static final ModConfigSpec.BooleanValue SEARCH_HISTORY_FILTER;
    public static final ModConfigSpec.BooleanValue SAVE_SEARCH_HISTORY;
    public static final ModConfigSpec.IntValue SEARCH_HISTORY_SIZE;
    public static final ModConfigSpec.IntValue CREATIVE_ROWS;
    public static final ModConfigSpec.BooleanValue CREATIVE_FIT_TO_SCREEN;
    public static final ModConfigSpec.BooleanValue CREATIVE_GROUPS;
    public static final ModConfigSpec.BooleanValue CREATIVE_FOLLOW_SEARCH;
    public static final ModConfigSpec.BooleanValue CREATIVE_FOCUS_SEARCH;
    public static final ModConfigSpec.EnumValue<GroupIcon.Style> CREATIVE_GROUP_ICON;
    public static final ModConfigSpec.BooleanValue REMI_SKIP_BUILTIN_GROUPS;
    public static final ModConfigSpec.EnumValue<GroupIcon.Style> REMI_GROUP_ICON;
    public static final ModConfigSpec.IntValue ICON_SIZE;
    public static final ModConfigSpec.IntValue MAX_SHEET_SIZE;
    public static final ModConfigSpec.IntValue ICONS_PER_FRAME;
    public static final ModConfigSpec.BooleanValue INCLUDE_NAMES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.translation("sampack_emitweaks.configuration.search").push("search");
        CLEAR_SEARCH_ON_TAB_SWITCH = builder
                .comment("Clear EMI's search bar when the creative inventory switches to another tab.")
                .translation("sampack_emitweaks.configuration.clear_on_tab_switch")
                .define("clear_on_tab_switch", true);
        CLEAR_SEARCH_ON_CLOSE = builder
                .comment("Clear EMI's search bar when an inventory screen (creative or any other) is closed.",
                        "Going to EMI's recipe screens and back doesn't count.")
                .translation("sampack_emitweaks.configuration.clear_on_close")
                .define("clear_on_close", true);
        CLEAR_SEARCH_BUTTON = builder
                .comment("Show an x at the end of EMI's search bar that clears it.")
                .translation("sampack_emitweaks.configuration.clear_button")
                .define("clear_button", true);
        SEARCH_HISTORY_DROPDOWN = builder
                .comment("Show an arrow at the end of EMI's search bar that opens a list of recent searches.",
                        "Left-click one to search it again, right-click to forget it.")
                .translation("sampack_emitweaks.configuration.history_dropdown")
                .define("history_dropdown", true);
        SEARCH_HISTORY_ROWS = builder
                .comment("How many searches the history list shows at once; scroll it for the rest.")
                .translation("sampack_emitweaks.configuration.history_rows")
                .defineInRange("history_rows", 8, 3, 20);
        SEARCH_HISTORY_FILTER = builder
                .comment("Narrow the history list to the searches that contain what is typed in the search bar.")
                .translation("sampack_emitweaks.configuration.history_filter")
                .define("history_filter", false);
        SAVE_SEARCH_HISTORY = builder
                .comment("Keep the search history across restarts, in config/sampack_emitweaks/search_history.json.")
                .translation("sampack_emitweaks.configuration.save_history")
                .define("save_history", true);
        SEARCH_HISTORY_SIZE = builder
                .comment("How many recent searches are kept. A search is added when the search bar loses focus",
                        "and its text differs from the last one; 0 turns the history off.")
                .translation("sampack_emitweaks.configuration.history_size")
                .defineInRange("history_size", 20, 0, 100);
        builder.pop();

        builder.translation("sampack_emitweaks.configuration.creative").push("creative");
        CREATIVE_ROWS = builder
                .comment("Item rows in the creative inventory, at most (vanilla: 5). Takes effect when it next opens.")
                .translation("sampack_emitweaks.configuration.rows")
                .defineInRange("rows", 20, 5, 20);
        CREATIVE_FIT_TO_SCREEN = builder
                .comment("Shrink the rows (never below vanilla's 5) to what the window has room for.")
                .translation("sampack_emitweaks.configuration.fit_to_screen")
                .define("fit_to_screen", true);
        CREATIVE_GROUPS = builder
                .comment("Fold items into REMI's stack groups, which open and close on click.")
                .translation("sampack_emitweaks.configuration.groups")
                .define("groups", true);
        CREATIVE_FOLLOW_SEARCH = builder
                .comment("Filter the creative tabs, and the search tab's EMI index, by EMI's search bar.")
                .translation("sampack_emitweaks.configuration.follow_search")
                .define("follow_search", true);
        CREATIVE_FOCUS_SEARCH = builder
                .comment("Opening the search tab puts the keyboard focus in EMI's search bar.")
                .translation("sampack_emitweaks.configuration.focus_search")
                .define("focus_search", true);
        CREATIVE_GROUP_ICON = builder
                .comment("What a collapsed group shows in the creative inventory when it names no icon.")
                .translation("sampack_emitweaks.configuration.creative_group_icon")
                .defineEnum("default_group_icon", GroupIcon.Style.FIRST);
        builder.pop();

        builder.translation("sampack_emitweaks.configuration.remi_tweaks").push("remi");
        REMI_SKIP_BUILTIN_GROUPS = builder
                .comment("Leave out the stack groups REMI ships in its own jar; the pack's groups and other mods'",
                        "still load. Takes effect when EMI next reloads.")
                .translation("sampack_emitweaks.configuration.skip_builtin_groups")
                .define("skip_builtin_groups", true);
        REMI_GROUP_ICON = builder
                .comment("What a collapsed stack group shows in EMI's panels when it names no icon.")
                .translation("sampack_emitweaks.configuration.remi_group_icon")
                .defineEnum("default_group_icon", GroupIcon.Style.STACKED);
        builder.pop();

        builder.translation("sampack_emitweaks.configuration.icon_export").push("icon_export");
        ICON_SIZE = builder
                .comment("Icon size in pixels when /emitweaks export or update_icons is given none.")
                .translation("sampack_emitweaks.configuration.default_size")
                .defineInRange("default_size", 32, 1, 512);
        MAX_SHEET_SIZE = builder
                .comment("Largest width and height of one sheet, in pixels. A sheet holds (max_sheet_size / size)^2",
                        "icons; 2048 at 32px is 4096 icons per sheet. Capped at what the GPU supports.")
                .translation("sampack_emitweaks.configuration.max_sheet_size")
                .defineInRange("max_sheet_size", 2048, 16, 16384);
        ICONS_PER_FRAME = builder
                .comment("How many icons are rendered each frame. Higher is faster, until frames get long",
                        "enough that the progress screen stutters.")
                .translation("sampack_emitweaks.configuration.icons_per_frame")
                .defineInRange("icons_per_frame", 64, 1, 4096);
        INCLUDE_NAMES = builder
                .comment("Also write each stack's display name into meta.json.")
                .translation("sampack_emitweaks.configuration.include_names")
                .define("include_names", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    /** The value, or its default before the config file has loaded. */
    public static boolean get(ModConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /** The value, or its default before the config file has loaded. */
    public static int get(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /** The value, or its default before the config file has loaded. */
    public static <E extends Enum<E>> E get(ModConfigSpec.EnumValue<E> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
