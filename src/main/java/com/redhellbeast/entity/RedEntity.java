package com.redhellbeast.entity;

import com.redhellbeast.network.GlitchPacket;
import com.redhellbeast.network.ModNetwork;
import com.redhellbeast.registry.ModItems;
import com.redhellbeast.world.ArenaBuilder;
import com.redhellbeast.world.NesRealm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * RED - "The Hellbeast" from the NES Godzilla creepypasta.
 *
 * Phase 1 (100%-66%)  THE HELL BEAST : claws, Lava Fingers, Soul Burn, Glitch Step, lava floor patches.
 * Phase 2 (66%-33%)   CORRUPTED      : + Hellfire skulls, Glitch Sprites (vex), Hell Rain, faster, more lava.
 * Phase 3 (33%-0%)    GAME OVER      : + Hell Beam, CRASH shockwaves (jump them!), double claws, constant glitching.
 */
public class RedEntity extends Monster {

    public enum Attack { NONE, CLAW, LAVA_FINGERS, SOUL_BURN, GLITCH_STEP, SKULLS, SUMMON, HELL_RAIN, BEAM, CRASH }

    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(RedEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK = SynchedEntityData.defineId(RedEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BUSY = SynchedEntityData.defineId(RedEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float DEG = (float) Math.PI / 180F;
    private static final float[] PHASE_AT = {0.66F, 0.33F, 0.0F};

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("RED"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_20);

    private int introTicks, transitionTicks, pendingPhase, attackCooldown = 60, attackTick, noPlayerSeconds, hazardTimer = 120;
    private Attack attack = Attack.NONE;
    private Vec3 beamDir = Vec3.ZERO;
    private final List<Eruption> eruptions = new ArrayList<>();
    private final List<Hazard> hazards = new ArrayList<>();
    /** client only: tickCount when the current attack animation started. */
    public int clientAttackStart = -1000;

    public RedEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
        bossEvent.setDarkenScreen(true);
        bossEvent.setCreateWorldFog(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 700.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.FOLLOW_RANGE, 80.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 6.0D);
    }

    // ------------------------------------------------------------------ data / goals
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_PHASE, 0);
        entityData.define(DATA_ATTACK, 0);
        entityData.define(DATA_BUSY, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_ATTACK.equals(key)) clientAttackStart = tickCount;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 48.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public int getPhase() { return entityData.get(DATA_PHASE); }

    private void setPhase(int p) {
        entityData.set(DATA_PHASE, p);
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.28D + 0.035D * p);
        switch (p) {
            case 0 -> { bossEvent.setColor(BossEvent.BossBarColor.RED); bossEvent.setName(Component.translatable("boss.redhellbeast.phase1")); }
            case 1 -> { bossEvent.setColor(BossEvent.BossBarColor.PURPLE); bossEvent.setName(Component.translatable("boss.redhellbeast.phase2")); }
            default -> { bossEvent.setColor(BossEvent.BossBarColor.WHITE); bossEvent.setName(Component.translatable("boss.redhellbeast.phase3")); }
        }
    }

    public Attack getAttack() {
        int i = entityData.get(DATA_ATTACK);
        Attack[] v = Attack.values();
        return v[Mth.clamp(i, 0, v.length - 1)];
    }

    public boolean isBusy() { return entityData.get(DATA_BUSY); }

    /** Used by the renderer to jitter the model. */
    public boolean isGlitching() {
        Attack a = getAttack();
        return isBusy() || deathTime > 0 || a == Attack.GLITCH_STEP || a == Attack.CRASH
                || (getPhase() >= 2 && (tickCount % 50) < 3) || (getPhase() == 1 && (tickCount % 90) < 2);
    }

    public float animTime(float ageInTicks) {
        return getAttack() == Attack.NONE ? 0 : ageInTicks - clientAttackStart;
    }

    private void startAttack(Attack a) {
        attack = a;
        attackTick = 0;
        entityData.set(DATA_ATTACK, a.ordinal());
    }

    private void endAttack(int cooldown) {
        attack = Attack.NONE;
        attackTick = 0;
        entityData.set(DATA_ATTACK, 0);
        attackCooldown = Math.max(6, cooldown - getPhase() * 6);
    }

