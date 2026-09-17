package ru.hostprotocol.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.hostprotocol.sound.ModSounds;

/**
 * Short flash when the Day-2 infection focus arms — chat/title are the primary signal.
 */
public final class InfectionActiveClientFx {
	private static final int DURATION_TICKS = 50;

	private static int ticksLeft;

	private InfectionActiveClientFx() {}

	public static boolean isActive() {
		return ticksLeft > 0;
	}

	public static void play(Minecraft minecraft) {
		ticksLeft = DURATION_TICKS;
		if (minecraft == null) {
			return;
		}
		try {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_SYSTEM_ERROR_SHORT, 1.0F, 1.0F));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_STATIC, 1.5F, 0.18F));
		} catch (Exception ignored) {
			// ignore
		}
	}

	public static void cancel() {
		ticksLeft = 0;
	}

	public static void clientTick() {
		if (ticksLeft > 0) {
			ticksLeft--;
		}
	}

	public static void render(GuiGraphics graphics, Minecraft minecraft) {
		if (ticksLeft <= 0 || minecraft == null) {
			return;
		}
		int w = minecraft.getWindow().getGuiScaledWidth();
		int h = minecraft.getWindow().getGuiScaledHeight();
		int elapsed = DURATION_TICKS - ticksLeft;
		float fadeIn = Mth.clamp(elapsed / 6.0F, 0.0F, 1.0F);
		float fadeOut = Mth.clamp(ticksLeft / 12.0F, 0.0F, 1.0F);
		float alpha = fadeIn * fadeOut;
		graphics.fill(0, 0, w, h, GlitchRenderer.withAlpha(0x140818, (int) (180 * alpha)));
		GlitchRenderer.render(graphics, w, h, elapsed, 0.4F * alpha);
		int barH = 44;
		int barY = h / 2 - barH / 2;
		graphics.fill(0, barY, w, barY + barH, GlitchRenderer.withAlpha(0x0A0A0E, (int) (230 * alpha)));
		int jx = GlitchRenderer.textJitterX(elapsed);
		graphics.drawCenteredString(minecraft.font, Component.translatable("hostprotocol.infection.active.title"),
				w / 2 + jx, barY + 10, GlitchRenderer.withAlpha(0xEDEDED, (int) (255 * alpha)));
		graphics.drawCenteredString(minecraft.font, Component.translatable("hostprotocol.infection.active.subtitle"),
				w / 2, barY + 24, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, (int) (255 * alpha)));
	}
}
