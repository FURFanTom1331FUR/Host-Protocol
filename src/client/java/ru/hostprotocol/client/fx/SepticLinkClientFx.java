package ru.hostprotocol.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.hostprotocol.client.sound.LoopingUiSound;
import ru.hostprotocol.sound.ModSounds;

/**
 * Failed handshake: full-screen glitch, titles «ПОДКЛЮЧЕНИЕ… / SEPTIC», SFX, disconnect. ~7s.
 */
public final class SepticLinkClientFx {
	private static int ticksLeft;
	private static int duration = 140;
	private static String subjectId = "—";
	private static String garbledId = "—";
	private static LoopingUiSound staticLoop;
	private static LoopingUiSound hum;

	private SepticLinkClientFx() {}

	public static void play(Minecraft minecraft, String subject, String garbled, int ticks) {
		cancel(minecraft);
		subjectId = subject == null || subject.isEmpty() ? "—" : subject;
		garbledId = garbled == null || garbled.isEmpty() ? "С000Х" : garbled;
		duration = Math.max(120, ticks);
		ticksLeft = duration;
		if (minecraft == null) {
			return;
		}
		try {
			staticLoop = new LoopingUiSound(ModSounds.VOICE_GLITCH_STATIC, 0.85F);
			hum = new LoopingUiSound(ModSounds.VOICE_MATERIALIZE_HUM, 0.9F);
			minecraft.getSoundManager().play(staticLoop);
			minecraft.getSoundManager().play(hum);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_CONNECTION_ERROR, 0.55F, 1.0F));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_HIT, 0.45F, 1.0F));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_SYSTEM_ERROR_SHORT, 0.75F, 0.85F));
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
		if (minecraft != null && (elapsed == 18 || elapsed == 40 || elapsed == 70 || elapsed == 100 || elapsed == 124)) {
			try {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_HIT, 0.7F, 0.95F));
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
		float intensity = Mth.clamp(0.72F + 0.28F * connect, 0.0F, 1.0F);
		if (elapsed > duration * 0.62F) {
			intensity *= 0.65F + 0.35F * life;
		}

		graphics.fill(0, 0, w, h, GlitchRenderer.withAlpha(0x050208, (int) (210 * intensity)));
		int fog = (int) (160 * intensity);
		graphics.fill(0, 0, w, 36, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, fog / 2));
		graphics.fill(0, h - 44, w, h, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, fog / 2));
		graphics.fill(0, 0, 28, h, GlitchRenderer.withAlpha(0x000000, fog));
		graphics.fill(w - 28, 0, w, h, GlitchRenderer.withAlpha(0x000000, fog));

		GlitchRenderer.render(graphics, w, h, elapsed, intensity);

		int barH = 58;
		int barY = h / 2 - barH / 2;
		graphics.fill(0, barY - 4, w, barY + barH + 4, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, (int) (200 * intensity)));
		graphics.fill(0, barY, w, barY + barH, GlitchRenderer.withAlpha(0x0A0A0E, (int) (240 * intensity)));

		int jx = GlitchRenderer.textJitterX(elapsed);
		Component title;
		Component sub;
		if (elapsed < 36) {
			title = Component.translatable("hostprotocol.septic.title.link");
			sub = Component.translatable("hostprotocol.septic.subtitle.link");
		} else if (elapsed < 88) {
			title = Component.translatable("hostprotocol.septic.title.presence");
			sub = Component.translatable("hostprotocol.septic.subtitle.presence");
		} else {
			title = Component.translatable("hostprotocol.septic.title.drop");
			sub = Component.translatable("hostprotocol.septic.subtitle.drop");
		}
		int color = GlitchRenderer.withAlpha(0xEDEDED, (int) (255 * Math.min(1.0F, intensity + 0.2F)));
		int accent = GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, 255);
		graphics.drawCenteredString(minecraft.font, title, w / 2 + jx, barY + 12, color);
		graphics.drawCenteredString(minecraft.font, sub, w / 2, barY + 28, accent);

		if (elapsed >= 40 && elapsed < 110) {
			Component echo = Component.translatable("hostprotocol.septic.hud.echo", garbledId, subjectId);
			graphics.drawCenteredString(minecraft.font, echo, w / 2 + (jx / 2), barY + barH + 12,
					GlitchRenderer.withAlpha(0xC8C8C8, (int) (230 * intensity)));
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
