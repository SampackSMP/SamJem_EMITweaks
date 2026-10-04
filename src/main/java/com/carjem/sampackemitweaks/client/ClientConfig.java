package com.carjem.sampackemitweaks.client;

import com.carjem.sampackemitweaks.creative.CreativeLayout;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config ({@code config/sampack_emitweaks-client.toml}): the creative inventory size and the icon export. */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue COLUMNS;
    public static final ModConfigSpec.IntValue ROWS;
    public static final ModConfigSpec.BooleanValue FIT_TO_SCREEN;
    public static final ModConfigSpec.IntValue ICON_SIZE;
    public static final ModConfigSpec.IntValue MAX_SHEET_SIZE;
    public static final ModConfigSpec.IntValue ICONS_PER_FRAME;
    public static final ModConfigSpec.BooleanValue INCLUDE_NAMES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.translation("sampack_emitweaks.configuration.creative_inventory").push("creative_inventory");
        COLUMNS = builder
                .comment("Item columns in the creative inventory. Vanilla is 9. Wider screens also fit more tabs per row.")
                .translation("sampack_emitweaks.configuration.columns")
                .defineInRange("columns", CreativeLayout.VANILLA_COLUMNS, CreativeLayout.VANILLA_COLUMNS, CreativeLayout.MAX_COLUMNS);
        ROWS = builder
                .comment("Item rows in the creative inventory, not counting the hotbar. Vanilla is 5.")
                .translation("sampack_emitweaks.configuration.rows")
                .defineInRange("rows", CreativeLayout.VANILLA_ROWS, CreativeLayout.VANILLA_ROWS, CreativeLayout.MAX_ROWS);
        FIT_TO_SCREEN = builder
                .comment("Shrink the columns and rows (never below vanilla) when the window is too small for them.")
                .translation("sampack_emitweaks.configuration.fit_to_screen")
                .define("fit_to_screen", true);
        builder.pop();

        builder.translation("sampack_emitweaks.configuration.icon_export").push("icon_export");
        ICON_SIZE = builder
                .comment("Icon size in pixels when /icondump export or update is given none.")
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
}
