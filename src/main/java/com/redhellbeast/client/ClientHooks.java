package com.redhellbeast.client;

import net.minecraft.client.Minecraft;

/** Only ever touched from client-side code paths (via DistExecutor). */
public final class ClientHooks {
    public static void openGameOver() {
        Minecraft.getInstance().setScreen(new GameOverScreen());
    }

    public static void glitch(int ticks, float intensity) {
        GlitchOverlay.start(ticks, intensity);
    }

    private ClientHooks() {}
}
