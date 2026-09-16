package ru.hostprotocol.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hostprotocol.client.fx.MaterializeClientFx;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
	@Unique
	private boolean hostprotocol$pushedJitter;

	@Inject(
			method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At("HEAD"),
			cancellable = true
	)
	private void hostprotocol$materializeHead(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
		hostprotocol$pushedJitter = false;
		if (MaterializeClientFx.shouldSkipPlayerFrame()) {
			ci.cancel();
			return;
		}
		if (MaterializeClientFx.isGlitching()) {
			Vector3d jitter = MaterializeClientFx.playerJitter();
			poseStack.pushPose();
			poseStack.translate(jitter.x, jitter.y, jitter.z);
			hostprotocol$pushedJitter = true;
		}
	}

	@Inject(
			method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At("RETURN")
	)
	private void hostprotocol$materializeReturn(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
		if (hostprotocol$pushedJitter) {
			poseStack.popPose();
			hostprotocol$pushedJitter = false;
		}
	}
}
