package com.redhellbeast.registry;

import com.redhellbeast.RedHellbeastMod;
import com.redhellbeast.block.RetroComputerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RedHellbeastMod.MODID);

    public static final RegistryObject<Block> RETRO_COMPUTER = BLOCKS.register("retro_computer",
            () -> new RetroComputerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 6)));

    private ModBlocks() {}
}
