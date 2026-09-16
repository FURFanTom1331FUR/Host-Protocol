package ru.hostprotocol.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.hostprotocol.sound.ModSounds;

/**
 * Brief Host Protocol title overlay when a new Minecraft day begins.
 */
public final class DayAnnounceClientFx {
	private static final int DURATION_TICKS = 70;

	private static int ticksLeft;
	private static int day = 1;

	private DayAnnounceClientFx() {}

	public static void play(Minecraft minecraft, int newDay) {
		day = Math.max(1, newDay);
		ticksLeft = DURATION_TICKS;
		if (minecraft != null) {
			try {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_HIT, 0.85F, 0.65F));
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_STATIC, 1.4F, 0.25F));
			} catch (Exception ignored) {
				// missing ogg should not crash
			}
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
		float fadeIn = Mth.clamp(elapsed / 8.0F, 0.0F, 1.0F);
		float fadeOut = Mth.clamp(ticksLeft / 14.0F, 0.0F, 1.0F);
		float alpha = fadeIn * fadeOut;
		int veil = (int) (150 * alpha);
		graphics.fill(0, 0, w, h, veil << 24);
		GlitchRenderer.render(graphics, w, h, elapsed, 0.22F * alpha);

		int barH = 42;
		int barY = h / 2 - barH / 2;
		graphics.fill(0, barY - 1, w, barY + barH + 1, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, (int) (180 * alpha)));
		graphics.fill(0, barY, w, barY + barH, GlitchRenderer.withAlpha(0x0A0A0E, (int) (220 * alpha)));

		int jx = (int) (GlitchRenderer.textJitterX(elapsed) * (elapsed < 16 ? 1 : 0.25F));
		int color = GlitchRenderer.withAlpha(0xEDEDED, (int) (255 * alpha));
		int accent = GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, (int) (255 * alpha));
		Component line = Component.translatable("hostprotocol.day.begin", day);
		Component sub = Component.translatable("hostprotocol.day.begin.sub");
		graphics.drawCenteredString(minecraft.font, line, w / 2 + jx, barY + 10, color);
		graphics.drawCenteredString(minecraft.font, sub, w / 2, barY + 24, accent);
	}
}
