package com.redhellbeast.client;

import com.redhellbeast.RedHellbeastMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.ThreadLocalRandom;

/** Screen glitch (random bars, scanlines, flashes, fake error text) + camera shake while Red is angry. */
@Mod.EventBusSubscriber(modid = RedHellbeastMod.MODID, value = Dist.CLIENT)
public final class GlitchOverlay {
    private static int ticksLeft;
    private static float intensity;

    public static void start(int ticks, float inten) {
        ticksLeft = Math.max(ticksLeft, ticks);
        intensity = Math.max(effective(), inten);
    }

    private static float effective() {
        return ticksLeft <= 0 ? 0F : intensity * Math.min(1F, ticksLeft / 20F);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (ticksLeft > 0 && !Minecraft.getInstance().isPaused()) {
            ticksLeft--;
            if (ticksLeft == 0) intensity = 0F;
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        float i = effective();
        if (i <= 0.01F) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        e.setYaw(e.getYaw() + (r.nextFloat() - 0.5F) * i * 5F);
        e.setPitch(e.getPitch() + (r.nextFloat() - 0.5F) * i * 4F);
        e.setRoll(e.getRoll() + (r.nextFloat() - 0.5F) * i * 8F);
    }

    /** Called from the registered GUI overlay. */
    public static void render(GuiGraphics g, int w, int h) {
        float i = effective();
        if (i <= 0.01F) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int bars = (int) (6 + 40 * i);
        for (int n = 0; n < bars; n++) {
            int y = r.nextInt(h), bh = 1 + r.nextInt(2 + (int) (6 * i));
            int x = r.nextInt(w), bw = 20 + r.nextInt(Math.max(21, w / 2));
            int a = (int) (Mth.clamp(40 + r.nextInt(120) * i, 0, 200));
            int pick = r.nextInt(5);
            int col = pick == 0 ? 0x00FF0000 : pick == 1 ? 0x00000000 : pick == 2 ? 0x00FFFFFF : pick == 3 ? 0x00FF2020 : 0x0000FFFF;
            if (pick == 2 || pick == 4) a = a / 3;
            g.fill(x, y, Math.min(w, x + bw), y + bh, (a << 24) | col);
        }
        for (int y = 0; y < h; y += 3) g.fill(0, y, w, y + 1, ((int) (30 * i) << 24));
        if (r.nextFloat() < 0.06F * i) g.fill(0, 0, w, h, ((int) (70 * i) << 24) | 0x00FF0000);
        if (i > 0.5F && r.nextFloat() < 0.12F) {
            String[] msgs = {"GAME OVER", "ERR", "0xDEAD", "RED", "NO CARRIER", "I SEE YOU"};
            g.drawString(Minecraft.getInstance().font, msgs[r.nextInt(msgs.length)], r.nextInt(Math.max(1, w - 60)), r.nextInt(Math.max(1, h - 10)), 0xFFFF2020, false);
        }
    }

    private GlitchOverlay() {}
}
