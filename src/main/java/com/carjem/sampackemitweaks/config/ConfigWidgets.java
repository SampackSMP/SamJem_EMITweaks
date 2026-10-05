package com.carjem.sampackemitweaks.config;

import dev.emi.emi.screen.widget.config.ConfigEntryWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Rows for EMI's config screen that EMI has no widget for: a button that runs something, a button
 * that cycles through a list of values, and hex color fields. Laid out like EMI's own rows, with
 * the control 150px wide at the right.
 */
final class ConfigWidgets {
    private static final int CONTROL_WIDTH = 150;

    private ConfigWidgets() {
    }

    /** A button that runs an action; greyed out while it isn't available. */
    static final class Action extends ConfigEntryWidget {
        private final Button button;
        private final BooleanSupplier available;

        Action(Component name, List<ClientTooltipComponent> tooltip, Supplier<String> search,
               Component label, BooleanSupplier available, Runnable action) {
            super(name, tooltip, search, 20);
            this.available = available;
            this.button = Button.builder(label, b -> action.run()).size(CONTROL_WIDTH, 20).build();
            setChildren(List.of(button));
        }

        @Override
        public void update(int y, int x, int width, int height) {
            button.active = available.getAsBoolean();
            button.setPosition(x + width - CONTROL_WIDTH, y);
        }
    }

    /** A button showing a value; a click moves to the next one, a shift-click to the previous. */
    static final class Cycle<T> extends ConfigEntryWidget {
        private final Button button;

        Cycle(Component name, List<ClientTooltipComponent> tooltip, Supplier<String> search,
              List<T> values, Supplier<T> getter, Consumer<T> setter, Function<T, Component> label) {
            super(name, tooltip, search, 20);
            this.button = Button.builder(label.apply(getter.get()), b -> {
                int index = Math.max(0, values.indexOf(getter.get()));
                int step = Screen.hasShiftDown() ? values.size() - 1 : 1;
                setter.accept(values.get((index + step) % values.size()));
                b.setMessage(label.apply(getter.get()));
            }).size(CONTROL_WIDTH, 20).build();
            setChildren(List.of(button));
        }

        @Override
        public void update(int y, int x, int width, int height) {
            button.setPosition(x + width - CONTROL_WIDTH, y);
        }
    }

    /**
     * One or more ARGB colors in one row, each as {@code #RRGGBB} (opaque) or {@code #AARRGGBB}
     * with a swatch after it; hovering a field shows its name.
     */
    static final class Colors extends ConfigEntryWidget {
        private static final int SWATCH = 12;
        private final List<EditBox> fields = new ArrayList<>();
        private final List<Component> names;
        private final List<IntSupplier> getters;

        Colors(Component name, List<ClientTooltipComponent> tooltip, Supplier<String> search,
               List<Component> names, List<IntSupplier> getters, List<IntConsumer> setters) {
            super(name, tooltip, search, 20);
            this.names = names;
            this.getters = getters;
            int width = (CONTROL_WIDTH - (getters.size() - 1) * 4) / getters.size() - SWATCH - 2;
            for (int i = 0; i < getters.size(); i++) {
                IntConsumer setter = setters.get(i);
                EditBox text = new EditBox(Minecraft.getInstance().font, 0, 0, width, 18, Component.empty());
                text.setMaxLength(9);
                text.setValue(format(getters.get(i).getAsInt()));
                text.setResponder(value -> {
                    Integer color = parse(value);
                    text.setTextColor(color != null ? 0xFFFFFF : ChatFormatting.RED.getColor());
                    if (color != null) setter.accept(color);
                });
                fields.add(text);
            }
            setChildren(fields);
        }

        static String format(int color) {
            return (color >>> 24) == 0xFF
                    ? String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF)
                    : String.format(Locale.ROOT, "#%08X", color);
        }

        static Integer parse(String value) {
            String hex = value.startsWith("#") ? value.substring(1) : value;
            if (hex.length() != 6 && hex.length() != 8) return null;
            try {
                long color = Long.parseLong(hex, 16);
                return hex.length() == 6 ? (int) (0xFF000000L | color) : (int) color;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private int slotWidth() {
            return (CONTROL_WIDTH - (fields.size() - 1) * 4) / fields.size();
        }

        @Override
        public void update(int y, int x, int width, int height) {
            int left = x + width - CONTROL_WIDTH;
            for (int i = 0; i < fields.size(); i++) {
                fields.get(i).setPosition(left + i * (slotWidth() + 4) + 1, y + 1);
            }
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY,
                           boolean hovered, float delta) {
            super.render(graphics, index, y, x, width, height, mouseX, mouseY, hovered, delta);
            for (int i = 0; i < fields.size(); i++) {
                EditBox field = fields.get(i);
                int sx = field.getX() + field.getWidth() + 2;
                graphics.fill(sx, y + 3, sx + SWATCH, y + 17, 0xFF000000);
                graphics.fill(sx + 1, y + 4, sx + SWATCH - 1, y + 16, getters.get(i).getAsInt());
            }
        }

        @Override
        public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
            for (int i = 0; i < fields.size(); i++) {
                EditBox field = fields.get(i);
                if (mouseX >= field.getX() && mouseX < field.getX() + slotWidth()
                        && mouseY >= field.getY() && mouseY < field.getY() + field.getHeight()) {
                    return List.of(ClientTooltipComponent.create(names.get(i).getVisualOrderText()));
                }
            }
            return super.getTooltip(mouseX, mouseY);
        }
    }
}
