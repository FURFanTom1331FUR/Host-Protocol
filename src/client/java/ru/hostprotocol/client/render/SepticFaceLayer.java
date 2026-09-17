package ru.hostprotocol.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.entity.SepticEntity;

/**
 * Photorealistic eyeless face glued to Septic's head so looking at the mob is not the old Steve skin.
 */
public class SepticFaceLayer extends RenderLayer<SepticEntity, HumanoidModel<SepticEntity>> {
	private static final ResourceLocation FACE = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/gui/septic_face.png");
	private static final ResourceLocation FACE_DRIP = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/gui/septic_face_drip.png");

	public SepticFaceLayer(RenderLayerParent<SepticEntity, HumanoidModel<SepticEntity>> parent) {
		super(parent);
	}

	@Override
	public void render(
			PoseStack poseStack,
			MultiBufferSource buffer,
			int packedLight,
			SepticEntity entity,
			float limbSwing,
			float limbSwingAmount,
			float partialTicks,
			float ageInTicks,
			float netHeadYaw,
			float headPitch
	) {
		poseStack.pushPose();
		this.getParentModel().head.translateAndRotate(poseStack);
		poseStack.translate(0.0F, -0.18F, -0.52F);
		poseStack.scale(1.55F, 1.85F, 1.55F);
		ResourceLocation tex = ((entity.tickCount / 8) & 1) == 0 ? FACE : FACE_DRIP;
		VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(tex));
		Matrix4f pose = poseStack.last().pose();
		Matrix3f normal = poseStack.last().normal();
		int light = LightTexture.FULL_BRIGHT;
		quad(consumer, pose, normal, light);
		poseStack.popPose();
	}

	private static void quad(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int light) {
		float s = 0.5F;
		vert(consumer, pose, normal, -s, -s, 0.0F, 0.0F, 1.0F, light);
		vert(consumer, pose, normal, -s, s, 0.0F, 0.0F, 0.0F, light);
		vert(consumer, pose, normal, s, s, 0.0F, 1.0F, 0.0F, light);
		vert(consumer, pose, normal, s, -s, 0.0F, 1.0F, 1.0F, light);
	}

	private static void vert(
			VertexConsumer consumer,
			Matrix4f pose,
			Matrix3f normal,
			float x,
			float y,
			float z,
			float u,
			float v,
			int light
	) {
		consumer.vertex(pose, x, y, z)
				.color(255, 255, 255, 255)
				.uv(u, v)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(light)
				.normal(normal, 0.0F, 0.0F, -1.0F)
				.endVertex();
	}
}
