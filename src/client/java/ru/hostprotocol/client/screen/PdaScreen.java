package ru.hostprotocol.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import ru.hostprotocol.client.ProtocolClientState;
import ru.hostprotocol.client.fx.GlitchRenderer;
import ru.hostprotocol.item.PdaItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Host Protocol PDA: Day-1 briefing, plus Day-2 infection log when unlocked.
 */
public class PdaScreen extends Screen {
	private static final String[] DAY1_KEYS = {
			"hostprotocol.pda.page1",
			"hostprotocol.pda.page2",
			"hostprotocol.pda.page3",
			"hostprotocol.pda.page4"
	};
	private static final String[] DAY2_KEYS = {
			"hostprotocol.pda.day2.page1",
			"hostprotocol.pda.day2.page2"
	};

	private static final int PANEL_COLOR = 0xE80A0A0E;
	private static final int BORDER = GlitchRenderer.PURPLE;
	private static final int HEADER = 0xFFEDEDED;
	private static final int MUTED = 0xFF8A8A96;
	private static final int BODY = 0xFFC8C8C8;
	private static final int TAB_H = 14;

	private final String subjectId;
	private final boolean day2Unlocked;
	private boolean day2Section;
	private int page;

	public PdaScreen(ItemStack stack) {
		super(Component.translatable("hostprotocol.pda.title"));
		this.subjectId = PdaItem.getSubjectId(stack);
		this.day2Unlocked = PdaItem.hasDay2Log(stack) || ProtocolClientState.hasDay2Log();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);

		int panelW = Math.min(300, this.width - 32);
		int panelH = Math.min(210, this.height - 32);
		int x = (this.width - panelW) / 2;
		int y = (this.height - panelH) / 2;

		graphics.fill(x - 1, y - 1, x + panelW + 1, y + panelH + 1, BORDER);
		graphics.fill(x, y, x + panelW, y + panelH, PANEL_COLOR);
		graphics.fill(x, y, x + panelW, y + 28, 0xFF140A1C);
		graphics.fill(x, y + 28, x + panelW, y + 29, BORDER);

		graphics.drawString(this.font, Component.translatable("hostprotocol.pda.title"), x + 10, y + 6, HEADER, false);
		graphics.drawString(this.font, Component.translatable("hostprotocol.pda.device", subjectId), x + 10, y + 16, GlitchRenderer.PURPLE, false);

		int tabY = y + 32;
		renderTab(graphics, x + 8, tabY, Component.translatable("hostprotocol.pda.tab.day1"), !day2Section, mouseX, mouseY);
		if (day2Unlocked) {
			int tab2X = x + 8 + tabWidth(Component.translatable("hostprotocol.pda.tab.day1")) + 6;
			renderTab(graphics, tab2X, tabY, Component.translatable("hostprotocol.pda.tab.day2"), day2Section, mouseX, mouseY);
		}

		int textX = x + 10;
		int textY = tabY + TAB_H + 8;
		int textW = panelW - 20;
		List<FormattedCharSequence> lines = wrapPage(textW);
		int maxLines = Math.max(1, (panelH - (textY - y) - 28) / 11);
		int shown = Math.min(maxLines, lines.size());
		for (int i = 0; i < shown; i++) {
			graphics.drawString(this.font, lines.get(i), textX, textY + i * 11, BODY, false);
		}

		String[] keys = currentKeys();
		String pageLabel = Component.translatable("hostprotocol.pda.page_of", page + 1, keys.length).getString();
		graphics.drawString(this.font, pageLabel, x + 10, y + panelH - 16, MUTED, false);

		Component hint = page + 1 < keys.length
				? Component.translatable("hostprotocol.pda.page_hint")
				: Component.translatable("hostprotocol.pda.close_hint");
		int hintW = this.font.width(hint);
		graphics.drawString(this.font, hint, x + panelW - 10 - hintW, y + panelH - 16, MUTED, false);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private void renderTab(GuiGraphics graphics, int tx, int ty, Component label, boolean selected, int mouseX, int mouseY) {
		int tw = tabWidth(label);
		boolean hover = mouseX >= tx && mouseX < tx + tw && mouseY >= ty && mouseY < ty + TAB_H;
		int bg = selected ? 0xFF2A1238 : hover ? 0xFF1A0E24 : 0xFF100A14;
		int fg = selected ? GlitchRenderer.PURPLE : MUTED;
		graphics.fill(tx, ty, tx + tw, ty + TAB_H, bg);
		if (selected) {
			graphics.fill(tx, ty + TAB_H - 1, tx + tw, ty + TAB_H, BORDER);
		}
		graphics.drawString(this.font, label, tx + 5, ty + 3, fg, false);
	}

	private int tabWidth(Component label) {
		return this.font.width(label) + 10;
	}

	private String[] currentKeys() {
		return day2Section ? DAY2_KEYS : DAY1_KEYS;
	}

	private List<FormattedCharSequence> wrapPage(int width) {
		String[] keys = currentKeys();
		int index = Math.min(page, keys.length - 1);
		String raw = Component.translatable(keys[index], subjectId).getString();
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
		int panelW = Math.min(300, this.width - 32);
		int panelH = Math.min(210, this.height - 32);
		int x = (this.width - panelW) / 2;
		int y = (this.height - panelH) / 2;
		int tabY = y + 32;
		Component day1 = Component.translatable("hostprotocol.pda.tab.day1");
		int tab1X = x + 8;
		int tab1W = tabWidth(day1);
		if (inside(mouseX, mouseY, tab1X, tabY, tab1W, TAB_H)) {
			selectSection(false);
			return true;
		}
		if (day2Unlocked) {
			int tab2X = tab1X + tab1W + 6;
			int tab2W = tabWidth(Component.translatable("hostprotocol.pda.tab.day2"));
			if (inside(mouseX, mouseY, tab2X, tabY, tab2W, TAB_H)) {
				selectSection(true);
				return true;
			}
		}
		advance();
		return true;
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void selectSection(boolean day2) {
		if (day2Section != day2) {
			day2Section = day2;
			page = 0;
		}
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
			} else if (day2Section) {
				selectSection(false);
				page = DAY1_KEYS.length - 1;
			}
			return true;
		}
		if (day2Unlocked && (keyCode == 265 || keyCode == 87)) { // up / W — Day 1
			selectSection(false);
			return true;
		}
		if (day2Unlocked && (keyCode == 264 || keyCode == 83)) { // down / S — Day 2
			selectSection(true);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void advance() {
		String[] keys = currentKeys();
		if (page + 1 < keys.length) {
			page++;
			return;
		}
		if (!day2Section && day2Unlocked) {
			selectSection(true);
			return;
		}
		onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}
