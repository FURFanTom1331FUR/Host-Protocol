package ru.hostprotocol.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.hostprotocol.sound.ModSounds;

/**
 * First iron-ore scan: assistant VO + lab / MK-II blueprint overlay.
 */
public final class BlueprintUpdateClientFx {
	private static final int DURATION_TICKS = 90;
	private static int ticksLeft;

	private BlueprintUpdateClientFx() {}

	public static boolean isActive() {
		return ticksLeft > 0;
	}

	public static void play(Minecraft minecraft) {
		ticksLeft = DURATION_TICKS;
		if (minecraft == null) {
			return;
		}
		try {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_BLUEPRINT_UPDATE, 1.0F, 1.0F));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_HIT, 1.2F, 0.35F));
		} catch (Exception ignored) {
			// missing ogg must not crash
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
		graphics.fill(0, 0, w, h, GlitchRenderer.withAlpha(0x061018, (int) (140 * alpha)));
		GlitchRenderer.render(graphics, w, h, elapsed, 0.35F * alpha);
		int barH = 58;
		int barY = h / 2 - barH / 2;
		graphics.fill(0, barY, w, barY + barH, GlitchRenderer.withAlpha(0x0A141C, (int) (230 * alpha)));
		int jx = GlitchRenderer.textJitterX(elapsed) / 3;
		graphics.drawCenteredString(minecraft.font, Component.translatable("hostprotocol.scan.iron.title"),
				w / 2 + jx, barY + 10, GlitchRenderer.withAlpha(0xA0FFF0, (int) (255 * alpha)));
		graphics.drawCenteredString(minecraft.font, Component.translatable("hostprotocol.scan.iron.subtitle"),
				w / 2, barY + 26, GlitchRenderer.withAlpha(0xC8C8C8, (int) (255 * alpha)));
	}
}
