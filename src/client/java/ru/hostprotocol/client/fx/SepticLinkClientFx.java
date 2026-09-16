package ru.hostprotocol.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.hostprotocol.client.sound.LoopingUiSound;
import ru.hostprotocol.sound.ModSounds;

/**
 * Night-2 failed handshake: purple/glitch overlay, titles, existing SFX. ~6 seconds.
 */
public final class SepticLinkClientFx {
	private static int ticksLeft;
	private static int duration = 120;
	private static String subjectId = "—";
	private static String garbledId = "—";
	private static LoopingUiSound staticLoop;
	private static LoopingUiSound hum;

	private SepticLinkClientFx() {}

	public static void play(Minecraft minecraft, String subject, String garbled, int ticks) {
		cancel(minecraft);
		subjectId = subject == null || subject.isEmpty() ? "—" : subject;
		garbledId = garbled == null || garbled.isEmpty() ? "С000Х" : garbled;
		duration = Math.max(80, ticks);
		ticksLeft = duration;
		if (minecraft == null) {
			return;
		}
		try {
			staticLoop = new LoopingUiSound(ModSounds.VOICE_GLITCH_STATIC, 0.55F);
			hum = new LoopingUiSound(ModSounds.VOICE_MATERIALIZE_HUM, 0.7F);
			minecraft.getSoundManager().play(staticLoop);
			minecraft.getSoundManager().play(hum);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_CONNECTION_ERROR, 0.85F, 0.75F));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_HIT, 0.7F, 0.8F));
		} catch (Exception ignored) {
			// missing ogg must not crash the handshake
		}
	}

	public static void cancel(Minecraft minecraft) {
		ticksLeft = 0;
		stopLoops(minecraft);
	}

	public static boolean isActive() {
		return ticksLeft > 0;
	}

	public static void clientTick(Minecraft minecraft) {
		if (ticksLeft <= 0) {
			return;
		}
		int elapsed = duration - ticksLeft;
		if (minecraft != null && (elapsed == 20 || elapsed == 48 || elapsed == 76 || elapsed == 100)) {
			try {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_HIT, 0.85F, 0.55F));
			} catch (Exception ignored) {
				// ignore
			}
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
		int elapsed = duration - ticksLeft;
		float life = ticksLeft / (float) duration;
		float connect = elapsed < duration * 0.55F ? 1.0F : Mth.clamp(ticksLeft / (duration * 0.45F), 0.0F, 1.0F);
		float intensity = Mth.clamp(0.45F + 0.55F * connect, 0.0F, 1.0F);
		if (elapsed > duration * 0.62F) {
			intensity *= 0.55F + 0.45F * life;
		}

		int veil = (int) (90 * intensity);
		graphics.fill(0, 0, w, h, GlitchRenderer.withAlpha(0x07040C, veil + 40));
		// tiny fog / vignette
		int fog = (int) (110 * intensity);
		graphics.fill(0, 0, w, 28, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, fog / 2));
		graphics.fill(0, h - 36, w, h, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, fog / 2));
		graphics.fill(0, 0, 18, h, GlitchRenderer.withAlpha(0x000000, fog));
		graphics.fill(w - 18, 0, w, h, GlitchRenderer.withAlpha(0x000000, fog));

		GlitchRenderer.render(graphics, w, h, elapsed, intensity);

		int barH = 48;
		int barY = h / 2 - barH / 2;
		graphics.fill(0, barY - 2, w, barY + barH + 2, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, (int) (160 * intensity)));
		graphics.fill(0, barY, w, barY + barH, GlitchRenderer.withAlpha(0x0A0A0E, (int) (230 * intensity)));

		int jx = GlitchRenderer.textJitterX(elapsed);
		Component title;
		Component sub;
		if (elapsed < 28) {
			title = Component.translatable("hostprotocol.septic.title.link");
			sub = Component.translatable("hostprotocol.septic.subtitle.link");
		} else if (elapsed < 72) {
			title = Component.translatable("hostprotocol.septic.title.presence");
			sub = Component.translatable("hostprotocol.septic.subtitle.presence");
		} else {
			title = Component.translatable("hostprotocol.septic.title.drop");
			sub = Component.translatable("hostprotocol.septic.subtitle.drop");
		}
		int color = GlitchRenderer.withAlpha(0xEDEDED, (int) (255 * Math.min(1.0F, intensity + 0.2F)));
		int accent = GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, 255);
		graphics.drawCenteredString(minecraft.font, title, w / 2 + jx, barY + 10, color);
		graphics.drawCenteredString(minecraft.font, sub, w / 2, barY + 24, accent);

		if (elapsed >= 48 && elapsed < 90) {
			Component echo = Component.translatable("hostprotocol.septic.hud.echo", garbledId, subjectId);
			graphics.drawCenteredString(minecraft.font, echo, w / 2 + (jx / 2), barY + barH + 10,
					GlitchRenderer.withAlpha(0x9A9A9A, (int) (220 * intensity)));
		}
	}

	private static void stopLoops(Minecraft minecraft) {
		if (staticLoop != null) {
			staticLoop.requestStop();
			staticLoop = null;
		}
		if (hum != null) {
			hum.requestStop();
			hum = null;
		}
		if (minecraft != null) {
			minecraft.getSoundManager().stop(ModSounds.VOICE_GLITCH_STATIC.getLocation(), net.minecraft.sounds.SoundSource.MASTER);
			minecraft.getSoundManager().stop(ModSounds.VOICE_MATERIALIZE_HUM.getLocation(), net.minecraft.sounds.SoundSource.MASTER);
		}
	}
}
