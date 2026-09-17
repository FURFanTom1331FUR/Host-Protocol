package ru.hostprotocol.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hostprotocol.client.fx.InfectionAtmosphereClient;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Shadow
	private static float fogRed;
	@Shadow
	private static float fogGreen;
	@Shadow
	private static float fogBlue;

	@Inject(method = "setupColor", at = @At("TAIL"))
	private static void hostprotocol$infectionFogColor(Camera camera, float partialTicks, ClientLevel level, int renderDistanceChunks, float bossColorModifier, CallbackInfo ci) {
		float a = InfectionAtmosphereClient.fogAmount();
		if (a <= 0.01F) {
			return;
		}
		fogRed = fogRed * (1.0F - a) + 0.10F * a;
		fogGreen = fogGreen * (1.0F - a) + 0.01F * a;
		fogBlue = fogBlue * (1.0F - a) + 0.14F * a;
		RenderSystem.setShaderFogColor(fogRed, fogGreen, fogBlue);
	}

	@Inject(method = "setupFog", at = @At("TAIL"))
	private static void hostprotocol$infectionFogDistance(Camera camera, FogRenderer.FogMode fogMode, float farPlaneDistance, boolean shouldCreateFog, float partialTick, CallbackInfo ci) {
		float a = InfectionAtmosphereClient.fogAmount();
		if (a <= 0.01F) {
			return;
		}
		float start = farPlaneDistance * (0.18F - 0.10F * a);
		float end = farPlaneDistance * (0.72F - 0.28F * a);
		RenderSystem.setShaderFogStart(Math.max(2.0F, start));
		RenderSystem.setShaderFogEnd(Math.max(start + 8.0F, end));
	}
}
