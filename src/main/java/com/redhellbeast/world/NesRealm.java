package com.redhellbeast.world;

import com.redhellbeast.RedHellbeastMod;
import com.redhellbeast.entity.RedEntity;
import com.redhellbeast.network.ModNetwork;
import com.redhellbeast.network.OpenGameOverPacket;
import com.redhellbeast.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** Everything about getting sucked into (and spat out of) the NES cartridge dimension. */
public final class NesRealm {
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(RedHellbeastMod.MODID, "nes_realm"));

    private static final String PENDING = "RedPendingTick";
    private static final String RET_DIM = "RedRetDim", RET_X = "RedRetX", RET_Y = "RedRetY", RET_Z = "RedRetZ", RET_YAW = "RedRetYaw", RET_PITCH = "RedRetPitch";

    /** Cartridge put into the computer. Returns false if a sequence is already running or the player is already inside. */
    public static boolean beginSequence(ServerPlayer p) {
        if (p.level().dimension() == DIM) return false;
        long now = p.level().getGameTime();
        CompoundTag d = p.getPersistentData();
        if (d.contains(PENDING) && now - d.getLong(PENDING) < 600) return false;

        d.putLong(PENDING, now);
        d.putString(RET_DIM, p.level().dimension().location().toString());
        d.putDouble(RET_X, p.getX());
        d.putDouble(RET_Y, p.getY());
        d.putDouble(RET_Z, p.getZ());
        d.putFloat(RET_YAW, p.getYRot());
        d.putFloat(RET_PITCH, p.getXRot());

        // keep the player safe and still while the screen takes over
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 22, 4, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 18, 6, false, false));
        p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.5F);
        ModNetwork.sendToPlayer(new OpenGameOverPacket(), p);
        return true;
    }

    public static void onGameOverFinished(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        if (!d.contains(PENDING)) return;
        long age = p.level().getGameTime() - d.getLong(PENDING);
        d.remove(PENDING);
        if (age > 1500) return;
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        enter(p);
    }

    public static void enter(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        ServerLevel nes = server.getLevel(DIM);
        if (nes == null) {
            p.sendSystemMessage(Component.literal("\u00a7cThe NES realm dimension is missing (is the datapack loaded?)"));
            return;
        }
        ArenaBuilder.ensureBuilt(nes);

        RedEntity existing = null;
        for (RedEntity r : nes.getEntities(ModEntities.RED.get(), e -> true)) {
            if (!r.isRemoved() && r.isAlive()) existing = r;
        }

        p.teleportTo(nes, 0.5, ArenaBuilder.FLOOR_Y + 1, 22.5, 180.0F, 0.0F);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 6, 4, false, false));
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 25));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("\u00a77PRESS START")));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("\u00a74\u00a7lGAME  START")));

        if (existing == null) {
            RedEntity red = ModEntities.RED.get().create(nes);
            if (red == null) return;
            red.moveTo(0.5, ArenaBuilder.FLOOR_Y + 1, -16.5, 0.0F, 0.0F);
            red.finalizeSpawn(nes, nes.getCurrentDifficultyAt(red.blockPosition()), MobSpawnType.EVENT, null, null);
            red.beginIntro();
            nes.addFreshEntity(red);
        }
    }

    /** Spit the player back out to where they put the cartridge in (or world spawn). */
    public static void exit(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        CompoundTag d = p.getPersistentData();
        ServerLevel dest = server.overworld();
        double x, y, z;
        float yaw = 0, pitch = 0;
        boolean has = false;
        if (d.contains(RET_DIM)) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(d.getString(RET_DIM)));
            ServerLevel l = server.getLevel(key);
            if (l != null && key != DIM) {
                dest = l;
                has = true;
            }
        }
        if (has) {
            x = d.getDouble(RET_X);
            y = d.getDouble(RET_Y);
            z = d.getDouble(RET_Z);
            yaw = d.getFloat(RET_YAW);
            pitch = d.getFloat(RET_PITCH);
        } else {
            BlockPos sp = dest.getSharedSpawnPos();
            x = sp.getX() + 0.5;
            z = sp.getZ() + 0.5;
            y = dest.getHeight(Heightmap.Types.MOTION_BLOCKING, sp.getX(), sp.getZ());
        }
        d.remove(RET_DIM);
        p.teleportTo(dest, x, y, z, yaw, pitch);
        p.removeEffect(MobEffects.DARKNESS);
        p.removeEffect(MobEffects.BLINDNESS);
        p.removeEffect(MobEffects.WITHER);
        p.clearFire();
    }

    private NesRealm() {}
}
