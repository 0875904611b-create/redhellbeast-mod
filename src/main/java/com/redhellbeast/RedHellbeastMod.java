package com.redhellbeast;

import com.redhellbeast.network.ModNetwork;
import com.redhellbeast.registry.ModBlocks;
import com.redhellbeast.registry.ModEntities;
import com.redhellbeast.registry.ModItems;
import com.redhellbeast.registry.ModLoot;
import com.redhellbeast.registry.ModTabs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RedHellbeastMod.MODID)
public class RedHellbeastMod {
    public static final String MODID = "redhellbeast";

    public RedHellbeastMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModTabs.TABS.register(bus);
        ModLoot.SERIALIZERS.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(ModEntities::registerAttributes);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
