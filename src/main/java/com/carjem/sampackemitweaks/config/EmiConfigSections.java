package com.carjem.sampackemitweaks.config;

import com.carjem.sampackemitweaks.SampackEmiTweaks;
import com.carjem.sampackemitweaks.client.ClientConfig;
import com.carjem.sampackemitweaks.creative.GroupIcon;
import com.carjem.sampackemitweaks.search.SearchHistory;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.config.IntGroup;
import dev.emi.emi.screen.ConfigScreen;
import dev.emi.emi.screen.widget.config.BooleanWidget;
import dev.emi.emi.screen.widget.config.ConfigEntryWidget;
import dev.emi.emi.screen.widget.config.ConfigJumpButton;
import dev.emi.emi.screen.widget.config.GroupNameWidget;
import dev.emi.emi.screen.widget.config.IntGroupWidget;
import dev.emi.emi.screen.widget.config.IntWidget;
import dev.emi.emi.screen.widget.config.ListWidget;
import dev.emi.emi.screen.widget.config.SubGroupNameWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import it.unimi.dsi.fastutil.ints.IntList;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * This mod's settings and REMI's, merged into EMI's own config screen by topic: into EMI's
 * General, UI and Dev groups, right after one of EMI's rows (Better Cheat Mode after Cheat Mode),
 * added to one of EMI's subgroups (General → Search), or as new subgroups with their own jump bar
 * buttons, drawn in the style of EMI's. Values that come in pairs share a row. One
 * instance per screen: it remembers each value as the screen opened, so EMI's revert button and
 * its change count cover these settings too, and only the files with a change are saved when the
 * screen closes. {@link com.carjem.sampackemitweaks.mixin.emi.EmiConfigScreenMixin} wires it in.
 */
public final class EmiConfigSections {
    /** Where the mod list's config buttons open the screen: this mod's search bar settings, REMI's creative tabs. */
    public static final String OWN = "general.search-bar";
    public static final String REMI = "ui.creative-tabs";

    /** The jump bar icons, laid out in a 256x256 sheet like EMI's config.png, 16px apart in one row. */
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(SampackEmiTweaks.MOD_ID, "textures/gui/config.png");
    static final int ICON_SEARCH_BAR = 0;
    static final int ICON_HISTORY = 16;
    static final int ICON_TAGS = 32;
    static final int ICON_CREATIVE = 48;
    static final int ICON_TABS = 64;
    static final int ICON_GROUPS = 80;
    static final int ICON_PAGES = 96;
    static final int ICON_EXPORT = 112;
    /** For a subgroup that is one of EMI's own, which already has a button. */
    static final int NO_ICON = -1;

    @Nullable private static String pendingJump;

    private final List<Subgroup> subgroups = new ArrayList<>();

    public EmiConfigSections() {
        Subgroups.add(subgroups);
    }

    /** EMI's config screen, scrolled to a group or subgroup by id. */
    public static Screen open(Screen parent, String jump) {
        pendingJump = jump;
        return new ConfigScreen(parent);
    }

    // ---- Model ----

    /**
     * Rows that go into EMI's group {@code parent}: right after EMI's own row named by the
     * translation key {@code after} when that is set, else into its existing subgroup {@code id}
     * when EMI has one, else into a new subgroup at the end of the group.
     */
    record Subgroup(String parent, String id, Component title, int icon, List<Option<?>> options, @Nullable String after) {
        Subgroup(String parent, String id, Component title, int icon, List<Option<?>> options) {
            this(parent, id, title, icon, options, null);
        }
    }

    /** Saves this mod's client config, and reapplies the search history settings. */
    static final Runnable SAVE_OWN = () -> {
        ClientConfig.SPEC.save();
        SearchHistory.settingsChanged();
    };

    /** One row: a value with what it was when the screen opened, or an action without one. */
    abstract static class Option<T> {
        final Component name;
        final List<ClientTooltipComponent> tooltip;
        final Supplier<T> getter;
        final Consumer<T> setter;
        final T original;
        // What writes this value to its file; options in the same file share one instance.
        Runnable save = SAVE_OWN;

        Option(Component name, @Nullable Component tooltip, Supplier<T> getter, Consumer<T> setter) {
            this.name = name;
            this.tooltip = tooltip(tooltip);
            this.getter = getter;
            this.setter = setter;
            this.original = getter.get();
        }

        Option<T> savedBy(Runnable save) {
            this.save = save;
            return this;
        }

        boolean changed() {
            return !Objects.equals(getter.get(), original);
        }