    // ------------------------------------------------------------------ boss bar
    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        bossEvent.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bossEvent.removePlayer(p);
    }

    // ------------------------------------------------------------------ intro / transitions
    public void beginIntro() {
        introTicks = 110;
        entityData.set(DATA_BUSY, true);
        setPhase(0);
        bossEvent.setProgress(1.0F);
    }

    private void tickIntro() {
        getNavigation().stop();
        int t = 110 - introTicks;
        if (level() instanceof ServerLevel sl) {
            if (t == 1) {
                playSound(SoundEvents.ENDER_DRAGON_GROWL, 5.0F, 0.45F);
                glitchNearby(110, 0.9F);
            }
            if (t == 45) {
                broadcast("\u00a74\u00a7k||\u00a7r \u00a7cI  W A S  H E R E  B E F O R E  Y O U  \u00a74\u00a7k||");
                playSound(SoundEvents.WARDEN_ROAR, 4.0F, 0.6F);
                glitchNearby(40, 1.0F);
            }
            if (t % 3 == 0) {
                sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.2, getZ(), 4, 1.6, 0.1, 1.6, 0.0);
                sl.sendParticles(DustParticleOptions.REDSTONE, getX(), getY() + 2, getZ(), 6, 1.5, 2.0, 1.5, 0.0);
            }
        }
        if (--introTicks <= 0) {
            entityData.set(DATA_BUSY, false);
            attackCooldown = 30;
        }
    }

    private void checkPhaseChange() {
        int p = getPhase();
        if (p < 2 && getHealth() <= PHASE_AT[p] * getMaxHealth() + 0.1F) startTransition(p + 1);
    }

    private void startTransition(int newPhase) {
        pendingPhase = newPhase;
        transitionTicks = 80;
        entityData.set(DATA_BUSY, true);
        attack = Attack.NONE;
        entityData.set(DATA_ATTACK, 0);
        eruptions.clear();
        getNavigation().stop();
        playSound(SoundEvents.WARDEN_ROAR, 5.0F, 0.5F);
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 5.0F, 0.4F);
        glitchNearby(90, 1.0F);
        broadcast(newPhase == 1 ? "\u00a74\u00a7lR\u00a7k#\u00a74\u00a7lD  \u00a7c-  C O R R U P T E D"
                : "\u00a74\u00a7l\u00a7kXXXX\u00a7r \u00a74\u00a7lG A M E   O V E R \u00a7kXXXX");
    }

    private void tickTransition() {
        getNavigation().stop();
        if (transitionTicks == 40) {
            setPhase(pendingPhase);
            glitchNearby(40, 1.0F);
            playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 0.6F);
        }
        if (level() instanceof ServerLevel sl && transitionTicks % 2 == 0) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 2, getZ(), 10, 1.8, 2.2, 1.8, 0.2);
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1, getZ(), 8, 1.5, 1.5, 1.5, 0.05);
        }
        if (--transitionTicks <= 0) {
            entityData.set(DATA_BUSY, false);
            attackCooldown = 30;
        }
    }

    // ------------------------------------------------------------------ damage
    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (!src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (introTicks > 0 || transitionTicks > 0) return false;
            int p = getPhase();
            if (p < 2) {   // never skip a phase with one huge hit
                float floor = PHASE_AT[p] * getMaxHealth();
                if (getHealth() - amount < floor) amount = Math.max(0.01F, getHealth() - floor);
            }
        }
        return super.hurt(src, amount);
    }

    @Override
    public boolean fireImmune() { return true; }

    @Override
    protected boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public void checkDespawn() { }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    // ------------------------------------------------------------------ main AI
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));

        if (tickCount % 20 == 0) {   // no players around -> the fight is over, vanish
            if (level().getNearestPlayer(this, 90.0D) == null) {
                if (++noPlayerSeconds > 10) discard();
            } else noPlayerSeconds = 0;
        }

        tickEruptions();
        tickHazards();

        if (introTicks > 0) { tickIntro(); return; }
        if (transitionTicks > 0) { tickTransition(); return; }
        checkPhaseChange();
        if (transitionTicks > 0) return;

        LivingEntity target = getTarget();
        if (target != null && !target.isAlive()) target = null;
        if (attackCooldown > 0) attackCooldown--;

        if (attack != Attack.NONE) {
            tickAttack(target);
        } else if (target != null) {
            chase(target);
            if (attackCooldown <= 0) chooseAttack(target);
        }

        if (target != null && getPhase() >= 1 && --hazardTimer <= 0) {
            hazardTimer = getPhase() == 1 ? 110 : 70;
            spawnHazard(target.position().add(random.nextGaussian() * 3, 0, random.nextGaussian() * 3), getPhase() == 1 ? 1 : 2);
        }
    }

    private void chase(LivingEntity t) {
        faceTowards(t, 10F);
        if (distanceTo(t) > 4.5F) {
            if (tickCount % 8 == 0) getNavigation().moveTo(t, 1.0D + getPhase() * 0.1D);
        } else getNavigation().stop();
    }

    private void chooseAttack(LivingEntity t) {
        double d = distanceTo(t);
        int ph = getPhase();
        List<Attack> pool = new ArrayList<>();
        if (d < 7.5) { add(pool, Attack.CLAW, ph == 2 ? 4 : 3); add(pool, Attack.SOUL_BURN, 1); }
        else { add(pool, Attack.LAVA_FINGERS, 2); add(pool, Attack.GLITCH_STEP, 2); add(pool, Attack.SOUL_BURN, 2); }
        add(pool, Attack.LAVA_FINGERS, 1);
        if (ph >= 1) {
            add(pool, Attack.SKULLS, 2);
            add(pool, Attack.HELL_RAIN, 1);
            if (countVexes() < 4) add(pool, Attack.SUMMON, 2);
        }
        if (ph >= 2) {
            add(pool, Attack.BEAM, 3);
            add(pool, Attack.CRASH, 2);
            add(pool, Attack.HELL_RAIN, 1);
        }
        Attack pick = pool.get(random.nextInt(pool.size()));
        if (pick == Attack.CRASH && d < 3) pick = Attack.CLAW;
        startAttack(pick);
    }

    private static void add(List<Attack> l, Attack a, int w) { for (int i = 0; i < w; i++) l.add(a); }

    private int countVexes() {
        return level().getEntitiesOfClass(Vex.class, getBoundingBox().inflate(48), v -> v.getOwner() == this).size();
    }

    private void tickAttack(@Nullable LivingEntity t) {
        attackTick++;
        int ph = getPhase();
        switch (attack) {
            case CLAW -> {
                getNavigation().stop();
                if (t != null && attackTick < 12) faceTowards(t, 30F);
                if (attackTick == 10 || (ph == 2 && attackTick == 17)) {
                    sweep(12.0F + ph * 3.0F, 5.0D, 1.5D);
                    playSound(SoundEvents.RAVAGER_ATTACK, 2.0F, 0.6F);
                }
                if (attackTick >= 26) endAttack(24);
            }
            case LAVA_FINGERS -> {
                getNavigation().stop();
                if (t != null && attackTick < 20) faceTowards(t, 30F);
                if (attackTick < 20 && attackTick % 3 == 0) particleBurst(ParticleTypes.FLAME, armPos(), 4, 0.5, 0.02);
                if (attackTick == 20) fireLavaFingers();
                if (attackTick >= 46) endAttack(26);
            }
            case SOUL_BURN -> {
                getNavigation().stop();
                if (t == null) { endAttack(30); return; }
                faceTowards(t, 20F);
                if (attackTick > 15 && attackTick < 40 && level() instanceof ServerLevel sl) {
                    Vec3 a = getEyePosition(), b = t.position().add(0, t.getBbHeight() * 0.6, 0);
                    for (int i = 0; i < 6; i++) {
                        Vec3 p = a.lerp(b, (attackTick - 15 + i / 6.0) / 25.0);
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
                    }
                }
                if (attackTick == 40 && hasLineOfSight(t) && distanceTo(t) < 45) {
                    t.hurt(damageSources().magic(), 7.0F + ph * 2.0F);
                    t.setSecondsOnFire(8);
                    t.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, ph >= 1 ? 1 : 0));
                    Vec3 pull = position().subtract(t.position()).multiply(1, 0, 1).normalize().scale(1.7);
                    t.setDeltaMovement(pull.x, 0.4, pull.z);
                    t.hurtMarked = true;
                    glitchPlayer(t, 25, 0.6F);
                    playSound(SoundEvents.WARDEN_SONIC_BOOM, 2.5F, 1.2F);
                }
                if (attackTick >= 56) endAttack(30);
            }
            case GLITCH_STEP -> {
                getNavigation().stop();
                if (t == null) { endAttack(30); return; }
                if (attackTick < 8 && level() instanceof ServerLevel sl)
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 2, getZ(), 12, 1.2, 1.8, 1.2, 0.3);
                if (attackTick == 8) {
                    particleBurst(ParticleTypes.ELECTRIC_SPARK, position().add(0, 2, 0), 40, 1.6, 0.3);
                    Vec3 look = t.getLookAngle().multiply(1, 0, 1);
                    Vec3 behind = t.position().subtract(look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize().scale(4.5));
                    behind = clampToArena(behind);
                    moveTo(behind.x, getY(), behind.z, getYRot(), 0F);
                    setDeltaMovement(Vec3.ZERO);
                    faceTowards(t, 180F);
                    particleBurst(ParticleTypes.ELECTRIC_SPARK, position().add(0, 2, 0), 40, 1.6, 0.3);
                    playSound(SoundEvents.ENDERMAN_TELEPORT, 3.0F, 0.4F);
                    glitchPlayer(t, 14, 0.5F);
                }
                if (attackTick > 8 && attackTick < 22) faceTowards(t, 40F);
                if (attackTick == 22) sweep(13.0F + ph * 3.0F, 5.0D, 1.5D);
                if (attackTick >= 32) endAttack(30);
            }
            case SKULLS -> {
                getNavigation().stop();
                if (t != null) faceTowards(t, 25F);
                int[] shots = ph == 2 ? new int[]{14, 18, 22, 26, 30} : new int[]{18, 26, 34};
                for (int s : shots) if (attackTick == s && t != null) shootSkull(t);
                if (attackTick >= (ph == 2 ? 44 : 50)) endAttack(28);
            }
            case SUMMON -> {
                getNavigation().stop();
                if (t != null) faceTowards(t, 20F);
                if (attackTick % 4 == 0) particleBurst(ParticleTypes.SCULK_SOUL, position().add(0, 3, 0), 6, 1.5, 0.05);
                if (attackTick == 40) summonSprites(ph == 2 ? 3 : 2);
                if (attackTick >= 60) endAttack(60);
            }
            case HELL_RAIN -> {
                getNavigation().stop();
                if (t != null && attackTick < 16) faceTowards(t, 25F);
                if (attackTick == 16 && t != null) rainEruptions(t, ph == 2 ? 24 : 13);
                if (attackTick >= 80) endAttack(40);
            }
            case BEAM -> tickBeam(t);
            case CRASH -> {
                getNavigation().stop();
                if (attackTick == 1) {
                    playSound(SoundEvents.ENDER_DRAGON_GROWL, 6.0F, 0.4F);
                    playSound(SoundEvents.WARDEN_ROAR, 6.0F, 0.5F);
                    glitchNearby(110, 1.0F);
                    broadcast("\u00a74\u00a7l\u00a7k||\u00a7r\u00a7c  C R A S H  \u00a74\u00a7l\u00a7k||");
                }
                if (attackTick < 30 && attackTick % 3 == 0) particleBurst(ParticleTypes.ELECTRIC_SPARK, position().add(0, 2, 0), 14, 2.0, 0.4);
                if (attackTick == 30) crashWave();
                if (attackTick >= 90) endAttack(70);
            }
            default -> endAttack(20);
        }
    }

    // ------------------------------------------------------------------ attacks
    private boolean isEnemy(LivingEntity e) {
        return e != this && e.isAlive() && e instanceof Player p && !p.isSpectator();
    }

    private Vec3 forward() {
        float y = getYRot() * DEG;
        return new Vec3(-Mth.sin(y), 0, Mth.cos(y));
    }

    private Vec3 armPos() {
        return position().add(forward().scale(2.4)).add(0, 1.4, 0);
    }

    private void faceTowards(net.minecraft.world.entity.Entity t, float maxStep) {
        double dx = t.getX() - getX(), dz = t.getZ() - getZ();
        float want = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float diff = Mth.wrapDegrees(want - getYRot());
        setYRot(getYRot() + Mth.clamp(diff, -maxStep, maxStep));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    private void sweep(float damage, double reach, double kb) {
        Vec3 fwd = forward();
        AABB box = getBoundingBox().inflate(reach, 1.0, reach);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, this::isEnemy)) {
            Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
            if (to.lengthSqr() < 1e-4 || to.normalize().dot(fwd) > 0.1) {
                if (e.hurt(damageSources().mobAttack(this), damage)) {
                    e.knockback(kb, Mth.sin(getYRot() * DEG), -Mth.cos(getYRot() * DEG));
                    e.hurtMarked = true;
                }
            }
        }
        particleBurst(ParticleTypes.SWEEP_ATTACK, position().add(fwd.scale(3.0)).add(0, 1.8, 0), 3, 1.2, 0.0);
    }

    private void fireLavaFingers() {
        Vec3 dir = forward();
        int lines = getPhase() == 2 ? 3 : 1;
        float[] angles = {0F, 0.5F, -0.5F};
        playSound(SoundEvents.BLAZE_SHOOT, 3.0F, 0.5F);
        for (int l = 0; l < lines; l++) {
            Vec3 d = dir.yRot(angles[l]);
            for (int i = 1; i <= 14; i++) {
                Vec3 p = position().add(d.scale(2.5 + i * 1.9));
                queueEruption(p.x, getY(), p.z, 4 + i * 2, 9.0F + getPhase() * 2, 1.8F, false);
            }
        }
    }

    private void shootSkull(LivingEntity t) {
        Vec3 from = getEyePosition().add(forward().scale(2.0));
        Vec3 to = t.getEyePosition().add(t.getDeltaMovement().scale(7));
        Vec3 dir = to.subtract(from).normalize().add(random.nextGaussian() * 0.04, random.nextGaussian() * 0.03, random.nextGaussian() * 0.04);
        level().addFreshEntity(HellSkull.create(level(), this, from, dir, 0.95D));
        playSound(SoundEvents.BLAZE_SHOOT, 2.5F, 0.6F);
    }

    private void summonSprites(int n) {
        if (!(level() instanceof ServerLevel sl)) return;
        playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 3.0F, 0.5F);
        for (int i = 0; i < n; i++) {
            Vex v = EntityType.VEX.create(sl);
            if (v == null) continue;
            Vec3 p = position().add(random.nextGaussian() * 2.5, 2.5 + random.nextDouble() * 2, random.nextGaussian() * 2.5);
            v.moveTo(p.x, p.y, p.z, getYRot(), 0F);
            v.finalizeSpawn(sl, sl.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            v.setOwner(this);
            v.setBoundOrigin(blockPosition());
            v.setLimitedLife(20 * 30);
            v.setCustomName(Component.translatable("entity.redhellbeast.glitch_sprite"));
            sl.addFreshEntity(v);
        }
    }

    private void rainEruptions(LivingEntity t, int n) {
        for (int i = 0; i < n; i++) {
            double ox, oz;
            if (i % 3 == 0) { ox = random.nextGaussian() * 1.4; oz = random.nextGaussian() * 1.4; }
            else { double a = random.nextDouble() * Math.PI * 2, r = 3 + random.nextDouble() * 11; ox = Math.cos(a) * r; oz = Math.sin(a) * r; }
            Vec3 p = clampToArena(new Vec3(t.getX() + ox, getY(), t.getZ() + oz));
            queueEruption(p.x, getY(), p.z, 24 + random.nextInt(50), 10.0F, 2.3F, false);
        }
    }

    private void crashWave() {
        playSound(SoundEvents.GENERIC_EXPLODE, 6.0F, 0.5F);
        glitchNearby(70, 1.0F);
        if (level() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(this) < 60 * 60) {
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                }
            }
        }
        for (int k = 1; k <= 7; k++) {     // expanding shockwave rings - JUMP over them
            double r = 3.5 * k;
            int n = (int) Math.max(8, Math.round(2 * Math.PI * r / 2.4));
            double offset = random.nextDouble() * Math.PI;
            for (int j = 0; j < n; j++) {
                double a = offset + j * Math.PI * 2 / n;
                Vec3 p = clampToArena(position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
                queueEruption(p.x, getY(), p.z, 2 + k * 6, 11.0F, 1.7F, true);
            }
        }
    }

    private void tickBeam(@Nullable LivingEntity t) {
        getNavigation().stop();
        if (t != null && attackTick <= 30) faceTowards(t, 25F);
        Vec3 origin = getEyePosition().add(forward().scale(1.8));
        if (attackTick == 1) playSound(SoundEvents.BEACON_POWER_SELECT, 5.0F, 0.5F);
        if (attackTick < 30) {
            if (attackTick % 2 == 0) particleBurst(ParticleTypes.SOUL_FIRE_FLAME, origin, 5, 0.4, 0.02);
            if (attackTick == 29 && t != null) beamDir = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(origin).normalize();
        } else if (attackTick < 95) {
            if (beamDir.lengthSqr() < 0.1) beamDir = forward();
            if (t != null) {
                Vec3 want = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(origin).normalize();
                beamDir = beamDir.lerp(want, 0.045).normalize();
            }
            Vec3 end = origin.add(beamDir.scale(42));
            BlockHitResult hit = level().clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            Vec3 stop = hit.getLocation();
            double len = origin.distanceTo(stop);
            if (level() instanceof ServerLevel sl) {
                for (double d = 0; d < len; d += 0.9) {
                    Vec3 p = origin.add(beamDir.scale(d));
                    sl.sendParticles((int) (d / 0.9) % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                }
                sl.sendParticles(ParticleTypes.LAVA, stop.x, stop.y, stop.z, 3, 0.4, 0.3, 0.4, 0.0);
                sl.sendParticles(ParticleTypes.EXPLOSION, stop.x, stop.y + 0.2, stop.z, 1, 0.3, 0.1, 0.3, 0.0);
            }
            if (attackTick % 4 == 0) playSound(SoundEvents.BEACON_AMBIENT, 4.0F, 0.5F);
            if (attackTick % 5 == 0) {
                AABB box = new AABB(origin, stop).inflate(1.6);
                for (Player p : level().getEntitiesOfClass(Player.class, box, pl -> !pl.isSpectator())) {
                    if (distToSegment(p.position().add(0, 0.9, 0), origin, stop) < 1.4) {
                        p.hurt(damageSources().mobAttack(this), 6.0F);
                        p.setSecondsOnFire(3);
                        if (p instanceof ServerPlayer sp) glitchPlayer(sp, 8, 0.4F);
                    }
                }
            }
        }
        if (attackTick >= 105) endAttack(50);
    }

    private static double distToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double l2 = ab.lengthSqr();
        double t = l2 < 1e-6 ? 0 : Mth.clamp(p.subtract(a).dot(ab) / l2, 0, 1);
        return a.add(ab.scale(t)).distanceTo(p);
    }

    private Vec3 clampToArena(Vec3 v) {
        if (level().dimension() != NesRealm.DIM) return v;
        double r = Math.sqrt(v.x * v.x + v.z * v.z), max = ArenaBuilder.FLOOR_R - 3;
        return r > max ? new Vec3(v.x / r * max, v.y, v.z / r * max) : v;
    }

    // ------------------------------------------------------------------ eruptions (telegraphed ground attacks)
    private static final class Eruption {
        final double x, y, z;
        int delay;
        final float damage, radius;
        final boolean groundOnly;

        Eruption(double x, double y, double z, int delay, float damage, float radius, boolean groundOnly) {
            this.x = x; this.y = y; this.z = z; this.delay = delay; this.damage = damage; this.radius = radius; this.groundOnly = groundOnly;
        }
    }

    private void queueEruption(double x, double y, double z, int delay, float dmg, float radius, boolean groundOnly) {
        eruptions.add(new Eruption(x, y, z, delay, dmg, radius, groundOnly));
    }

    private void tickEruptions() {
        if (eruptions.isEmpty() || !(level() instanceof ServerLevel sl)) return;
        Iterator<Eruption> it = eruptions.iterator();
        while (it.hasNext()) {
            Eruption e = it.next();
            e.delay--;
            if (e.delay > 0) {
                if (!e.groundOnly && e.delay % 3 == 0) {          // red warning ring
                    for (int i = 0; i < 6; i++) {
                        double a = i * Math.PI / 3 + e.delay * 0.3;
                        sl.sendParticles(DustParticleOptions.REDSTONE, e.x + Math.cos(a) * e.radius, e.y + 0.15, e.z + Math.sin(a) * e.radius, 1, 0, 0, 0, 0);
                    }
                }
                continue;
            }
            sl.sendParticles(ParticleTypes.FLAME, e.x, e.y + 0.5, e.z, 22, e.radius * 0.35, 0.6, e.radius * 0.35, 0.09);
            sl.sendParticles(ParticleTypes.LAVA, e.x, e.y + 0.5, e.z, 3, e.radius * 0.3, 0.3, e.radius * 0.3, 0.0);
            if (!e.groundOnly || sl.random.nextInt(3) == 0) sl.playSound(null, BlockPos.containing(e.x, e.y, e.z), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.7F, 0.6F);
            AABB box = new AABB(e.x - e.radius, e.y - 1, e.z - e.radius, e.x + e.radius, e.y + 3, e.z + e.radius);
            for (Player p : sl.getEntitiesOfClass(Player.class, box, pl -> !pl.isSpectator())) {
                if (e.groundOnly && !p.onGround()) continue;
                p.hurt(damageSources().mobAttack(this), e.damage);
                if (!e.groundOnly) p.setSecondsOnFire(4);
            }
            it.remove();
        }
    }

    // ------------------------------------------------------------------ lava floor patches (arena only)
    private static final class Hazard {
        final List<BlockPos> positions = new ArrayList<>();
        final List<BlockState> originals = new ArrayList<>();
        int timer = 36;
        boolean active;
    }

    private void spawnHazard(Vec3 center, int half) {
        if (level().dimension() != NesRealm.DIM || !(level() instanceof ServerLevel sl) || hazards.size() >= 6) return;
        Hazard h = new Hazard();
        BlockPos c = BlockPos.containing(center);
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                BlockPos p = new BlockPos(c.getX() + dx, ArenaBuilder.FLOOR_Y, c.getZ() + dz);
                if (Math.hypot(p.getX(), p.getZ()) > ArenaBuilder.FLOOR_R - 2) continue;
                BlockState st = sl.getBlockState(p);
                if (st.isAir() || st.is(Blocks.LAVA)) continue;
                h.positions.add(p);
                h.originals.add(st);
            }
        }
        if (!h.positions.isEmpty()) hazards.add(h);
    }

    private void tickHazards() {
        if (hazards.isEmpty() || !(level() instanceof ServerLevel sl)) return;
        Iterator<Hazard> it = hazards.iterator();
        while (it.hasNext()) {
            Hazard h = it.next();
            h.timer--;
            if (!h.active) {
                if (tickCount % 3 == 0) for (BlockPos p : h.positions)
                    sl.sendParticles(DustParticleOptions.REDSTONE, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 1, 0.3, 0.05, 0.3, 0.0);
                if (h.timer <= 0) {
                    for (BlockPos p : h.positions) sl.setBlock(p, Blocks.LAVA.defaultBlockState(), 3);
                    h.active = true;
                    h.timer = 110;
                    sl.playSound(null, blockPosition(), SoundEvents.LAVA_POP, SoundSource.HOSTILE, 1.5F, 0.6F);
                }
            } else if (h.timer <= 0) {
                revert(sl, h);
                it.remove();
            }
        }
    }

    private void revert(ServerLevel sl, Hazard h) {
        if (!h.active) return;
        for (int i = 0; i < h.positions.size(); i++) sl.setBlock(h.positions.get(i), h.originals.get(i), 3);
    }

    private void revertAllHazards() {
        if (level() instanceof ServerLevel sl) for (Hazard h : hazards) revert(sl, h);
        hazards.clear();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            revertAllHazards();
            bossEvent.removeAllPlayers();
        }
        super.remove(reason);
    }

    // ------------------------------------------------------------------ helpers
    private void particleBurst(ParticleOptions type, Vec3 pos, int count, double spread, double speed) {
        if (level() instanceof ServerLevel sl) sl.sendParticles(type, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
    }

    private void glitchNearby(int ticks, float intensity) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) < 90 * 90) ModNetwork.sendToPlayer(new GlitchPacket(ticks, intensity), p);
        }
    }

    private void glitchPlayer(LivingEntity e, int ticks, float intensity) {
        if (e instanceof ServerPlayer sp) ModNetwork.sendToPlayer(new GlitchPacket(ticks, intensity), sp);
    }

    private void broadcast(String msg) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) < 100 * 100) p.sendSystemMessage(Component.literal(msg));
        }
    }

    // ------------------------------------------------------------------ client ambience
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            int ph = getPhase();
            if (random.nextInt(2) == 0)
                level().addParticle(ph == 0 ? ParticleTypes.SMOKE : ParticleTypes.FLAME, getRandomX(0.8), getY() + random.nextDouble() * 3.5, getRandomZ(0.8), 0, 0.03, 0);
            if (ph >= 1 && random.nextInt(2) == 0)
                level().addParticle(DustParticleOptions.REDSTONE, getRandomX(1.0), getY() + random.nextDouble() * 4, getRandomZ(1.0), 0, 0, 0);
            if (ph >= 2 && random.nextInt(2) == 0)
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(1.2), getY() + random.nextDouble() * 4, getRandomZ(1.2), 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ sounds
    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.WARDEN_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WARDEN_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 0.9F, 0.6F);
    }

    @Override
    public float getVoicePitch() { return 0.55F; }

    // ------------------------------------------------------------------ death
    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (level().isClientSide) return;
        revertAllHazards();
        eruptions.clear();
        attack = Attack.NONE;
        entityData.set(DATA_ATTACK, 0);
        bossEvent.setProgress(0.0F);
        glitchNearby(110, 1.0F);
        broadcast("\u00a78\u00a7l[ \u00a7r\u00a74\u00a7lG A M E   O V E R\u00a7r \u00a78\u00a7l]  \u00a7r\u00a77- Red is defeated.");
    }

    @Override
    protected void dropAllDeathLoot(DamageSource source) {
        // rewards are handed out at the end of the death animation (see tickDeath)
    }

    @Override
    protected void tickDeath() {
        ++deathTime;
        if (level().isClientSide) {
            for (int i = 0; i < 3; i++)
                level().addParticle(i == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.FLAME, getRandomX(1.2), getY() + random.nextDouble() * 4, getRandomZ(1.2), 0, 0.05, 0);
            return;
        }
        if (!(level() instanceof ServerLevel sl)) return;
        if (deathTime % 6 == 0) {
            sl.sendParticles(ParticleTypes.EXPLOSION, getX() + random.nextGaussian() * 1.5, getY() + 1 + random.nextDouble() * 3, getZ() + random.nextGaussian() * 1.5, 1, 0, 0, 0, 0);
            playSound(SoundEvents.GENERIC_EXPLODE, 2.0F, 0.6F + random.nextFloat() * 0.5F);
        }
        if (deathTime == 40) glitchNearby(60, 1.0F);
        if (deathTime >= 100 && !isRemoved()) {
            boolean inRealm = level().dimension() == NesRealm.DIM;
            List<ServerPlayer> winners = new ArrayList<>();
            for (ServerPlayer p : sl.players()) if (p.distanceToSqr(this) < 100 * 100) winners.add(p);
            for (ServerPlayer p : winners) {
                give(p, new ItemStack(ModItems.HELLBEAST_HEART.get()));
                give(p, new ItemStack(Items.NETHER_STAR));
                give(p, new ItemStack(Items.NETHERITE_SCRAP, 6));
                give(p, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 2));
                p.giveExperiencePoints(1800);
            }
            if (winners.isEmpty()) {
                spawnAtLocation(new ItemStack(ModItems.HELLBEAST_HEART.get()));
                ExperienceOrb.award(sl, position(), 1800);
            }
            remove(RemovalReason.KILLED);
            if (inRealm) for (ServerPlayer p : winners) NesRealm.exit(p);
        }
    }

    private static void give(ServerPlayer p, ItemStack s) {
        p.getInventory().placeItemBackInInventory(s);
    }

    // ------------------------------------------------------------------ save / load
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RedPhase", getPhase());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPhase(tag.getInt("RedPhase"));
    }
}
