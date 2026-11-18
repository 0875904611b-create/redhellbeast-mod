package com.redhellbeast.registry;

import com.redhellbeast.RedHellbeastMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RedHellbeastMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.redhellbeast"))
                    .icon(() -> new ItemStack(ModItems.GODZILLA_CARTRIDGE.get()))
                    .displayItems((params, out) -> {
                        out.accept(ModItems.GODZILLA_CARTRIDGE.get());
                        out.accept(ModItems.RETRO_COMPUTER.get());
                        out.accept(ModItems.HELLBEAST_HEART.get());
                        out.accept(ModItems.RED_SPAWN_EGG.get());
                    })
                    .build());

    private ModTabs() {}
}