        void revert() {
            if (changed()) setter.accept(original);
        }

        abstract ConfigEntryWidget widget(ConfigScreen screen, Supplier<String> search);
    }

    static Option<Boolean> bool(Component name, @Nullable Component tooltip, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return new Option<>(name, tooltip, getter, setter) {
            @Override
            ConfigEntryWidget widget(ConfigScreen screen, Supplier<String> search) {
                return new BooleanWidget(name, this.tooltip, search, screen.new Mutator<Boolean>() {
                    @Override
                    protected Boolean getValue() {
                        return getter.get();
                    }

                    @Override
                    protected void setValue(Boolean value) {
                        setter.accept(value);
                    }
                });
            }
        };
    }

    static Option<Integer> integer(Component name, @Nullable Component tooltip, int min, int max,
                                   Supplier<Integer> getter, Consumer<Integer> setter) {
        return new Option<>(name, tooltip, getter, setter) {
            @Override
            ConfigEntryWidget widget(ConfigScreen screen, Supplier<String> search) {
                return new IntWidget(name, this.tooltip, search, screen.new Mutator<Integer>() {
                    @Override
                    protected Integer getValue() {
                        return getter.get();
                    }

                    @Override
                    protected void setValue(Integer value) {
                        setter.accept(Mth.clamp(value, min, max));
                    }
                });
            }
        };
    }

    static <E extends Enum<E>> Option<E> choice(Component name, @Nullable Component tooltip, Class<E> type,
                                               Function<E, Component> label, Supplier<E> getter, Consumer<E> setter) {
        return new Option<>(name, tooltip, getter, setter) {
            @Override
            ConfigEntryWidget widget(ConfigScreen screen, Supplier<String> search) {
                return new ConfigWidgets.Cycle<>(name, this.tooltip, search, List.of(type.getEnumConstants()), getter,
                        value -> {
                            setter.accept(value);
                            screen.updateChanges();
                        }, label);
            }
        };
    }

    /** Two numbers in one row, in EMI's own widget for its sidebar sizes; hovering one shows its {@code field.<name>}. */
    static Option<List<Integer>> pair(Component name, @Nullable Component tooltip, String first, String second,
                                      int min1, int max1, Supplier<Integer> get1, Consumer<Integer> set1,
                                      int min2, int max2, Supplier<Integer> get2, Consumer<Integer> set2) {
        return new Option<>(name, tooltip, () -> List.of(get1.get(), get2.get()), values -> {
            set1.accept(values.get(0));
            set2.accept(values.get(1));
        }) {
            @Override
            ConfigEntryWidget widget(ConfigScreen screen, Supplier<String> search) {
                IntGroup group = new IntGroup("sampack_emitweaks.configuration.field.", List.of(first, second),
                        IntList.of(get1.get(), get2.get()));
                return new IntGroupWidget(name, this.tooltip, search, screen.new Mutator<IntGroup>() {
                    @Override
                    protected IntGroup getValue() {
                        return group;
                    }

                    @Override
                    protected void setValue(IntGroup value) {
                        set1.accept(Mth.clamp(value.values.getInt(0), min1, max1));
                        set2.accept(Mth.clamp(value.values.getInt(1), min2, max2));
                    }
                });
            }
        };
    }

    /** Colors in one row; hovering one shows its {@code field.<name>}. */
    static Option<List<Integer>> colors(Component name, @Nullable Component tooltip, List<String> names,
                                        List<Supplier<Integer>> getters, List<Consumer<Integer>> setters) {
        return new Option<>(name, tooltip, () -> getters.stream().map(Supplier::get).toList(), values -> {
            for (int i = 0; i < setters.size(); i++) setters.get(i).accept(values.get(i));
        }) {
            @Override
            ConfigEntryWidget widget(ConfigScreen screen, Supplier<String> search) {
                List<IntSupplier> ints = getters.stream().<IntSupplier>map(getter -> getter::get).toList();
                List<IntConsumer> sets = setters.stream().<IntConsumer>map(setter -> value -> {
                    setter.accept(value);
                    screen.updateChanges();
                }).toList();
                return new ConfigWidgets.Colors(name, this.tooltip, search,
                        names.stream().map(field -> (Component) text("field." + field)).toList(), ints, sets);
            }
        };
    }

