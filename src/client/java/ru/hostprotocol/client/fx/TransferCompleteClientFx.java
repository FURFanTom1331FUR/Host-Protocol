package ru.hostprotocol.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import ru.hostprotocol.sound.ModSounds;

/**
 * Laboratory transfer complete overlay. World SFX is also played from the server.
 */
public final class TransferCompleteClientFx {
	private static final int DURATION_TICKS = 50;
	private static int ticksLeft;

	private TransferCompleteClientFx() {}

	public static boolean isActive() {
		return ticksLeft > 0;
	}

	public static void play(Minecraft minecraft) {
		ticksLeft = DURATION_TICKS;
		if (minecraft == null) {
			return;
		}
		try {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_TRANSFER_COMPLETE, 1.0F, 0.85F));
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
		float alpha = Mth.clamp(ticksLeft / 12.0F, 0.0F, 1.0F) * Mth.clamp(elapsed / 6.0F, 0.0F, 1.0F);
		GlitchRenderer.render(graphics, w, h, elapsed, 0.28F * alpha);
		graphics.fill(0, h / 2 - 18, w, h / 2 + 18, GlitchRenderer.withAlpha(0x082018, (int) (180 * alpha)));
		graphics.drawCenteredString(minecraft.font,
				net.minecraft.network.chat.Component.translatable("hostprotocol.transfer.title"),
				w / 2, h / 2 - 10, GlitchRenderer.withAlpha(0xA0FFF0, (int) (255 * alpha)));
		graphics.drawCenteredString(minecraft.font,
				net.minecraft.network.chat.Component.translatable("hostprotocol.transfer.subtitle"),
				w / 2, h / 2 + 4, GlitchRenderer.withAlpha(0xC8C8C8, (int) (255 * alpha)));
	}
}
