package com.redhellbeast.network;

import com.redhellbeast.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: screen glitch + camera shake for a number of ticks. */
public class GlitchPacket {
    private final int ticks;
    private final float intensity;

    public GlitchPacket(int ticks, float intensity) {
        this.ticks = ticks;
        this.intensity = intensity;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(ticks);
        buf.writeFloat(intensity);
    }

    public static GlitchPacket decode(FriendlyByteBuf buf) {
        return new GlitchPacket(buf.readVarInt(), buf.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        final int t = ticks;
        final float i = intensity;
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.glitch(t, i)));
        ctx.get().setPacketHandled(true);
    }
}
