package ru.hostprotocol.client.fx;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/**
 * Scanlines, slice jitter, and purple/black flashes used during materialization.
 */
public final class GlitchRenderer {
	/** #7b2cbf */
	public static final int PURPLE_RGB = 0x7B2CBF;
	public static final int PURPLE = 0xFF7B2CBF;
	public static final int BLACK = 0xFF000000;

	private GlitchRenderer() {}

	public static void render(GuiGraphics graphics, int width, int height, int ticks, float intensity) {
		if (width <= 0 || height <= 0) {
			return;
		}
		intensity = Math.max(0.0F, Math.min(1.0F, intensity));
		Random random = new Random(ticks * 9973L + 17L);

		int veilAlpha = (int) (140 * intensity);
		graphics.fill(0, 0, width, height, (veilAlpha << 24));

		int scanJitter = (int) ((random.nextInt(7) - 3) * intensity);
		int step = 3;
		for (int y = (ticks * 2) % step; y < height; y += step) {
			int a = (int) ((0x22 + ((y * 17 + ticks * 3) & 31)) * intensity);
			graphics.fill(scanJitter, y, width + scanJitter, y + 1, (Math.min(255, a) << 24));
		}

		if (random.nextFloat() < 0.22F * intensity) {
			int color = random.nextBoolean() ? withAlpha(PURPLE_RGB, 0xC0) : withAlpha(0x000000, 0xE0);
			int gy = random.nextInt(height);
			int gh = 2 + random.nextInt(16);
			int gx = random.nextInt(25) - 12;
			graphics.fill(gx, gy, width + gx, Math.min(height, gy + gh), color);
		}

		int tears = 2 + (int) (4 * intensity);
		for (int i = 0; i < tears; i++) {
			int sx = random.nextInt(width);
			int sy = random.nextInt(height);
			int tw = 16 + random.nextInt(Math.max(8, width / 5));
			int th = 1 + random.nextInt(8);
			int color = random.nextBoolean() ? withAlpha(PURPLE_RGB, 0x90 + random.nextInt(0x50)) : withAlpha(0x000000, 0xC0);
			graphics.fill(sx, sy, Math.min(width, sx + tw), Math.min(height, sy + th), color);
		}

		if (random.nextFloat() < 0.10F * intensity) {
			int a = 0x30 + random.nextInt(0x60);
			graphics.fill(0, 0, width, height, withAlpha(PURPLE_RGB, a));
		} else if (random.nextFloat() < 0.06F * intensity) {
			graphics.fill(0, 0, width, height, withAlpha(0x000000, 0xA0));
		}
	}

	public static int textJitterX(int ticks) {
		Random random = new Random(ticks * 131L + 9L);
		return random.nextInt(11) - 5;
	}

	public static int textJitterY(int ticks) {
		Random random = new Random(ticks * 191L + 3L);
		return random.nextInt(7) - 3;
	}

	public static int withAlpha(int rgb, int alpha) {
		return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
	}
}
