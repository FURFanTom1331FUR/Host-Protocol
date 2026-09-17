package ru.hostprotocol.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.hostprotocol.client.breach.BreachDesktop;
import ru.hostprotocol.client.sound.LoopingUiSound;
import ru.hostprotocol.sound.ModSounds;

/**
 * Day-3 stay punishment: sharp interference, female glitch VO, long busy tone, then server kick.
 */
public final class Day3DisconnectClientFx {
	private static final int DURATION_TICKS = 100;

	private static int ticksLeft;
	private static LoopingUiSound tone;
	private static LoopingUiSound staticLoop;

	private Day3DisconnectClientFx() {}

	public static boolean isActive() {
		return ticksLeft > 0;
	}

	public static void play(Minecraft minecraft) {
		cancel(minecraft);
		ticksLeft = DURATION_TICKS;
		BreachDesktop.writeSignalLost();
		if (minecraft == null) {
			return;
		}
		try {
			staticLoop = new LoopingUiSound(ModSounds.VOICE_GLITCH_STATIC, 0.45F);
			tone = new LoopingUiSound(ModSounds.VOICE_SIGNAL_LOST_TONE, 0.95F, 1.0F);
			minecraft.getSoundManager().play(staticLoop);
			minecraft.getSoundManager().play(tone);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_SIGNAL_LOST, 1.0F, 1.0F));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_SYSTEM_ERROR_SHORT, 1.0F, 1.05F));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_BUNKER_SIREN, 1.2F, 0.12F));
		} catch (Exception ignored) {
			// missing ogg must not crash the kick beat
		}
	}

	public static void cancel(Minecraft minecraft) {
		ticksLeft = 0;
		stopLoops(minecraft);
	}

	public static void clientTick(Minecraft minecraft) {
		if (ticksLeft <= 0) {
			return;
		}
		ticksLeft--;
		if (ticksLeft <= 0) {
			stopLoops(minecraft);
		}
	}

	public static void render(GuiGraphics graphics, Minecraft minecraft) {
		if (ticksLeft <= 0 || minecraft == null) {
			return;
		}
		int w = minecraft.getWindow().getGuiScaledWidth();
		int h = minecraft.getWindow().getGuiScaledHeight();
		int elapsed = DURATION_TICKS - ticksLeft;
		float fadeIn = Mth.clamp(elapsed / 4.0F, 0.0F, 1.0F);
		float fadeOut = Mth.clamp(ticksLeft / 8.0F, 0.0F, 1.0F);
		float alpha = Math.max(0.55F, fadeIn * fadeOut);
		graphics.fill(0, 0, w, h, GlitchRenderer.withAlpha(0x120308, (int) (230 * alpha)));
		GlitchRenderer.render(graphics, w, h, elapsed, 0.95F * alpha);

		int barH = 58;
		int barY = h / 2 - barH / 2;
		graphics.fill(0, barY - 4, w, barY + barH + 4, GlitchRenderer.withAlpha(0x7B0010, (int) (210 * alpha)));
		graphics.fill(0, barY, w, barY + barH, GlitchRenderer.withAlpha(0x0A0A0E, (int) (245 * alpha)));
		int jx = GlitchRenderer.textJitterX(elapsed);
		graphics.drawCenteredString(minecraft.font, Component.translatable("hostprotocol.day3.title"),
				w / 2 + jx, barY + 12, GlitchRenderer.withAlpha(0xFFE0E0, 255));
		graphics.drawCenteredString(minecraft.font, Component.translatable("hostprotocol.day3.subtitle"),
				w / 2, barY + 28, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, 255));
	}

	private static void stopLoops(Minecraft minecraft) {
		if (tone != null) {
			tone.requestStop();
			tone = null;
		}
		if (staticLoop != null) {
			staticLoop.requestStop();
			staticLoop = null;
		}
		if (minecraft != null) {
			minecraft.getSoundManager().stop(ModSounds.VOICE_SIGNAL_LOST_TONE.getLocation(), net.minecraft.sounds.SoundSource.MASTER);
			minecraft.getSoundManager().stop(ModSounds.VOICE_GLITCH_STATIC.getLocation(), net.minecraft.sounds.SoundSource.MASTER);
		}
	}
}
