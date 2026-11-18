package com.redhellbeast.entity;

import com.redhellbeast.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Flaming skull fired by Red. Can be punched back at him (vanilla fireball deflection). */
public class HellSkull extends AbstractHurtingProjectile {
    public HellSkull(EntityType<? extends HellSkull> type, Level level) {
        super(type, level);
    }

    public static HellSkull create(Level level, LivingEntity owner, Vec3 pos, Vec3 dir, double speed) {
        HellSkull s = new HellSkull(ModEntities.HELL_SKULL.get(), level);
        s.setOwner(owner);
        s.setPos(pos.x, pos.y, pos.z);
        s.setDeltaMovement(dir.normalize().scale(speed));
        return s;
    }

    @Override
    protected float getInertia() {
        return 1.0F;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SOUL_FIRE_FLAME;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 160) discard();
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (getOwner() instanceof RedEntity && (e instanceof RedEntity || e instanceof Vex)) return false;
        return super.canHitEntity(e);
    }

    private DamageSource source() {
        Entity o = getOwner();
        return damageSources().mobProjectile(this, o instanceof LivingEntity le ? le : null);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!level().isClientSide) {
            hit.getEntity().hurt(source(), 6.0F);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 36, 0.7, 0.7, 0.7, 0.06);
        sl.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 12, 0.5, 0.5, 0.5, 0.0);
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.9F, 1.5F);
        Entity owner = getOwner();
        boolean fromRed = owner instanceof RedEntity;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2.4),
                x -> x != owner && !(fromRed && (x instanceof RedEntity || x instanceof Vex)))) {
            e.hurt(source(), 5.0F);
            e.setSecondsOnFire(4);
        }
        discard();
    }
}
