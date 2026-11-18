package com.redhellbeast.network;

import com.redhellbeast.world.NesRealm;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: the jumpscare ended, suck me into the game. (Server validates a pending sequence.) */
public class GameOverFinishedPacket {
    public void encode(FriendlyByteBuf buf) {}

    public static GameOverFinishedPacket decode(FriendlyByteBuf buf) {
        return new GameOverFinishedPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) NesRealm.onGameOverFinished(sender);
        });
        ctx.get().setPacketHandled(true);
    }
}
