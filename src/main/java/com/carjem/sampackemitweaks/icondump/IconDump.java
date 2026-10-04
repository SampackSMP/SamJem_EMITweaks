package com.carjem.sampackemitweaks.icondump;

import com.carjem.sampackemitweaks.icondump.command.ExportCommand;
import com.carjem.sampackemitweaks.icondump.data.DataCommand;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Everything InvIndexLedger reads from the game, under /icondump (merged in from SamJem: IconDump).
 * Icons: every stack rendered into a few spritesheets plus a meta.json keyed by EMI stack id, the
 * job IconExporter does with a different output (see ExportJob). Data: EMI's stack list, the item
 * registry and tags, the chipped workstation recipes and the creative tab ids (see DataCommand).
 * Client only; its settings are the icon_export section of {@link com.carjem.sampackemitweaks.client.ClientConfig}.
 */
public final class IconDump {
    public static final Logger LOG = LoggerFactory.getLogger("Sampack EMI Tweaks/IconDump");

    private IconDump() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ExportCommand::register);
        NeoForge.EVENT_BUS.addListener(DataCommand::register);
    }
}
