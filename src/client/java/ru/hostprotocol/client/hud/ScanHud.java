package ru.hostprotocol.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import ru.hostprotocol.client.fx.GlitchRenderer;
import ru.hostprotocol.item.ModItems;
import ru.hostprotocol.scan.ScanHit;
import ru.hostprotocol.scan.ScanRay;
import ru.hostprotocol.scan.ScanTiming;

/**
 * On-screen scan fill while the player holds the scanner.
 */
public final class ScanHud {
	private ScanHud() {}

	public static void render(GuiGraphics graphics, Minecraft client, float partialTick) {
		if (client == null || client.player == null || client.level == null) {
			return;
		}
		if (client.options.hideGui) {
			return;
		}
		if (!client.player.isUsingItem() || !client.player.getUseItem().is(ModItems.SCANNER)) {
			return;
		}
		ScanHit hit = ScanRay.pick(client.player, partialTick);
		if (hit.isEmpty()) {
			return;
		}
		int duration = ScanTiming.durationTicks(hit.isOre());
		int used = client.player.getTicksUsingItem();
		float pct = Math.max(0.0F, Math.min(1.0F, used / (float) duration));
		int percent = Math.round(pct * 100.0F);

		int w = client.getWindow().getGuiScaledWidth();
		int h = client.getWindow().getGuiScaledHeight();
		int barW = 120;
		int barH = 8;
		int x = (w - barW) / 2;
		int y = h - 72;
		graphics.fill(x - 2, y - 12, x + barW + 2, y + barH + 4, 0xC00A0A0E);
		graphics.fill(x - 3, y - 13, x + barW + 3, y + barH + 5, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, 0x55));
		Component label = Component.translatable("hostprotocol.scan.progress", percent);
		int lw = client.font.width(label);
		graphics.drawString(client.font, label, x + (barW - lw) / 2, y - 10, GlitchRenderer.PURPLE, false);
		graphics.fill(x, y, x + barW, y + barH, 0xFF140A1C);
		int fill = Math.round(barW * pct);
		if (fill > 0) {
			graphics.fill(x, y, x + fill, y + barH, GlitchRenderer.PURPLE);
		}
		graphics.fill(x, y, x + barW, y + 1, 0xFF3A1A50);
		graphics.fill(x, y + barH - 1, x + barW, y + barH, 0xFF3A1A50);
	}
}