    /** A button; {@code action} gets EMI's config screen, to return to. */
    static Option<Void> action(Component name, @Nullable Component tooltip, Component label,
                               BooleanSupplier available, Consumer<Screen> action) {
        return new Option<>(name, tooltip, () -> null, value -> { }) {
            @Override
            ConfigEntryWidget widget(ConfigScreen screen, Supplier<String> search) {
                return new ConfigWidgets.Action(name, this.tooltip, search, label, available, () -> action.accept(screen));
            }
        };
    }

    static Option<Boolean> bool(ModConfigSpec.BooleanValue value, String key) {
        return bool(text(key), text(key + ".tooltip"), value::get, value::set);
    }

    static Option<Integer> integer(ModConfigSpec.IntValue value, String key, int min, int max) {
        return integer(text(key), text(key + ".tooltip"), min, max, value::get, value::set);
    }

    static Option<List<Integer>> pair(ModConfigSpec.IntValue first, ModConfigSpec.IntValue second, String key,
                                      String firstName, String secondName, int min1, int max1, int min2, int max2) {
        return pair(text(key), text(key + ".tooltip"), firstName, secondName,
                min1, max1, first::get, first::set, min2, max2, second::get, second::set);
    }

    static Option<GroupIcon.Style> iconStyle(ModConfigSpec.EnumValue<GroupIcon.Style> value, String key) {
        return choice(text(key), text(key + ".tooltip"), GroupIcon.Style.class,
                style -> text("group_icon." + style.name().toLowerCase(Locale.ROOT)), value::get, value::set);
    }

    static Component text(String key) {
        return Component.translatable("sampack_emitweaks.configuration." + key);
    }

    /** A tooltip wrapped to EMI's config tooltip width; a key without a translation gives none. */
    private static List<ClientTooltipComponent> tooltip(@Nullable Component text) {
        if (text == null) return List.of();
        if (text.getContents() instanceof TranslatableContents translatable && !I18n.exists(translatable.getKey())) {
            return List.of();
        }
        return Minecraft.getInstance().font.split(text, 220).stream().map(ClientTooltipComponent::create).toList();
    }

    // ---- What goes where ----

    /** The subgroups, by topic, mixing this mod's rows and REMI's. */
    private static final class Subgroups {
        static void add(List<Subgroup> out) {
            out.add(new Subgroup("general", "general.cheat-mode", Component.empty(), NO_ICON,
                    RemiConfigOptions.cheatMode(), "config.emi.general.cheat_mode"));

            out.add(new Subgroup("general", "general.search", Component.empty(), NO_ICON, RemiConfigOptions.search()));

            List<Option<?>> searchBar = new ArrayList<>(List.of(
                    bool(ClientConfig.CLEAR_SEARCH_BUTTON, "clear_button"),
                    bool(ClientConfig.CLEAR_SEARCH_ON_TAB_SWITCH, "clear_on_tab_switch"),
                    bool(ClientConfig.CLEAR_SEARCH_ON_CLOSE, "clear_on_close")));
            searchBar.addAll(RemiConfigOptions.searchBar());
            out.add(new Subgroup("general", "general.search-bar", text("search"), ICON_SEARCH_BAR, searchBar));

            out.add(new Subgroup("general", "general.search-history", text("search_history"), ICON_HISTORY, List.of(
                    bool(ClientConfig.SEARCH_HISTORY_DROPDOWN, "history_dropdown"),
                    pair(ClientConfig.SEARCH_HISTORY_ROWS, ClientConfig.SEARCH_HISTORY_SIZE, "history_length",
                            "shown", "kept", 3, 20, 0, 100),
                    bool(ClientConfig.SEARCH_HISTORY_FILTER, "history_filter"),
                    bool(ClientConfig.SAVE_SEARCH_HISTORY, "save_history"),
                    action(text("clear_history"), text("clear_history.tooltip"), text("clear_history.button"),
                            () -> !SearchHistory.entries().isEmpty(), screen -> SearchHistory.clearAll()))));

            out.add(new Subgroup("general", "general.tags", text("tags"), ICON_TAGS, RemiConfigOptions.tags()));

            out.add(new Subgroup("ui", "ui.creative-inventory", text("creative"), ICON_CREATIVE, List.of(
                    integer(ClientConfig.CREATIVE_ROWS, "rows", 5, 20),
                    bool(ClientConfig.CREATIVE_FIT_TO_SCREEN, "fit_to_screen"),
                    bool(ClientConfig.CREATIVE_FOLLOW_SEARCH, "follow_search"),
                    bool(ClientConfig.CREATIVE_FOCUS_SEARCH, "focus_search"),
                    bool(ClientConfig.CREATIVE_GROUPS, "groups"),
                    iconStyle(ClientConfig.CREATIVE_GROUP_ICON, "creative_group_icon"))));

            out.add(new Subgroup("ui", "ui.creative-tabs", text("creative_tabs"), ICON_TABS, RemiConfigOptions.creativeTabs()));
            out.add(new Subgroup("ui", "ui.stack-groups", text("stack_groups"), ICON_GROUPS, RemiConfigOptions.stackGroups()));
            out.add(new Subgroup("ui", "ui.sidebar-pages", text("sidebar_pages"), ICON_PAGES, RemiConfigOptions.sidebarPages()));

            out.add(new Subgroup("dev", "dev.icon-export", text("icon_export"), ICON_EXPORT, List.of(
                    pair(ClientConfig.ICON_SIZE, ClientConfig.MAX_SHEET_SIZE, "export_sizes", "icon", "sheet", 1, 512, 16, 16384),
                    integer(ClientConfig.ICONS_PER_FRAME, "icons_per_frame", 1, 4096),
                    bool(ClientConfig.INCLUDE_NAMES, "include_names"))));
        }
    }

