package ru.hostprotocol.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import ru.hostprotocol.client.fx.GlitchRenderer;
import ru.hostprotocol.menu.LabTableMenu;

/**
 * Laboratory table: vanilla-like crafting layout with a 5×5 grid (not 3×3).
 */
public class LabTableScreen extends AbstractContainerScreen<LabTableMenu> {
	private static final int PANEL = 0xE80A0A0E;
	private static final int SLOT_BG = 0xFF0A0A10;
	private static final int SLOT_EDGE = 0xFF2A1238;

	public LabTableScreen(LabTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = LabTableMenu.IMAGE_WIDTH;
		this.imageHeight = LabTableMenu.IMAGE_HEIGHT;
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int x = this.leftPos;
		int y = this.topPos;
		graphics.fill(x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, GlitchRenderer.PURPLE);
		graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL);
		graphics.fill(x, y, x + this.imageWidth, y + 14, 0xFF140A1C);
		graphics.fill(x, y + 14, x + this.imageWidth, y + 15, GlitchRenderer.PURPLE);

		for (var slot : this.menu.slots) {
			int sx = x + slot.x;
			int sy = y + slot.y;
			graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, SLOT_EDGE);
			graphics.fill(sx, sy, sx + 16, sy + 16, SLOT_BG);
		}

		int ax = x + 116;
		int ay = y + LabTableMenu.RESULT_Y + 5;
		graphics.fill(ax, ay + 3, ax + 18, ay + 5, GlitchRenderer.PURPLE);
		graphics.fill(ax + 14, ay, ax + 18, ay + 8, GlitchRenderer.PURPLE);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, this.title, 8, 4, GlitchRenderer.PURPLE, false);
		graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFF8A8A96, false);
	}
}
