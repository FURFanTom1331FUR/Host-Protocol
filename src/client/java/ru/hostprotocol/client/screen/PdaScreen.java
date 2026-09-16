package ru.hostprotocol.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import ru.hostprotocol.client.fx.GlitchRenderer;
import ru.hostprotocol.item.PdaItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Day-1 briefing UI for the Host Protocol PDA / КПК.
 */
public class PdaScreen extends Screen {
	private static final String[] PAGE_KEYS = {
			"hostprotocol.pda.page1",
			"hostprotocol.pda.page2",
			"hostprotocol.pda.page3",
			"hostprotocol.pda.page4"
	};

	private static final int PANEL_COLOR = 0xE80A0A0E;
	private static final int BORDER = GlitchRenderer.PURPLE;
	private static final int HEADER = 0xFFEDEDED;
	private static final int MUTED = 0xFF8A8A96;
	private static final int BODY = 0xFFC8C8C8;

	private final String subjectId;
	private int page;

	public PdaScreen(ItemStack stack) {
		super(Component.translatable("hostprotocol.pda.title"));
		this.subjectId = PdaItem.getSubjectId(stack);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);

		int panelW = Math.min(280, this.width - 32);
		int panelH = Math.min(196, this.height - 32);
		int x = (this.width - panelW) / 2;
		int y = (this.height - panelH) / 2;

		graphics.fill(x - 1, y - 1, x + panelW + 1, y + panelH + 1, BORDER);
		graphics.fill(x, y, x + panelW, y + panelH, PANEL_COLOR);
		graphics.fill(x, y, x + panelW, y + 28, 0xFF140A1C);
		graphics.fill(x, y + 28, x + panelW, y + 29, BORDER);

		graphics.drawString(this.font, Component.translatable("hostprotocol.pda.title"), x + 10, y + 6, HEADER, false);
		graphics.drawString(this.font, Component.translatable("hostprotocol.pda.device", subjectId), x + 10, y + 16, GlitchRenderer.PURPLE, false);

		int textX = x + 10;
		int textY = y + 38;
		int textW = panelW - 20;
		List<FormattedCharSequence> lines = wrapPage(textW);
		int maxLines = Math.max(1, (panelH - 70) / 11);
		int shown = Math.min(maxLines, lines.size());
		for (int i = 0; i < shown; i++) {
			graphics.drawString(this.font, lines.get(i), textX, textY + i * 11, BODY, false);
		}

		String pageLabel = Component.translatable("hostprotocol.pda.page_of", page + 1, PAGE_KEYS.length).getString();
		graphics.drawString(this.font, pageLabel, x + 10, y + panelH - 16, MUTED, false);

		Component hint = page + 1 < PAGE_KEYS.length
				? Component.translatable("hostprotocol.pda.page_hint")
				: Component.translatable("hostprotocol.pda.close_hint");
		int hintW = this.font.width(hint);
		graphics.drawString(this.font, hint, x + panelW - 10 - hintW, y + panelH - 16, MUTED, false);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private List<FormattedCharSequence> wrapPage(int width) {
		String raw = Component.translatable(PAGE_KEYS[page], subjectId).getString();
		List<FormattedCharSequence> out = new ArrayList<>();
		for (String paragraph : raw.split("\\n", -1)) {
			if (paragraph.isEmpty()) {
				out.add(FormattedCharSequence.EMPTY);
			} else {
				out.addAll(this.font.split(Component.literal(paragraph), width));
			}
		}
		return out;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		advance();
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 262 || keyCode == 32 || keyCode == 257) { // right, space, enter
			advance();
			return true;
		}
		if (keyCode == 263) { // left
			if (page > 0) {
				page--;
			}
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void advance() {
		if (page + 1 < PAGE_KEYS.length) {
			page++;
		} else {
			onClose();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}
