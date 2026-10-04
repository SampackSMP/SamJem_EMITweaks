package com.carjem.sampackemitweaks.icondump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Everything InvIndexLedger reads from the game, under /emitweaks export and update_icons (merged in
 * from SamJem: IconDump; see {@link com.carjem.sampackemitweaks.client.EmiTweaksCommands}).
 * Icons: every stack rendered into a few spritesheets plus a meta.json keyed by EMI stack id, the
 * job IconExporter does with a different output (see ExportJob). Data: EMI's stack list, the item
 * registry and tags, the chipped workstation recipes and the creative tab ids (see DataCommand).
 * Client only; its settings are the icon_export section of {@link com.carjem.sampackemitweaks.client.ClientConfig}.
 */
public final class IconDump {
    public static final Logger LOG = LoggerFactory.getLogger("Sampack EMI Tweaks/IconDump");

    private IconDump() {
    }
}
