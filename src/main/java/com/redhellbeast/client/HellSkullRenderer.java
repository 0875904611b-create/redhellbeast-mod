package com.redhellbeast.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.redhellbeast.RedHellbeastMod;
import com.redhellbeast.entity.HellSkull;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Fullbright billboard skull. */
public class HellSkullRenderer extends EntityRenderer<HellSkull> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(RedHellbeastMod.MODID, "textures/entity/hell_skull.png");

    public HellSkullRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(HellSkull e, float yaw, float partialTick, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        ps.translate(0F, 0.1F, 0F);
        ps.scale(2.4F, 2.4F, 2.4F);
        ps.mulPose(entityRenderDispatcher.cameraOrientation());
        ps.mulPose(Axis.YP.rotationDegrees(180F));
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        vertex(vc, m, n, 0F, 0, 0, 1);
        vertex(vc, m, n, 1F, 0, 1, 1);
        vertex(vc, m, n, 1F, 1, 1, 0);
        vertex(vc, m, n, 0F, 1, 0, 0);
        ps.popPose();
        super.render(e, yaw, partialTick, ps, buf, light);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, int y, int u, int v) {
        vc.vertex(m, x - 0.5F, y - 0.25F, 0F).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(n, 0F, 1F, 0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(HellSkull e) {
        return TEXTURE;
    }
}
