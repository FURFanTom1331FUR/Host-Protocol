package ru.hostprotocol.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.hostprotocol.client.fx.BlueprintUpdateClientFx;
import ru.hostprotocol.client.fx.CoordsUnlockClientFx;
import ru.hostprotocol.client.fx.Day3DisconnectClientFx;
import ru.hostprotocol.client.fx.InfectionActiveClientFx;
import ru.hostprotocol.client.fx.ModuleOnlineClientFx;
import ru.hostprotocol.client.fx.SepticLinkClientFx;
import ru.hostprotocol.client.fx.SepticPresenceClientFx;
import ru.hostprotocol.client.fx.SystemErrorClientFx;
import ru.hostprotocol.client.fx.TransferCompleteClientFx;

/**
 * Draw Host Protocol cinematics on top of HUD, F1, sleep screens, and pause menus so they
 * cannot be missed. Look-at Septic also narrows FOV and adds a brief camera hitch.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Final
	private RenderBuffers renderBuffers;

	@Inject(method = "render", at = @At("TAIL"))
	private void hostprotocol$protocolOverlays(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
		if (!SepticLinkClientFx.isActive() && !CoordsUnlockClientFx.isActive() && !SystemErrorClientFx.isActive()
				&& !InfectionActiveClientFx.isActive() && !Day3DisconnectClientFx.isActive()
				&& !BlueprintUpdateClientFx.isActive() && !TransferCompleteClientFx.isActive()
				&& !ModuleOnlineClientFx.isActive() && !SepticPresenceClientFx.shouldOverlay()
				&& !ru.hostprotocol.client.fx.HorrorClientFx.shouldOverlay()) {
			return;
		}
		if (this.minecraft == null) {
			return;
		}
		// HudRenderCallback already paints these while the HUD is visible. Draw here for F1 / pause / screens.
		if (!this.minecraft.options.hideGui && this.minecraft.screen == null) {
			return;
		}
		GuiGraphics graphics = new GuiGraphics(this.minecraft, this.renderBuffers.bufferSource());
		InfectionActiveClientFx.render(graphics, this.minecraft);
		SepticLinkClientFx.render(graphics, this.minecraft);
		CoordsUnlockClientFx.render(graphics, this.minecraft);
		SystemErrorClientFx.render(graphics, this.minecraft);
		Day3DisconnectClientFx.render(graphics, this.minecraft);
		BlueprintUpdateClientFx.render(graphics, this.minecraft);
		TransferCompleteClientFx.render(graphics, this.minecraft);
		ModuleOnlineClientFx.render(graphics, this.minecraft);
		SepticPresenceClientFx.render(graphics, this.minecraft);
		ru.hostprotocol.client.fx.HorrorClientFx.render(graphics, this.minecraft);
		graphics.flush();
	}

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void hostprotocol$septicFov(Camera camera, float partialTicks, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
		float narrow = SepticPresenceClientFx.fovNarrow(partialTicks);
		if (narrow <= 0.01F) {
			return;
		}
		cir.setReturnValue(Math.max(12.0D, cir.getReturnValue() - (double) narrow));
	}

	@Inject(method = "bobHurt", at = @At("TAIL"))
	private void hostprotocol$septicShake(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
		float shake = SepticPresenceClientFx.shakeIntensity(partialTicks);
		if (shake <= 0.01F || this.minecraft == null) {
			return;
		}
		float t = this.minecraft.gui.getGuiTicks() + partialTicks;
		poseStack.translate(
				Mth.sin(t * 2.7F) * 0.045F * shake,
				Mth.cos(t * 3.3F) * 0.032F * shake,
				0.0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 4.1F) * 1.8F * shake));
	}
}
