package com.carjem.sampackemitweaks.creative;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config ({@code config/sampack_emitweaks-client.toml}) for the creative inventory size. */
public final class CreativeLayoutConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue COLUMNS;
    public static final ModConfigSpec.IntValue ROWS;
    public static final ModConfigSpec.BooleanValue FIT_TO_SCREEN;

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
        SPEC = builder.build();
    }

    private CreativeLayoutConfig() {
    }
}
