package ru.hostprotocol.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.entity.SepticEntity;

/**
 * Taller, thinner humanoid — uncanny silhouette, not a Steve clone.
 */
public class SepticModel extends HumanoidModel<SepticEntity> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(HostProtocolMod.id("septic"), "main");

	public SepticModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.0F), 0.0F);
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("head", CubeListBuilder.create()
						.texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, CubeDeformation.NONE),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		root.addOrReplaceChild("hat", CubeListBuilder.create()
						.texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.45F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		root.addOrReplaceChild("body", CubeListBuilder.create()
						.texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 13.0F, 4.0F, new CubeDeformation(-0.15F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		root.addOrReplaceChild("right_arm", CubeListBuilder.create()
						.texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 16.0F, 3.0F, CubeDeformation.NONE),
				PartPose.offset(-5.0F, 1.5F, 0.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create()
						.texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 16.0F, 3.0F, CubeDeformation.NONE),
				PartPose.offset(5.0F, 1.5F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
						.texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 3.0F, 13.0F, 3.0F, CubeDeformation.NONE),
				PartPose.offset(-1.9F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
						.texOffs(16, 48).addBox(-1.0F, 0.0F, -2.0F, 3.0F, 13.0F, 3.0F, CubeDeformation.NONE),
				PartPose.offset(1.9F, 12.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}
}
