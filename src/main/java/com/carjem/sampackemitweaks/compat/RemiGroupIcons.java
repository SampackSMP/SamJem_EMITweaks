package com.carjem.sampackemitweaks.compat;

import com.carjem.sampackemitweaks.creative.GroupIcon;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The {@code icon} of each REMI stack group whose json names one (see {@link GroupIcon}). REMI
 * ignores the key; {@link com.carjem.sampackemitweaks.mixin.compat.RemiStackGroupIconMixin} reads
 * it as the group loads, on EMI's reload thread, and the render thread reads it back. Keyed by the
 * group object (REMI's groups use identity equality), so a reload's old groups drop out by
 * themselves.
 */
public final class RemiGroupIcons {
    private static final Map<Object, GroupIcon> ICONS = Collections.synchronizedMap(new WeakHashMap<>());

    private RemiGroupIcons() {
    }

    public static void put(Object group, @Nullable GroupIcon icon) {
        if (icon != null) {
            ICONS.put(group, icon);
        } else {
            ICONS.remove(group);
        }
    }

    @Nullable
    public static GroupIcon get(Object group) {
        return ICONS.get(group);
    }
}
