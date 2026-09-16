package ru.hostprotocol.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.hostprotocol.sound.ModSounds;

/**
 * Full-screen lock of infection coordinates — title + coords, not chat-only.
 */
public final class CoordsUnlockClientFx {
	private static final int DURATION_TICKS = 90;

	private static int ticksLeft;
	private static int x;
	private static int y;
	private static int z;

	private CoordsUnlockClientFx() {}

	public static boolean isActive() {
		return ticksLeft > 0;
	}

	public static void play(Minecraft minecraft, int fx, int fy, int fz) {
		x = fx;
		y = fy;
		z = fz;
		ticksLeft = DURATION_TICKS;
		if (minecraft == null) {
			return;
		}
		try {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_HIT, 0.55F, 0.9F));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_CONNECTION_ERROR, 0.7F, 0.55F));
		} catch (Exception ignored) {
			// missing ogg must not crash the lock
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
		float fadeOut = Mth.clamp(ticksLeft / 16.0F, 0.0F, 1.0F);
		float alpha = fadeIn * fadeOut;
		int veil = (int) (200 * alpha);
		graphics.fill(0, 0, w, h, veil << 24);
		GlitchRenderer.render(graphics, w, h, elapsed, 0.45F * alpha);

		int barH = 56;
		int barY = h / 2 - barH / 2;
		graphics.fill(0, barY - 2, w, barY + barH + 2, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, (int) (200 * alpha)));
		graphics.fill(0, barY, w, barY + barH, GlitchRenderer.withAlpha(0x0A0A0E, (int) (235 * alpha)));

		int jx = GlitchRenderer.textJitterX(elapsed);
		int color = GlitchRenderer.withAlpha(0xEDEDED, (int) (255 * alpha));
		int accent = GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, (int) (255 * alpha));
		graphics.drawCenteredString(minecraft.font, Component.translatable("hostprotocol.infection.coords.title"),
				w / 2 + jx, barY + 10, color);
		graphics.drawCenteredString(minecraft.font, Component.translatable("hostprotocol.infection.coords.subtitle", x, y, z),
				w / 2, barY + 26, accent);
		graphics.drawCenteredString(minecraft.font, Component.translatable("hostprotocol.infection.coords.hint"),
				w / 2, barY + 40, GlitchRenderer.withAlpha(0x9A9A9A, (int) (220 * alpha)));
	}
}
