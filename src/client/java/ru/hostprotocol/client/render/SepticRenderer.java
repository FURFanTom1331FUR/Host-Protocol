package ru.hostprotocol.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.entity.SepticEntity;

public class SepticRenderer extends HumanoidMobRenderer<SepticEntity, SepticModel> {
	private static final ResourceLocation TEXTURE = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/entity/septic.png");

	public SepticRenderer(EntityRendererProvider.Context context) {
		super(context, new SepticModel(context.bakeLayer(SepticModel.LAYER)), 0.48F);
		this.addLayer(new SepticFaceLayer(this));
	}

	@Override
	protected void scale(SepticEntity entity, PoseStack poseStack, float partialTick) {
		float tall = entity.isHorrorStalker() ? 1.32F : 1.24F;
		poseStack.scale(0.82F, tall, 0.82F);
	}

	@Override
	public ResourceLocation getTextureLocation(SepticEntity entity) {
		return TEXTURE;
	}
}
