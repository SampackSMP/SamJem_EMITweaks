package com.carjem.sampackemitweaks.creative;

import com.carjem.sampackemitweaks.SampackEmiTweaks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * What a collapsed group's slot shows, from an {@code icon} string in a REMI stack group json:
 * <ul>
 *   <li>{@code "first"}: the group's first item (the creative inventory's default)</li>
 *   <li>{@code "stacked"}: its first three items fanned out, as REMI draws groups (REMI's default)</li>
 *   <li>an item id, like {@code "minecraft:oak_log"} (or {@code "item:minecraft:oak_log"})</li>
 *   <li>a texture path ending in {@code .png}, like {@code "sampack:textures/gui/groups/logs.png"},
 *   drawn over the whole 16x16 slot; any square size works</li>
 * </ul>
 * A missing, blank or unknown icon leaves the default, which the config sets per place
 * ({@link Style}).
 */
public final class GroupIcon {
    public enum Kind { FIRST, STACKED, ITEM, TEXTURE }

    /** The defaults a group without an icon can take. */
    public enum Style {
        FIRST, STACKED;

        public GroupIcon icon() {
            return this == FIRST ? GroupIcon.FIRST : GroupIcon.STACKED;
        }
    }

    public static final GroupIcon FIRST = new GroupIcon(Kind.FIRST, null, ItemStack.EMPTY);
    public static final GroupIcon STACKED = new GroupIcon(Kind.STACKED, null, ItemStack.EMPTY);

    public final Kind kind;
    @Nullable public final ResourceLocation texture;
    public final ItemStack item;

    private GroupIcon(Kind kind, @Nullable ResourceLocation texture, ItemStack item) {
        this.kind = kind;
        this.texture = texture;
        this.item = item;
    }

    /** The icon a string names, or null for the default. */
    @Nullable
    public static GroupIcon parse(@Nullable String spec, Object group) {
        if (spec == null || spec.isBlank()) return null;
        spec = spec.trim();
        switch (spec.toLowerCase(Locale.ROOT)) {
            case "first":
                return FIRST;
            case "stacked":
                return STACKED;
            default:
                break;
        }
        if (spec.endsWith(".png")) {
            ResourceLocation texture = ResourceLocation.tryParse(spec);
            if (texture != null) return new GroupIcon(Kind.TEXTURE, texture, ItemStack.EMPTY);
        } else {
            ResourceLocation id = ResourceLocation.tryParse(spec.startsWith("item:") ? spec.substring("item:".length()) : spec);
            Item item = id != null ? BuiltInRegistries.ITEM.getOptional(id).orElse(null) : null;
            if (item != null) return new GroupIcon(Kind.ITEM, null, new ItemStack(item));
        }
        SampackEmiTweaks.LOGGER.warn("Group {} has an unknown icon \"{}\"; expected first, stacked, an item id or a .png texture",
                group, spec);
        return null;
    }

    /** Draws a texture or item icon over a 16x16 slot. */
    public void render(GuiGraphics graphics, int x, int y) {
        if (kind == Kind.TEXTURE) {
            graphics.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
        } else if (kind == Kind.ITEM) {
            graphics.renderItem(item, x, y);
        }
    }

    /** Draws the group's slot from its items: this icon, or the items as {@link Kind#FIRST} or {@link Kind#STACKED} say. */
    public void render(GuiGraphics graphics, int x, int y, List<ItemStack> items) {
        if (kind == Kind.FIRST) {
            if (!items.isEmpty()) graphics.renderItem(items.getFirst(), x, y);
        } else if (kind == Kind.STACKED) {
            renderStacked(graphics, x, y, items);
        } else {
            render(graphics, x, y);
        }
    }

    /** Up to three items, scaled down and fanned out, laid out exactly as REMI's EmiGroupStack does. */
    private static void renderStacked(GuiGraphics graphics, int x, int y, List<ItemStack> items) {
        if (items.isEmpty()) return;
        graphics.pose().pushPose();
        graphics.pose().translate(x + 1.6F, y + 1.6F, 0);
        graphics.pose().scale(0.8F, 0.8F, 0.8F);
        if (items.size() == 1) {
            graphics.renderItem(items.getFirst(), 0, 0);
        } else if (items.size() == 2) {
            graphics.pose().translate(0.5F, 0, 0);
            graphics.renderItem(items.get(1), 1, -1);
            graphics.pose().translate(0, 0, 10);
            graphics.renderItem(items.getFirst(), -2, 1);
        } else {
            graphics.renderItem(items.get(2), 3, -2);
            graphics.pose().translate(0, 0, 10);
            graphics.renderItem(items.get(1), 0, 0);
            graphics.pose().translate(0, 0, 10);
            graphics.renderItem(items.getFirst(), -3, 2);
        }
        graphics.pose().popPose();
    }
}
