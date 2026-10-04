package com.carjem.sampackemitweaks.icondump.source;

import com.carjem.sampackemitweaks.icondump.source.StackSources.Entry;
import com.carjem.sampackemitweaks.pristine.PristineTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Without EMI: IconExporter's list, every creative tab's items and then every source fluid. Ids
 * take EMI's shape (item:ns:path, a component patch as SNBT in braces, fluid:ns:path), so the
 * meta.json reads the same either way.
 *
 * The tabs are the registered ones as their mods built them (see {@link PristineTabs}), so the
 * pack's creative tab rules neither reorder the export nor add their custom tabs to it.
 */
final class VanillaSource {

    private VanillaSource() {
    }

    static List<Entry> collect(Predicate<String> wantNamespace) {
        Minecraft mc = Minecraft.getInstance();
        CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(),
                mc.options.operatorItemsTab().get(), mc.level.registryAccess());

        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CreativeModeTab tab : PristineTabs.order()) {
            if (tab.getType() != CreativeModeTab.Type.CATEGORY) {
                continue;   // search, hotbar, inventory: nothing the category tabs lack
            }
            for (ItemStack stack : PristineTabs.displayItems(tab)) {
                if (!stack.isEmpty() && wantNamespace.test(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) {
                    Entry entry = item(stack, false);
                    if (seen.add(entry.id())) {
                        entries.add(entry);
                    }
                }
            }
        }
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
            // flowing variants share their source's icon; EMI lists sources only too
            if (fluid.isSource(fluid.defaultFluidState()) && wantNamespace.test(key.getNamespace())) {
                entries.add(fluid(fluid, key));
            }
        }
        return entries;
    }

    static Entry unlisted(ItemStack stack) {
        return item(stack, true);
    }

    private static Entry item(ItemStack stack, boolean unlisted) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = "item:" + key;
        DataComponentPatch patch = stack.getComponentsPatch();
        if (!patch.isEmpty()) {
            var ops = Minecraft.getInstance().level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            id += DataComponentPatch.CODEC.encodeStart(ops, patch).result().map(Object::toString).orElse("");
        }
        ItemStack copy = stack.copy();
        return new Entry(id, key.getNamespace(), StackSources.nameOf(copy::getHoverName), unlisted,
                graphics -> graphics.renderItem(copy, 0, 0));
    }

    private static Entry fluid(Fluid fluid, ResourceLocation key) {
        return new Entry("fluid:" + key, key.getNamespace(),
                StackSources.nameOf(() -> fluid.getFluidType().getDescription()), false,
                graphics -> {
                    IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
                    TextureAtlasSprite sprite = Minecraft.getInstance()
                            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
                    int tint = ext.getTintColor();
                    graphics.blit(0, 0, 0, 16, 16, sprite,
                            ((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f, 1f);
                });
    }
}
