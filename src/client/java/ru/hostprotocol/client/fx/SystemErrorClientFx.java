package ru.hostprotocol.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.hostprotocol.client.sound.LoopingUiSound;
import ru.hostprotocol.sound.ModSounds;

/**
 * Next-join-after-breach: female system-error voice + light siren under VO + full-screen fault overlay.
 */
public final class SystemErrorClientFx {
	private static final int DURATION_TICKS = 160;

	private static int ticksLeft;
	private static LoopingUiSound siren;

	private SystemErrorClientFx() {}

	public static boolean isActive() {
		return ticksLeft > 0;
	}

	public static void play(Minecraft minecraft) {
		cancel(minecraft);
		ticksLeft = DURATION_TICKS;
		if (minecraft == null) {
			return;
		}
		try {
			siren = new LoopingUiSound(ModSounds.VOICE_BUNKER_SIREN, 0.18F);
			minecraft.getSoundManager().play(siren);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_SYSTEM_ERROR, 1.0F, 1.0F));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_STATIC, 1.4F, 0.22F));
		} catch (Exception ignored) {
			// missing ogg must not crash acknowledgement
		}
	}

	public static void playTitleSiren(Minecraft minecraft) {
		if (minecraft == null) {
			return;
		}
		try {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_BUNKER_SIREN, 1.6F, 0.16F));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_SYSTEM_ERROR_SHORT, 1.0F, 1.0F));
		} catch (Exception ignored) {
			// ignore
		}
	}

	public static void cancel(Minecraft minecraft) {
		ticksLeft = 0;
		if (siren != null) {
			siren.requestStop();
			siren = null;
		}
		if (minecraft != null) {
			minecraft.getSoundManager().stop(ModSounds.VOICE_BUNKER_SIREN.getLocation(), net.minecraft.sounds.SoundSource.MASTER);
		}
	}

	public static void clientTick(Minecraft minecraft) {
		if (ticksLeft <= 0) {
			return;
		}
		ticksLeft--;
		if (ticksLeft <= 0) {
			cancel(minecraft);
		}
	}

	public static void render(GuiGraphics graphics, Minecraft minecraft) {
		if (ticksLeft <= 0 || minecraft == null) {
			return;
		}
		int w = minecraft.getWindow().getGuiScaledWidth();
		int h = minecraft.getWindow().getGuiScaledHeight();
		int elapsed = DURATION_TICKS - ticksLeft;
		float fadeIn = Mth.clamp(elapsed / 10.0F, 0.0F, 1.0F);
		float fadeOut = Mth.clamp(ticksLeft / 20.0F, 0.0F, 1.0F);
		float alpha = fadeIn * fadeOut;
		graphics.fill(0, 0, w, h, GlitchRenderer.withAlpha(0x1A0508, (int) (210 * alpha)));
		GlitchRenderer.render(graphics, w, h, elapsed, 0.75F * alpha);

		int barH = 52;
		int barY = h / 2 - barH / 2;
		graphics.fill(0, barY - 3, w, barY + barH + 3, GlitchRenderer.withAlpha(0x7B0010, (int) (200 * alpha)));
		graphics.fill(0, barY, w, barY + barH, GlitchRenderer.withAlpha(0x0A0A0E, (int) (240 * alpha)));
		int jx = GlitchRenderer.textJitterX(elapsed);
		graphics.drawCenteredString(minecraft.font, Component.translatable("hostprotocol.breach.system.title"),
				w / 2 + jx, barY + 10, GlitchRenderer.withAlpha(0xFFE0E0, (int) (255 * alpha)));
		graphics.drawCenteredString(minecraft.font, Component.translatable("hostprotocol.breach.system.subtitle"),
				w / 2, barY + 26, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, (int) (255 * alpha)));
	}
}
