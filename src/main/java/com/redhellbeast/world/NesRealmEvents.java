package com.redhellbeast.world;

import com.redhellbeast.RedHellbeastMod;
import com.redhellbeast.entity.RedEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps the boss realm "arcade-pure": no digging, no placing, and nobody gets stuck inside without a boss. */
@Mod.EventBusSubscriber(modid = RedHellbeastMod.MODID)
public final class NesRealmEvents {
    private static final String NO_BOSS = "RedNoBossTicks";

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.getPlayer() != null && e.getPlayer().level().dimension() == NesRealm.DIM && !e.getPlayer().isCreative()) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() != null && e.getEntity().level().dimension() == NesRealm.DIM
                && !(e.getEntity() instanceof net.minecraft.world.entity.player.Player p && p.isCreative())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp)) return;
        if (sp.level().dimension() != NesRealm.DIM || sp.tickCount % 20 != 0) return;
        boolean boss = !sp.level().getEntitiesOfClass(RedEntity.class, sp.getBoundingBox().inflate(100), r -> !r.isRemoved()).isEmpty();
        int n = boss ? 0 : sp.getPersistentData().getInt(NO_BOSS) + 1;
        sp.getPersistentData().putInt(NO_BOSS, n);
        if (n >= 6) {            // 6 seconds with no Red alive -> go home (also fixes relogging inside)
            sp.getPersistentData().putInt(NO_BOSS, 0);
            NesRealm.exit(sp);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        e.getEntity().getPersistentData().putInt(NO_BOSS, 0);
    }

    private NesRealmEvents() {}
}
