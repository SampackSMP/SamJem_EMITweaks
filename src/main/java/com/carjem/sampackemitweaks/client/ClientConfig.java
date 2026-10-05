package com.carjem.sampackemitweaks.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config ({@code config/sampack_emitweaks-client.toml}): the icon export. */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue ICON_SIZE;
    public static final ModConfigSpec.IntValue MAX_SHEET_SIZE;
    public static final ModConfigSpec.IntValue ICONS_PER_FRAME;
    public static final ModConfigSpec.BooleanValue INCLUDE_NAMES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
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
}
