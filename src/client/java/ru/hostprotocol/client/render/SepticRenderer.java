package ru.hostprotocol.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.entity.SepticEntity;

public class SepticRenderer extends HumanoidMobRenderer<SepticEntity, HumanoidModel<SepticEntity>> {
	private static final ResourceLocation TEXTURE = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/entity/septic.png");

	public SepticRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
	}

	@Override
	public ResourceLocation getTextureLocation(SepticEntity entity) {
		return TEXTURE;
	}
}
