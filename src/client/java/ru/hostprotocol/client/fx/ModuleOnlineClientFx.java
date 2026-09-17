package ru.hostprotocol.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import ru.hostprotocol.sound.ModSounds;

/**
 * First boot of the activated MK-II: player-centered ring, particles, overlay.
 */
public final class ModuleOnlineClientFx {
	private static final int DURATION_TICKS = 70;
	private static int ticksLeft;

	private ModuleOnlineClientFx() {}

	public static boolean isActive() {
		return ticksLeft > 0;
	}

	public static void play(Minecraft minecraft) {
		ticksLeft = DURATION_TICKS;
		if (minecraft == null) {
			return;
		}
		try {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_MODULE_ONLINE, 1.0F, 1.0F));
		} catch (Exception ignored) {
			// ignore
		}
	}

	public static void cancel() {
		ticksLeft = 0;
	}

	public static void clientTick(Minecraft minecraft) {
		if (ticksLeft <= 0) {
			return;
		}
		if (minecraft != null && minecraft.player != null && minecraft.level != null) {
			Player player = minecraft.player;
			int elapsed = DURATION_TICKS - ticksLeft;
			double radius = 0.4 + elapsed * 0.035;
			for (int i = 0; i < 8; i++) {
				double a = (elapsed * 0.25) + i * (Math.PI * 2.0 / 8.0);
				minecraft.level.addParticle(ParticleTypes.END_ROD,
						player.getX() + Math.cos(a) * radius,
						player.getY() + 0.15 + (elapsed % 8) * 0.02,
						player.getZ() + Math.sin(a) * radius,
						0.0, 0.02, 0.0);
			}
		}
		ticksLeft--;
	}

	public static void render(GuiGraphics graphics, Minecraft minecraft) {
		if (ticksLeft <= 0 || minecraft == null) {
			return;
		}
		int w = minecraft.getWindow().getGuiScaledWidth();
		int h = minecraft.getWindow().getGuiScaledHeight();
		int elapsed = DURATION_TICKS - ticksLeft;
		float life = ticksLeft / (float) DURATION_TICKS;
		float alpha = Mth.clamp(life * 1.4F, 0.0F, 1.0F) * Mth.clamp(elapsed / 6.0F, 0.0F, 1.0F);
		int cx = w / 2;
		int cy = h / 2;
		int rings = 5;
		for (int i = 0; i < rings; i++) {
			int r = 12 + i * 10 + elapsed;
			int a = (int) ((0x40 - i * 8) * alpha);
			drawRing(graphics, cx, cy, r, GlitchRenderer.withAlpha(0x80E8E0, Math.max(0, a)));
		}
		graphics.fill(0, cy - 22, w, cy + 22, GlitchRenderer.withAlpha(0x061418, (int) (160 * alpha)));
		graphics.drawCenteredString(minecraft.font,
				net.minecraft.network.chat.Component.translatable("hostprotocol.module.online.title"),
				cx, cy - 10, GlitchRenderer.withAlpha(0xA0FFF0, (int) (255 * alpha)));
		graphics.drawCenteredString(minecraft.font,
				net.minecraft.network.chat.Component.translatable("hostprotocol.module.online.subtitle"),
				cx, cy + 4, GlitchRenderer.withAlpha(0xC8C8C8, (int) (255 * alpha)));
	}

	private static void drawRing(GuiGraphics graphics, int cx, int cy, int r, int color) {
		int steps = Math.max(24, r);
		for (int i = 0; i < steps; i++) {
			double a = i * (Math.PI * 2.0 / steps);
			int x = cx + (int) Math.round(Math.cos(a) * r);
			int y = cy + (int) Math.round(Math.sin(a) * r * 0.55);
			graphics.fill(x, y, x + 1, y + 1, color);
		}
	}
}
