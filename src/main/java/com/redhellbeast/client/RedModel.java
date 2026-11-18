package com.redhellbeast.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.redhellbeast.RedHellbeastMod;
import com.redhellbeast.entity.RedEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Red: raptor-like body, a sad stone mask for a face, curling tail, clawed arms and eight spider legs. */
public class RedModel extends EntityModel<RedEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(RedHellbeastMod.MODID, "red"), "main");
    private static final float RAD = (float) Math.PI / 180F;
    private static final float[] FAN = {0.65F, 0.22F, -0.22F, -0.65F};
    private static final float[] LEG_Z = {-8F, -3F, 2F, 7F};
    private static final float[] TAIL_BASE = {0.2F, 0.3F, 0.35F, 0.45F};

    private final ModelPart root, body, neck, head, armR, armL, foreR, foreL;
    private final ModelPart[] tail = new ModelPart[4];
    private final ModelPart[] legU = new ModelPart[8];
    private final ModelPart[] legL = new ModelPart[8];

    public RedModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        neck = body.getChild("neck");
        head = neck.getChild("head");
        armR = body.getChild("armR");
        armL = body.getChild("armL");
        foreR = armR.getChild("foreR");
        foreL = armL.getChild("foreL");
        ModelPart t = body;
        for (int i = 0; i < 4; i++) {
            t = t.getChild("tail" + (i + 1));
            tail[i] = t;
        }
        for (int i = 0; i < 8; i++) {
            legU[i] = body.getChild("legU" + i);
            legL[i] = legU[i].getChild("legL" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6F, -5F, -11F, 12F, 10F, 22F)
                .texOffs(0, 32).addBox(-1F, -8F, -9F, 2F, 3F, 18F),
                PartPose.offset(0F, 6F, 0F));

        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(70, 0).addBox(-3.5F, -10F, -3.5F, 7F, 12F, 7F),
                PartPose.offsetAndRotation(0F, -3F, -10F, 0.55F, 0F, 0F));
        neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 56).addBox(-6F, -13F, -5F, 12F, 15F, 10F),
                PartPose.offsetAndRotation(0F, -9F, -1F, -0.5F, 0F, 0F));

        // tail: 4 chained segments curling upward
        int[][] uv = {{70, 20}, {70, 36}, {70, 52}, {70, 66}};
        float[] size = {5F, 4F, 3F, 2F};
        PartDefinition parent = body;
        for (int i = 0; i < 4; i++) {
            float s = size[i];
            parent = parent.addOrReplaceChild("tail" + (i + 1), CubeListBuilder.create()
                    .texOffs(uv[i][0], uv[i][1]).addBox(-s / 2F, -s / 2F, 0F, s, s, 10F),
                    PartPose.offsetAndRotation(0F, i == 0 ? -1F : 0F, i == 0 ? 10F : 10F, TAIL_BASE[i], 0F, 0F));
        }

        // arms with three claws each
        for (int side = 0; side < 2; side++) {
            String n = side == 0 ? "R" : "L";
            float x = side == 0 ? 6F : -6F;
            PartDefinition arm = body.addOrReplaceChild("arm" + n, CubeListBuilder.create()
                    .texOffs(0, 84).addBox(-1.5F, -1.5F, -9F, 3F, 3F, 9F),
                    PartPose.offsetAndRotation(x, 0F, -9F, 0.35F, 0F, 0F));
            arm.addOrReplaceChild("fore" + n, CubeListBuilder.create()
                    .texOffs(28, 84).addBox(-1.5F, -1.5F, -8F, 3F, 3F, 8F)
                    .texOffs(54, 84).addBox(-1.5F, -0.5F, -13F, 1F, 1F, 5F)
                    .texOffs(54, 84).addBox(-0.5F, -0.5F, -14F, 1F, 1F, 6F)
                    .texOffs(54, 84).addBox(0.5F, -0.5F, -13F, 1F, 1F, 5F),
                    PartPose.offsetAndRotation(0F, 0F, -9F, -0.5F, 0F, 0F));
        }

        // eight spider legs (4 per side), two segments each
        for (int i = 0; i < 8; i++) {
            float side = i < 4 ? 1F : -1F;
            int k = i % 4;
            CubeListBuilder up = CubeListBuilder.create().texOffs(0, 100);
            CubeListBuilder low = CubeListBuilder.create().texOffs(0, 106);
            if (side > 0) { up.addBox(0F, -1F, -1F, 9F, 2F, 2F); low.addBox(0F, -1F, -1F, 22F, 2F, 2F); }
            else { up.addBox(-9F, -1F, -1F, 9F, 2F, 2F); low.addBox(-22F, -1F, -1F, 22F, 2F, 2F); }
            PartDefinition u = body.addOrReplaceChild("legU" + i, up, PartPose.offset(5.5F * side, 2F, LEG_Z[k]));
            u.addOrReplaceChild("legL" + i, low, PartPose.offset(9F * side, 0F, 0F));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static float c01(float v) { return Mth.clamp(v, 0F, 1F); }

    private static float armCurve(float t, float t1, float t2, float t3, float base, float raised, float slam) {
        if (t <= 0) return base;
        if (t < t1) return Mth.lerp(t / t1, base, raised);
        if (t < t2) return Mth.lerp((t - t1) / (t2 - t1), raised, slam);
        if (t < t3) return Mth.lerp((t - t2) / (t3 - t2), slam, base);
        return base;
    }

    @Override
    public void setupAnim(RedEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float breath = Mth.sin(age * 0.09F);
        body.y = 6F + breath * 0.35F;
        body.xRot = body.yRot = body.zRot = 0F;
        neck.xRot = 0.55F + breath * 0.02F;
        head.xRot = -0.5F + headPitch * RAD * 0.6F;
        head.yRot = netHeadYaw * RAD * 0.8F;
        head.zRot = 0F;
        armR.xRot = armL.xRot = 0.35F;
        armR.yRot = armL.yRot = 0F;
        armR.zRot = armL.zRot = 0F;
        foreR.xRot = foreL.xRot = -0.5F;

        for (int i = 0; i < 4; i++) {
            tail[i].xRot = TAIL_BASE[i];
            tail[i].yRot = Mth.sin(age * 0.08F - i * 0.7F) * 0.14F + Mth.cos(limbSwing * 0.3F) * limbSwingAmount * 0.12F;
        }

        for (int i = 0; i < 8; i++) {
            float side = i < 4 ? 1F : -1F;
            int k = i % 4;
            float ph = limbSwing * 0.6662F + k * 1.7F + (side < 0 ? (float) Math.PI : 0F);
            legU[i].yRot = FAN[k] * side + Mth.cos(ph) * 0.5F * limbSwingAmount;
            legU[i].zRot = -0.7F * side - Math.max(0F, Mth.sin(ph)) * 0.4F * limbSwingAmount * side;
            legL[i].zRot = 2.2F * side;
        }

        RedEntity.Attack a = e.getAttack();
        float t = e.animTime(age);
        switch (a) {
            case CLAW, GLITCH_STEP -> {
                armR.xRot = armCurve(t, 10, 13, 26, 0.35F, -1.6F, 0.9F);
                armL.xRot = armCurve(t - 4, 10, 13, 24, 0.35F, -1.6F, 0.9F);
                body.xRot = 0.1F * c01(t / 10F);
            }
            case LAVA_FINGERS -> {
                armR.xRot = armL.xRot = armCurve(t, 20, 24, 45, 0.35F, -2.1F, 1.0F);
                body.xRot = armCurve(t, 20, 24, 45, 0F, -0.15F, 0.2F);
            }
            case SOUL_BURN -> {
                head.xRot -= 0.7F * c01(t / 20F);
                armR.yRot = -0.6F * c01(t / 15F);
                armL.yRot = 0.6F * c01(t / 15F);
                armR.xRot = armL.xRot = -0.2F;
                head.yRot += Mth.sin(age * 2.5F) * 0.08F * c01(t / 30F);
            }
            case SKULLS -> {
                head.xRot += -0.8F * c01(t / 14F) + 1.0F * c01((t - 14F) / 6F) * (1F - c01((t - 40F) / 10F));
            }
            case SUMMON -> {
                float c = c01(t / 15F) * (1F - c01((t - 50F) / 10F));
                armR.yRot = -1.0F * c;
                armL.yRot = 1.0F * c;
                armR.xRot = armL.xRot = 0.35F - 0.95F * c;
                body.y += 1.5F * c;
                head.xRot -= 0.4F * c;
            }
            case HELL_RAIN -> {
                float c = c01(t / 14F) * (1F - c01((t - 70F) / 10F));
                armR.xRot = armL.xRot = 0.35F - 2.7F * c;
                head.xRot -= 0.5F * c;
            }
            case BEAM -> {
                head.xRot -= 0.35F;
                neck.xRot += 0.15F * c01(t / 20F);
                armR.xRot = armL.xRot = 0.1F;
                armR.yRot = -0.5F;
                armL.yRot = 0.5F;
            }
            case CRASH -> {
                float c = c01(t / 20F);
                head.xRot -= 0.9F * c;
                armR.yRot = -1.2F * c;
                armL.yRot = 1.2F * c;
                armR.xRot = armL.xRot = 0.35F - 0.7F * c;
                body.xRot = Mth.sin(age * 3F) * 0.05F * c;
            }
            default -> { }
        }
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        root.render(ps, vc, light, overlay, r, g, b, a);
    }
}
