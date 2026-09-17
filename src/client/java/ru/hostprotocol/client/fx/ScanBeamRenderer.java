package ru.hostprotocol.client.fx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import ru.hostprotocol.item.ModItems;
import ru.hostprotocol.scan.ScanRay;

/**
 * Purple Host Protocol scan beam (line + end-rod motes) while the scanner is held.
 */
public final class ScanBeamRenderer {
	private ScanBeamRenderer() {}

	public static void render(WorldRenderContext context) {
		Minecraft client = Minecraft.getInstance();
		Player player = client.player;
		if (player == null || client.level == null) {
			return;
		}
		if (!player.isUsingItem() || !player.getUseItem().is(ModItems.SCANNER)) {
			return;
		}
		float partial = context.tickDelta();
		Vec3 cam = context.camera().getPosition();
		Vec3 look = player.getViewVector(partial);
		Vec3 start = player.getEyePosition(partial).add(look.scale(0.35)).add(0.0, -0.08, 0.0);
		Vec3 end = ScanRay.beamEnd(player, partial);
		drawBeam(cam, start, end);
	}

	public static void clientTick(Minecraft client) {
		if (client.player == null || client.level == null) {
			return;
		}
		Player player = client.player;
		if (!player.isUsingItem() || !player.getUseItem().is(ModItems.SCANNER)) {
			return;
		}
		if ((player.tickCount & 1) != 0) {
			return;
		}
		Vec3 start = player.getEyePosition(1.0F);
		Vec3 end = ScanRay.beamEnd(player, 1.0F);
		Vec3 delta = end.subtract(start);
		double len = delta.length();
		if (len < 0.05) {
			return;
		}
		Vec3 step = delta.scale(1.0 / Math.max(4.0, len * 3.0));
		Vec3 at = start.add(step);
		for (int i = 0; i < 8 && at.distanceToSqr(start) < len * len; i++) {
			client.level.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, 0.0, 0.01, 0.0);
			client.level.addParticle(ParticleTypes.WITCH, at.x, at.y, at.z, 0.0, 0.0, 0.0);
			at = at.add(step.scale(2.5));
		}
		client.level.addParticle(ParticleTypes.END_ROD, end.x, end.y, end.z, 0.0, 0.04, 0.0);
	}

	private static void drawBeam(Vec3 cam, Vec3 start, Vec3 end) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		RenderSystem.lineWidth(3.0F);

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();
		buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		float r = 0.48F;
		float g = 0.17F;
		float b = 0.75F;
		vertex(buffer, cam, start, r, g, b, 0.95F);
		vertex(buffer, cam, end, 0.72F, 0.45F, 1.0F, 0.35F);
		Vec3 n = end.subtract(start).cross(new Vec3(0.0, 1.0, 0.0));
		if (n.lengthSqr() < 1.0E-6) {
			n = end.subtract(start).cross(new Vec3(1.0, 0.0, 0.0));
		}
		if (n.lengthSqr() > 1.0E-6) {
			n = n.normalize().scale(0.025);
			vertex(buffer, cam, start.add(n), r, g, b, 0.55F);
			vertex(buffer, cam, end.add(n), r, g, b, 0.2F);
			vertex(buffer, cam, start.subtract(n), r, g, b, 0.55F);
			vertex(buffer, cam, end.subtract(n), r, g, b, 0.2F);
		}
		tesselator.end();

		RenderSystem.lineWidth(1.0F);
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static void vertex(BufferBuilder buffer, Vec3 cam, Vec3 pos, float r, float g, float b, float a) {
		buffer.vertex(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z).color(r, g, b, a).endVertex();
	}
}
