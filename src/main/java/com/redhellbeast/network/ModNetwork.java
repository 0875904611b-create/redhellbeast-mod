package com.redhellbeast.network;

import com.redhellbeast.RedHellbeastMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RedHellbeastMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenGameOverPacket.class, OpenGameOverPacket::encode, OpenGameOverPacket::decode, OpenGameOverPacket::handle);
        CHANNEL.registerMessage(id++, GlitchPacket.class, GlitchPacket::encode, GlitchPacket::decode, GlitchPacket::handle);
        CHANNEL.registerMessage(id++, GameOverFinishedPacket.class, GameOverFinishedPacket::encode, GameOverFinishedPacket::decode, GameOverFinishedPacket::handle);
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    private ModNetwork() {}
}
