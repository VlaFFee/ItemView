package com.example.itemview;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ItemViewMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ItemViewMod.MODID);

    // Нет рецептов, нет лута -> получить можно только из креатива (или командой /give)
    public static final RegistryObject<Item> EXHIBIT =
            ITEMS.register("exhibit", () -> new ExhibitItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.itemview"))
                    .icon(() -> new ItemStack(EXHIBIT.get()))
                    .displayItems((params, out) -> out.accept(EXHIBIT.get()))
                    .build());

    private ModItems() {}
}
