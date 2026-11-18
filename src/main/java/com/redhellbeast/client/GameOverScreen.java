package com.redhellbeast.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.redhellbeast.RedHellbeastMod;
import com.redhellbeast.network.GameOverFinishedPacket;
import com.redhellbeast.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Cartridge inserted -> "GAME OVER" -> more and more GAME OVERs (a tunnel, then they assemble into Red's silhouette)
 * -> eyes open -> JUMPSCARE -> you get pulled into the game.
 */
public class GameOverScreen extends Screen {
    private static final ResourceLocation SCARE_CLOSED = new ResourceLocation(RedHellbeastMod.MODID, "textures/gui/red_scare_closed.png");
    private static final ResourceLocation SCARE_OPEN = new ResourceLocation(RedHellbeastMod.MODID, "textures/gui/red_scare_open.png");
    private static final int TEX = 256;
    private static final String WORD = "GAME OVER";

    private static final int T_TUNNEL = 40, T_FACE = 110, T_FACE_DONE = 215, T_SCARE = 250, T_FADE = 300, T_END = 330;

    private int ticks;
    private boolean sent;
    private final long seed = System.nanoTime();
    private final List<int[]> cells = new ArrayList<>();

    public GameOverScreen() {
        super(Component.empty());
        for (int y = 0; y < RedFaceMask.ROWS; y++) {
            String row = RedFaceMask.ROWS_DATA[y];
            for (int x = 0; x < RedFaceMask.COLS; x++) {
                if (row.charAt(x) == '1' && (x * 7 + y * 13) % 3 != 0) cells.add(new int[]{x, y});
            }
        }
        Collections.shuffle(cells, new Random(seed));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    private void play(SoundEvent s, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol));
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks == 2) play(SoundEvents.NOTE_BLOCK_BIT.value(), 0.5F, 1.0F);
        if (ticks == 8) play(SoundEvents.NOTE_BLOCK_BIT.value(), 0.42F, 1.0F);
        if (ticks == 14) play(SoundEvents.NOTE_BLOCK_BIT.value(), 0.35F, 1.0F);
        if (ticks > T_TUNNEL && ticks < T_SCARE) {
            int step = Math.max(2, 12 - (ticks - T_TUNNEL) / 18);
            if (ticks % step == 0) play(SoundEvents.NOTE_BLOCK_BIT.value(), 0.5F + (ticks - T_TUNNEL) / 150F, 0.7F);
        }
        if (ticks >= T_FACE && ticks < T_SCARE && ticks % Math.max(4, 22 - (ticks - T_FACE) / 6) == 0)
            play(SoundEvents.WARDEN_HEARTBEAT, 0.8F, 1.5F);
        if (ticks == T_FACE_DONE) play(SoundEvents.WARDEN_NEARBY_CLOSEST, 0.5F, 1.5F);
        if (ticks == T_SCARE) {
            play(SoundEvents.ENDER_DRAGON_GROWL, 0.5F, 3.0F);
            play(SoundEvents.WARDEN_SONIC_BOOM, 0.6F, 3.0F);
            play(SoundEvents.WARDEN_ROAR, 0.5F, 3.0F);
        }
        if (ticks == T_SCARE + 14) play(SoundEvents.GHAST_SCREAM, 0.4F, 3.0F);
        if (ticks >= T_END && !sent) {
            sent = true;
            ModNetwork.CHANNEL.sendToServer(new GameOverFinishedPacket());
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float t = ticks + pt;
        g.fill(0, 0, width, height, 0xFF000000);
        RandomSource rnd = RandomSource.create(seed + (long) (t * 0.5F) * 7919L);

        float shake = t > 190 ? Math.min(1F, (t - 190) / 60F) : 0F;
        PoseStack ps = g.pose();
        ps.pushPose();
        if (shake > 0) ps.translate((rnd.nextFloat() - 0.5F) * shake * 12F, (rnd.nextFloat() - 0.5F) * shake * 12F, 0F);

        if (t < T_SCARE) renderWords(g, t, rnd);
        else renderScare(g, t, rnd);
        ps.popPose();

        renderStatic(g, t, rnd);
        if (t >= T_FADE) {
            int a = (int) (255 * Mth.clamp((t - T_FADE) / (T_END - T_FADE), 0F, 1F));
            g.fill(0, 0, width, height, a << 24);
        }
    }

    // --------------------------------------------------------------------------------------- words
    private void word(GuiGraphics g, float x, float y, float scale, float rotDeg, int argb) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 0F);
        ps.mulPose(Axis.ZP.rotationDegrees(rotDeg));
        ps.scale(scale, scale, 1F);
        g.drawString(font, WORD, -font.width(WORD) / 2, -4, argb, false);
        ps.popPose();
    }

    private void renderWords(GuiGraphics g, float t, RandomSource rnd) {
        float cx = width / 2F, cy = height / 2F;
        float jx = t > 20 ? (rnd.nextFloat() - 0.5F) * 6F : 0F;

        // 1) tunnel of radially arranged GAME OVERs
        if (t > T_TUNNEL) {
            int rays = 24, depth = 10, total = rays * depth;
            float p = Mth.clamp((t - T_TUNNEL) / 110F, 0F, 1F);
            int shown = (int) (p * p * total);
            float fade = t > T_FACE ? Mth.clamp(1F - (t - T_FACE) / 90F, 0.25F, 1F) : 1F;
            float maxD = Math.max(width, height) * 0.62F;
            for (int i = 0; i < shown; i++) {
                int ray = (i * 7) % rays, k = i / rays;
                float ang = (float) Math.toRadians(ray * 360.0 / rays) + (t > 130 ? (t - 130) * 0.004F : 0F);
                float d = maxD * (float) Math.pow(1F - k / (float) depth, 1.3);
                float s = 0.5F + (1F - k / (float) depth) * 1.7F;
                int a = (int) (255 * fade * (0.45F + 0.55F * (1F - k / (float) depth)));
                word(g, cx + Mth.cos(ang) * d + (rnd.nextFloat() - 0.5F) * 3F, cy + Mth.sin(ang) * d, s, (float) Math.toDegrees(ang), (a << 24) | 0xFF2010);
            }
        }

        // 2) the words assemble into Red's silhouette
        if (t > T_FACE) {
            float p = Mth.clamp((t - T_FACE) / (T_FACE_DONE - T_FACE), 0F, 1F);
            int shown = (int) (p * cells.size());
            float areaH = height * 0.9F;
            float cell = areaH / RedFaceMask.ROWS;
            float areaW = cell * RedFaceMask.COLS;
            float left = (width - areaW) / 2F, top = (height - areaH) / 2F;
            float scale = cell * 1.9F / Math.max(1, font.width(WORD));
            for (int i = 0; i < shown; i++) {
                int[] c = cells.get(i);
                float x = left + (c[0] + 0.5F) * cell + (rnd.nextFloat() - 0.5F) * 1.5F;
                float y = top + (c[1] + 0.5F) * cell;
                int bright = 170 + rnd.nextInt(86);
                word(g, x, y, scale, 0F, 0xFF000000 | (bright << 16) | (rnd.nextInt(3) == 0 ? 0x2020 : 0x0808));
            }
            if (t > T_FACE_DONE - 12) {   // the eyes open
                float ex = Mth.clamp((t - (T_FACE_DONE - 12)) / 12F, 0F, 1F);
                int[][] eyes = {RedFaceMask.EYE_A, RedFaceMask.EYE_B};
                for (int[] e : eyes) {
                    float x = left + (e[0] + 0.5F) * cell, y = top + (e[1] + 0.5F) * cell, r = cell * 0.9F * ex;
                    g.fill((int) (x - r * 2), (int) (y - r * 2), (int) (x + r * 2), (int) (y + r * 2), 0x40FF0000);
                    g.fill((int) (x - r), (int) (y - r * 0.6F), (int) (x + r), (int) (y + r * 0.6F), 0xFFFFFFFF);
                }
            }
        }

        // 3) the main GAME OVER
        float s = t < 40 ? 5F : 5F - Math.min(2.5F, (t - 40) / 30F);
        int a = (int) (t < 8 ? 255 * (t / 8F) : 255);
        if (t < 130 || (int) t % 6 < 4) word(g, cx + jx, cy - (t > T_FACE ? height * 0.38F : 0F), s, 0F, (a << 24) | 0xFF1A1A);
        if (t > T_FACE_DONE && (int) (t / 3) % 2 == 0) g.fill(0, 0, width, height, 0x30FF0000);
    }

    // --------------------------------------------------------------------------------------- jumpscare
    private void renderScare(GuiGraphics g, float t, RandomSource rnd) {
        float p = t - T_SCARE;
        g.fill(0, 0, width, height, p < 3 ? 0xFFFFFFFF : (p < 10 ? 0xFF500000 : 0xFF1A0000));
        float size = height * (1.05F + p * 0.012F + (rnd.nextFloat() - 0.5F) * 0.06F);
        boolean open = p > 4 && ((int) (p / 2) % 2 == 0 || p > 12);
        ResourceLocation tex = open ? SCARE_OPEN : SCARE_CLOSED;
        float x = (width - size) / 2F + (rnd.nextFloat() - 0.5F) * 18F;
        float y = (height - size) / 2F + (rnd.nextFloat() - 0.5F) * 18F;
        RenderSystem.enableBlend();
        g.blit(tex, (int) x, (int) y, (int) size, (int) size, 0F, 0F, TEX, TEX, TEX, TEX);
        // offset red "ghost" copy for the glitchy look
        g.setColor(1F, 0.2F, 0.2F, 0.45F);
        g.blit(tex, (int) x + 14, (int) y - 6, (int) size, (int) size, 0F, 0F, TEX, TEX, TEX, TEX);
        g.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    private void renderStatic(GuiGraphics g, float t, RandomSource rnd) {
        float amt = t < 20 ? 0.15F : (t < T_SCARE ? 0.2F + Math.max(0F, (t - 150) / 220F) : 1.0F);
        int n = (int) (8 + amt * 50);
        for (int i = 0; i < n; i++) {
            int y = rnd.nextInt(height), h = 1 + rnd.nextInt(3), x = rnd.nextInt(width), w = 10 + rnd.nextInt(width / 3);
            int a = 25 + rnd.nextInt((int) (90 * amt) + 1);
            int col = rnd.nextInt(3) == 0 ? 0xFFFFFF : (rnd.nextBoolean() ? 0xFF0000 : 0x000000);
            g.fill(x, y, Math.min(width, x + w), y + h, (a << 24) | col);
        }
        for (int y = 0; y < height; y += 3) g.fill(0, y, width, y + 1, 0x28000000);
    }
}
