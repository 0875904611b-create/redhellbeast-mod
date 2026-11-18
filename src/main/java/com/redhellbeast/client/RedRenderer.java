package com.redhellbeast.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.redhellbeast.RedHellbeastMod;
import com.redhellbeast.entity.RedEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class RedRenderer extends MobRenderer<RedEntity, RedModel> {
    private static final ResourceLocation[] TEX = {
            new ResourceLocation(RedHellbeastMod.MODID, "textures/entity/red_phase1.png"),
            new ResourceLocation(RedHellbeastMod.MODID, "textures/entity/red_phase2.png"),
            new ResourceLocation(RedHellbeastMod.MODID, "textures/entity/red_phase3.png")
    };

    public RedRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new RedModel(ctx.bakeLayer(RedModel.LAYER)), 2.2F);
        addLayer(new RedGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(RedEntity e) {
        return TEX[Mth.clamp(e.getPhase(), 0, 2)];
    }

    @Override
    protected void scale(RedEntity e, PoseStack ps, float partialTick) {
        ps.scale(1.35F, 1.35F, 1.35F);
    }

    @Override
    protected float getFlipDegrees(RedEntity e) {
        return 0F;   // no falling over: he glitches apart instead
    }

    @Override
    public void render(RedEntity e, float yaw, float partialTick, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        if (e.isGlitching()) {
            RandomSource r = RandomSource.create((e.tickCount / 2) * 31L + e.getId());
            ps.translate((r.nextFloat() - 0.5F) * 0.5F, 0F, (r.nextFloat() - 0.5F) * 0.5F);
            ps.scale(1F + (r.nextFloat() - 0.5F) * 0.25F, 1F + (r.nextFloat() - 0.5F) * 0.12F, 1F);
        }
        super.render(e, yaw, partialTick, ps, buf, light);
        ps.popPose();
    }
}
