package ru.hostprotocol.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hostprotocol.client.breach.BreachClientFlags;
import ru.hostprotocol.client.fx.GlitchRenderer;
import ru.hostprotocol.client.fx.SystemErrorClientFx;
import ru.hostprotocol.sound.ModSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * After protocol breach: corrupt the panorama and replace Quit so it cannot actually exit.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	private static final String[] GLITCH_QUIT = {
			"ВЫХ.Д?",
			"что ты такое",
			"QUIT//ERR",
			"нельзя",
			"ВЫХОД",
			"menu.???"
	};

	@Unique
	private int hostprotocol$glitchTick;

	@Unique
	private boolean hostprotocol$quitReplaced;

	@Unique
	private boolean hostprotocol$sirenPlayed;

	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void hostprotocol$corruptQuit(CallbackInfo ci) {
		hostprotocol$quitReplaced = false;
		if (!BreachClientFlags.isBreached()) {
			return;
		}
		String quit = Component.translatable("menu.quit").getString();
		List<Button> remove = new ArrayList<>();
		for (Renderable renderable : this.renderables) {
			if (renderable instanceof Button button && button.getMessage().getString().equals(quit)) {
				remove.add(button);
			}
		}
		for (Button old : remove) {
			this.removeWidget(old);
			Button fake = Button.builder(Component.literal(GLITCH_QUIT[0]), this::hostprotocol$onFakeQuit)
					.bounds(old.getX(), old.getY(), old.getWidth(), old.getHeight())
					.build();
			this.addRenderableWidget(fake);
			hostprotocol$quitReplaced = true;
		}
		if (BreachClientFlags.consumeTitleSiren() && !hostprotocol$sirenPlayed && this.minecraft != null) {
			hostprotocol$sirenPlayed = true;
			SystemErrorClientFx.playTitleSiren(this.minecraft);
		}
	}

	@Unique
	private void hostprotocol$onFakeQuit(Button button) {
		hostprotocol$glitchTick += 7;
		button.setMessage(Component.literal(GLITCH_QUIT[Math.floorMod(hostprotocol$glitchTick, GLITCH_QUIT.length)]));
		if (this.minecraft != null) {
			try {
				this.minecraft.getSoundManager().play(
						net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_HIT, 0.55F, 1.0F));
			} catch (Exception ignored) {
				// ignore
			}
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void hostprotocol$corruptPanorama(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!BreachClientFlags.isBreached()) {
			return;
		}
		hostprotocol$glitchTick++;
		int w = this.width;
		int h = this.height;
		graphics.fill(0, 0, w, h, GlitchRenderer.withAlpha(0x07040C, 90));
		GlitchRenderer.render(graphics, w, h, hostprotocol$glitchTick, 0.42F);
		graphics.fill(0, 0, w, 18, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, 80));
		graphics.fill(0, h - 22, w, h, GlitchRenderer.withAlpha(0x000000, 140));
		Minecraft mc = this.minecraft;
		if (mc != null) {
			graphics.drawCenteredString(mc.font, Component.translatable("hostprotocol.breach.title.banner"),
					w / 2 + GlitchRenderer.textJitterX(hostprotocol$glitchTick), h - 16,
					GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, 220));
		}
		if (hostprotocol$quitReplaced && (hostprotocol$glitchTick % 12) == 0) {
			String quit = Component.translatable("menu.quit").getString();
			for (Renderable renderable : this.renderables) {
				if (renderable instanceof Button button) {
					String msg = button.getMessage().getString();
					if (!msg.equals(quit) && isGlitchQuit(msg)) {
						button.setMessage(Component.literal(GLITCH_QUIT[Math.floorMod(hostprotocol$glitchTick / 12, GLITCH_QUIT.length)]));
					}
				}
			}
		}
	}

	@Unique
	private static boolean isGlitchQuit(String msg) {
		for (String s : GLITCH_QUIT) {
			if (s.equals(msg)) {
				return true;
			}
		}
		return false;
	}
}
