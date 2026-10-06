package com.carjem.sampackemitweaks.tabs;

import com.carjem.sampackemitweaks.SampackEmiTweaks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Forgets the tab builds ModernFix has memoized, so the next build of each tab really runs.
 *
 * ModernFix's mixin.perf.memoize_creative_tab_build (on by default) wraps
 * CreativeModeTab.buildContents and skips it whenever the parameters match the ones the tab was
 * last built with. A tab rules reload rebuilds with the very same parameters, so without this every
 * tab keeps the contents it had at startup. The memo is two @Unique fields ModernFix merges into
 * CreativeModeTab: each tab's last parameters, and a static flag for the non-category tabs.
 * Mixin may rename a @Unique field, so they are found by name suffix and type. Without
 * ModernFix, or with the option off, neither exists and this does nothing.
 */
final class TabBuildMemo {
    private static final @Nullable Field PARAMETERS = find("oldParameters", CreativeModeTab.ItemDisplayParameters.class, false);
    private static final @Nullable Field REBUILT = find("REBUILT_NON_CATEGORY", boolean.class, true);

    private TabBuildMemo() {
    }

    static void forget() {
        try {
            if (PARAMETERS != null) {
                for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
                    PARAMETERS.set(tab, null);
                }
            }
            if (REBUILT != null) {
                REBUILT.setBoolean(null, false);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            SampackEmiTweaks.LOGGER.warn("Could not reset ModernFix's memoized creative tab builds; "
                    + "the tabs may keep their old contents until a restart", e);
        }
    }

    private static @Nullable Field find(String suffix, Class<?> type, boolean isStatic) {
        for (Field field : CreativeModeTab.class.getDeclaredFields()) {
            if (field.getName().endsWith(suffix) && field.getType() == type
                    && Modifier.isStatic(field.getModifiers()) == isStatic) {
                try {
                    field.setAccessible(true);
                    return field;
                } catch (RuntimeException e) {
                    SampackEmiTweaks.LOGGER.warn("Cannot access {}; creative tab reloads may not apply", field, e);
                    return null;
                }
            }
        }
        return null;
    }
}
