package com.redhellbeast.registry;

import com.redhellbeast.RedHellbeastMod;
import com.redhellbeast.item.GodzillaCartridgeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RedHellbeastMod.MODID);

    public static final RegistryObject<Item> GODZILLA_CARTRIDGE = ITEMS.register("godzilla_cartridge",
            () -> new GodzillaCartridgeItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> RETRO_COMPUTER = ITEMS.register("retro_computer",
            () -> new BlockItem(ModBlocks.RETRO_COMPUTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> HELLBEAST_HEART = ITEMS.register("hellbeast_heart",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()) {
                @Override
                public boolean isFoil(net.minecraft.world.item.ItemStack stack) {
                    return true;
                }
            });

    public static final RegistryObject<Item> RED_SPAWN_EGG = ITEMS.register("red_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.RED, 0x4A0000, 0xFF2020, new Item.Properties()));

    private ModItems() {}
}
