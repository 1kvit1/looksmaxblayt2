package com.looksmax.client;

import com.looksmax.LooksMax;
import com.looksmax.LooksUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Рисует скулы, челюсть и подбородок поверх головы. Размер/форма зависят от рейтинга:
 * чем ниже рейтинг, тем более гипертрофированное и "всратое" лицо; высокий рейтинг = острая линия челюсти.
 * Использует текстуру кожи игрока, поэтому работает с любым скином.
 */
public class FaceLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LOC =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(LooksMax.MODID, "face"), "main");

    private final ModelPart cheekL, cheekR, jaw, chin;

    public FaceLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        ModelPart root = models.bakeLayer(LOC);
        cheekL = root.getChild("cheek_l");
        cheekR = root.getChild("cheek_r");
        jaw = root.getChild("jaw");
        chin = root.getChild("chin");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // texOffs(9,11) попадает в область лица на скине -> цвет кожи
        root.addOrReplaceChild("cheek_l", CubeListBuilder.create().texOffs(9, 11).addBox(-1f, -1f, -1f, 2f, 2f, 2f), PartPose.ZERO);
        root.addOrReplaceChild("cheek_r", CubeListBuilder.create().texOffs(9, 11).addBox(-1f, -1f, -1f, 2f, 2f, 2f), PartPose.ZERO);
        root.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(9, 11).addBox(-3f, -1f, -1f, 6f, 2f, 2f), PartPose.ZERO);
        root.addOrReplaceChild("chin", CubeListBuilder.create().texOffs(9, 11).addBox(-1.5f, -1f, -1f, 3f, 2f, 2f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        double s = LooksUtil.score(LooksUtil.get(player));
        float ugly = (float) Mth.clamp((5.0 - s) / 4.0, 0.0, 1.0);  // 1.0 при рейтинге 1.0
        float chad = (float) Mth.clamp((s - 6.0) / 4.0, 0.0, 1.0);  // 1.0 при рейтинге 10.0
        // Молоточек: чем выше уровень скул, тем выше и острее скулы и тем меньше "всратость"
        float hi = Mth.clamp(player.getData(LooksMax.CHEEK.get()) / (float) LooksUtil.MAX_CHEEK, 0f, 1f);
        ugly *= (1f - 0.7f * hi);
        chad = Math.min(1f, chad + 0.6f * hi);
        if (ugly < 0.03f && chad < 0.03f && hi < 0.03f) return;

        // Скулы
        float cheekScale = 0.5f + ugly * 2.6f + chad * 0.5f;
        float cx = 3.0f + ugly * 0.9f;
        float cy = -3.0f + ugly * 0.6f - chad * 1.3f - hi * 1.2f;
        float cz = -3.6f - ugly * 1.4f;
        setup(cheekL, cx, cy, cz, cheekScale, cheekScale, cheekScale);
        setup(cheekR, -cx, cy, cz, cheekScale, cheekScale, cheekScale);

        // Челюсть: у "всратых" широкая и тяжёлая, у чадов узкая и острая
        float jawW = 1.0f + ugly * 0.7f + chad * 0.1f;
        float jawH = 0.7f + ugly * 2.3f - chad * 0.2f;
        setup(jaw, 0f, -0.8f + ugly * 1.0f, -3.3f - ugly * 0.8f, jawW, jawH, 1.0f + ugly * 0.8f);

        // Подбородок: у низких рейтингов выпирает вперёд, у высоких — чёткий и широкий
        float chinW = 0.9f + ugly * 1.8f + chad * 0.9f;
        float chinH = 0.8f + ugly * 2.2f + chad * 0.3f;
        setup(chin, 0f, 0.3f + ugly * 1.6f, -3.8f - ugly * 2.6f - chad * 0.4f, chinW, chinH, 1.0f + ugly * 1.5f);

        pose.pushPose();
        getParentModel().getHead().translateAndRotate(pose);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(player.getSkin().texture()));
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0f);
        cheekL.render(pose, vc, light, overlay);
        cheekR.render(pose, vc, light, overlay);
        jaw.render(pose, vc, light, overlay);
        chin.render(pose, vc, light, overlay);
        pose.popPose();
    }

    private static void setup(ModelPart part, float x, float y, float z, float sx, float sy, float sz) {
        part.setPos(x, y, z);
        part.xScale = sx;
        part.yScale = sy;
        part.zScale = sz;
    }
}
