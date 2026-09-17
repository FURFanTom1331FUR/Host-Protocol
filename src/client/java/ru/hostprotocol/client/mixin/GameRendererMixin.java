package ru.hostprotocol.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
 * cannot be missed.
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
				&& !ModuleOnlineClientFx.isActive() && !SepticPresenceClientFx.isLooking()) {
			return;
		}
		if (this.minecraft == null) {
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
		graphics.flush();
	}
}
