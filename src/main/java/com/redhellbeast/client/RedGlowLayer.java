package com.redhellbeast.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.redhellbeast.RedHellbeastMod;
import com.redhellbeast.entity.RedEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Glowing eyes (and the screaming mouth rim in phase 3). */
public class RedGlowLayer extends RenderLayer<RedEntity, RedModel> {
    private static final ResourceLocation GLOW = new ResourceLocation(RedHellbeastMod.MODID, "textures/entity/red_glow.png");
    private static final ResourceLocation GLOW3 = new ResourceLocation(RedHellbeastMod.MODID, "textures/entity/red_glow3.png");

    public RedGlowLayer(RenderLayerParent<RedEntity, RedModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, RedEntity e, float limbSwing, float limbSwingAmount,
                       float partialTick, float age, float headYaw, float headPitch) {
        if (e.isInvisible()) return;
        VertexConsumer vc = buf.getBuffer(RenderType.eyes(e.getPhase() >= 2 ? GLOW3 : GLOW));
        getParentModel().renderToBuffer(ps, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
    }
}