    // ---- Screen hooks ----

    /** Changed settings since the screen opened, for EMI's revert button. */
    public int changes() {
        int changes = 0;
        for (Subgroup subgroup : subgroups) {
            for (Option<?> option : subgroup.options()) {
                if (option.changed()) changes++;
            }
        }
        return changes;
    }

    public void revert() {
        subgroups.forEach(subgroup -> subgroup.options().forEach(Option::revert));
    }

    /** Saves each file with a change, once. Runs after EMI has written its own config (REMI's save reloads it). */
    public void save() {
        Set<Runnable> saves = new LinkedHashSet<>();
        for (Subgroup subgroup : subgroups) {
            for (Option<?> option : subgroup.options()) {
                if (option.changed()) saves.add(option.save);
            }
        }
        for (Runnable save : saves) {
            try {
                save.run();
            } catch (RuntimeException e) {
                SampackEmiTweaks.LOGGER.error("Couldn't save settings from EMI's config screen", e);
            }
        }
    }

    /**
     * Puts the rows into EMI's list, which EMI has just filled: into EMI's own subgroup where one
     * has the id, joined onto its tree line, else as a new subgroup at the end of the parent group.
     *
     * @param collapsed the headings collapsed before the screen was rebuilt, by text
     */
    public void addEntries(ConfigScreen screen, ListWidget list, Supplier<String> search, Set<String> collapsed) {
        List<ListWidget.Entry> entries = list.children();
        for (Subgroup subgroup : subgroups) {
            GroupNameWidget parent = findGroup(entries, subgroup.parent(), false);
            if (parent == null || subgroup.options().isEmpty()) continue;

            ConfigEntryWidget anchor = subgroup.after() != null ? findEntry(entries, subgroup.after()) : null;
            if (anchor != null) {
                insertAfter(screen, list, search, anchor, subgroup.options());
                continue;
            }

            GroupNameWidget sub = findGroup(entries, subgroup.id(), true);
            EmiConfig.ConfigGroup tree = treeLine(subgroup.id());
            int at;
            if (sub != null) {
                // After the subgroup's last row, which is then no longer the end of its tree line.
                at = entries.indexOf(sub) + 1;
                for (int i = at; i < entries.size(); i++) {
                    if (!(entries.get(i) instanceof ConfigEntryWidget entry) || !entry.parentGroups.contains(sub)) break;
                    at = i + 1;
                    if (entry.group != null) tree = entry.group;
                    entry.endGroup = false;
                }
            } else {
                at = endOfGroup(entries, parent);
                SubGroupNameWidget created = new SubGroupNameWidget(subgroup.id(), subgroup.title());
                created.collapsed = collapsed.contains(subgroup.title().getString());
                created.parent = parent;
                created.parentList = list;
                entries.add(at++, created);
                sub = created;
            }

            List<Option<?>> options = subgroup.options();
            for (int i = 0; i < options.size(); i++) {
                ConfigEntryWidget entry = options.get(i).widget(screen, search);
                entry.group = tree;
                entry.endGroup = i == options.size() - 1;
                entry.parentGroups.add(parent);
                entry.parentGroups.add(sub);
                parent.children.add(entry);
                sub.children.add(entry);
                entry.parentList = list;
                entries.add(at++, entry);
            }
        }
    }

