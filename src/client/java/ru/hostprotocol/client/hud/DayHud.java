package ru.hostprotocol.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import ru.hostprotocol.client.IntroClientState;
import ru.hostprotocol.client.fx.GlitchRenderer;
import ru.hostprotocol.client.screen.IntroScreen;
import ru.hostprotocol.world.ProtocolTime;

/**
 * Compact always-on day counter at the top of the screen.
 */
public final class DayHud {
	private DayHud() {}

	public static void render(GuiGraphics graphics, Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			return;
		}
		if (client.options.hideGui) {
			return;
		}
		if (client.screen instanceof IntroScreen || IntroClientState.isFreezeActive()) {
			return;
		}

		int day = ProtocolTime.dayIndex(client.level.getDayTime());
		Component label = Component.translatable("hostprotocol.hud.day", day);
		int w = client.getWindow().getGuiScaledWidth();
		int textW = client.font.width(label);
		int padX = 6;
		int padY = 3;
		int boxW = textW + padX * 2;
		int boxH = 12 + padY * 2;
		int x = (w - boxW) / 2;
		int y = 4;

		graphics.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, 0x66));
		graphics.fill(x, y, x + boxW, y + boxH, 0xC00A0A0E);
		graphics.drawString(client.font, label, x + padX, y + padY + 1, GlitchRenderer.PURPLE, false);
	}
}
