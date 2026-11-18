package com.redhellbeast.registry;

import com.redhellbeast.RedHellbeastMod;
import com.redhellbeast.entity.HellSkull;
import com.redhellbeast.entity.RedEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RedHellbeastMod.MODID);

    public static final RegistryObject<EntityType<RedEntity>> RED = ENTITIES.register("red",
            () -> EntityType.Builder.<RedEntity>of(RedEntity::new, MobCategory.MONSTER)
                    .sized(3.4F, 4.2F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("red"));

    public static final RegistryObject<EntityType<HellSkull>> HELL_SKULL = ENTITIES.register("hell_skull",
            () -> EntityType.Builder.<HellSkull>of(HellSkull::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .fireImmune()
                    .clientTrackingRange(4)
                    .updateInterval(2)
                    .build("hell_skull"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(RED.get(), RedEntity.createAttributes().build());
    }

    private ModEntities() {}
}