    /**
     * Puts rows right after one of EMI's, as part of whatever group, subgroup and tree line it
     * belongs to; the last of them takes over its end of the tree line.
     */
    private static void insertAfter(ConfigScreen screen, ListWidget list, Supplier<String> search,
                                    ConfigEntryWidget anchor, List<Option<?>> options) {
        List<ListWidget.Entry> entries = list.children();
        int at = entries.indexOf(anchor) + 1;
        boolean endGroup = anchor.endGroup;
        anchor.endGroup = false;
        for (int i = 0; i < options.size(); i++) {
            ConfigEntryWidget entry = options.get(i).widget(screen, search);
            entry.group = anchor.group;
            entry.endGroup = endGroup && i == options.size() - 1;
            for (GroupNameWidget group : anchor.parentGroups) {
                entry.parentGroups.add(group);
                group.children.add(entry);
            }
            entry.parentList = list;
            entries.add(at++, entry);
        }
    }

    /** EMI's row whose name is the translation of {@code key}, matched by its text as EMI shows it. */
    @Nullable
    private static ConfigEntryWidget findEntry(List<ListWidget.Entry> entries, String key) {
        String name = I18n.get(key);
        for (ListWidget.Entry entry : entries) {
            if (entry instanceof ConfigEntryWidget row && row.getSearchableText().equals(name)) return row;
        }
        return null;
    }

    @Nullable
    private static GroupNameWidget findGroup(List<ListWidget.Entry> entries, String id, boolean subgroup) {
        for (ListWidget.Entry entry : entries) {
            if (entry instanceof GroupNameWidget group && (group instanceof SubGroupNameWidget) == subgroup && group.id.equals(id)) {
                return group;
            }
        }
        return null;
    }

    /** Where the group's rows end: at the next top-level group, or the end of the list. */
    private static int endOfGroup(List<ListWidget.Entry> entries, GroupNameWidget group) {
        for (int i = entries.indexOf(group) + 1; i < entries.size(); i++) {
            if (entries.get(i) instanceof GroupNameWidget next && !(next instanceof SubGroupNameWidget)) return i;
        }
        return entries.size();
    }

    /** Scrolls to the group or subgroup {@link #open} asked for, if any. */
    public static void jump(ListWidget list) {
        String id = pendingJump;
        pendingJump = null;
        if (id == null) return;
        int y = 0;
        for (ListWidget.Entry entry : list.children()) {
            if (entry instanceof GroupNameWidget group && group.id.equals(id)) {
                list.setScrollAmount(y);
                return;
            }
            int height = entry.getHeight();
            if (height > 0) y += height + list.padding;
        }
    }

    // ---- Jump bar ----

    /** Adds the new subgroups' ids to EMI's jump bar list, each after the last button of its parent group. */
    public void addJumpIds(List<String> jumps) {
        for (Subgroup subgroup : subgroups) {
            if (subgroup.icon() == NO_ICON) continue;
            int at = -1;
            for (int i = 0; i < jumps.size(); i++) {
                String jump = jumps.get(i);
                if (jump.equals(subgroup.parent()) || jump.startsWith(subgroup.parent() + ".")) at = i;
            }
            jumps.add(at < 0 ? jumps.size() : at + 1, subgroup.id());
        }
    }

    /** The new subgroups' buttons, for EMI to drop first when its jump bar runs out of room. */
    public List<String> jumpRemovals() {
        return subgroups.stream().filter(subgroup -> subgroup.icon() != NO_ICON).map(Subgroup::id).toList();
    }

    /** A jump button for one of the new subgroups, or null for one of EMI's. */
    @Nullable
    public ConfigJumpButton jumpButton(String id, int x, int y, Button.OnPress action) {
        for (Subgroup subgroup : subgroups) {
            if (subgroup.icon() != NO_ICON && subgroup.id().equals(id)) {
                return new JumpButton(x, y, subgroup.icon(), action, subgroup.title());
            }
        }
        return null;
    }

    /** EMI's own jump button, drawing from this mod's icon sheet instead of EMI's. */
    private static final class JumpButton extends ConfigJumpButton {
        JumpButton(int x, int y, int u, OnPress action, Component title) {
            super(x, y, u, 0, action, List.of(title));
            this.texture = ICONS;
        }
    }

    /**
     * EMI draws a row's tree line when the row has a config group, an annotation it reads off its
     * own fields. Only whether it's set matters.
     */
    private static EmiConfig.ConfigGroup treeLine(String id) {
        return new EmiConfig.ConfigGroup() {
            @Override
            public String value() {
                return id;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return EmiConfig.ConfigGroup.class;
            }
        };
    }
}
