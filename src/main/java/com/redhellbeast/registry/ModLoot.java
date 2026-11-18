package com.redhellbeast.registry;

import com.mojang.serialization.Codec;
import com.redhellbeast.RedHellbeastMod;
import com.redhellbeast.loot.AddCartridgeModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLoot {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RedHellbeastMod.MODID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> ADD_CARTRIDGE =
            SERIALIZERS.register("add_cartridge", AddCartridgeModifier.CODEC);

    private ModLoot() {}
}
