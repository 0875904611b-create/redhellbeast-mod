package com.redhellbeast.network;

import com.redhellbeast.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: start the "GAME OVER" sequence + jumpscare. */
public class OpenGameOverPacket {
    public void encode(FriendlyByteBuf buf) {}

    public static OpenGameOverPacket decode(FriendlyByteBuf buf) {
        return new OpenGameOverPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openGameOver));
        ctx.get().setPacketHandled(true);
    }
}
