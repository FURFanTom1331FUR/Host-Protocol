package ru.hostprotocol.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.hostprotocol.client.ProtocolClientState;
import ru.hostprotocol.client.fx.GlitchRenderer;
import ru.hostprotocol.item.ModItems;
import ru.hostprotocol.item.PdaItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Host Protocol PDA: Day-1 briefing, Day-2 infection log, Day-3 Septic residue,
 * post-breach system fault, scanner blueprint.
 */
public class PdaScreen extends Screen {
	private enum Section {
		DAY1, DAY2, DAY3, SYSTEM, BLUEPRINT
	}

	private static final ItemStack[][] SCANNER_GRID = {
			{new ItemStack(Items.DIAMOND), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COPPER_INGOT)},
			{new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GLASS), new ItemStack(Items.COPPER_INGOT)},
			{new ItemStack(Items.DIAMOND), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COPPER_INGOT)}
	};
	private static final ItemStack SCANNER_RESULT = new ItemStack(ModItems.SCANNER);
	private static final ResourceLocation CRAFTING_TEX = new ResourceLocation("textures/gui/container/crafting_table.png");

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
	private static final String[] DAY3_KEYS = {
			"hostprotocol.pda.day3.page1",
			"hostprotocol.pda.day3.page2",
			"hostprotocol.pda.day3.page3",
			"hostprotocol.pda.day3.page4"
	};
	private static final String[] SYSTEM_KEYS = {
			"hostprotocol.pda.system.page1",
			"hostprotocol.pda.system.page2"
	};
	private static final String[] BLUEPRINT_KEYS = {
			"hostprotocol.pda.blueprint.page1"
	};

	private static final int PANEL_COLOR = 0xE80A0A0E;
	private static final int BORDER = GlitchRenderer.PURPLE;
	private static final int HEADER = 0xFFEDEDED;
	private static final int MUTED = 0xFF8A8A96;
	private static final int BODY = 0xFFC8C8C8;
	private static final int TAB_H = 14;

	private final String subjectId;
	private final boolean day2Unlocked;
	private final boolean day3Unlocked;
	private final boolean systemUnlocked;
	private final boolean blueprintUnlocked;
	private final boolean coordsLocked;
	private final String coordsText;
	private final String garbledId;
	private Section section = Section.DAY1;
	private int page;

	public PdaScreen(ItemStack stack) {
		super(Component.translatable("hostprotocol.pda.title"));
		this.subjectId = PdaItem.getSubjectId(stack);
		this.day2Unlocked = PdaItem.hasDay2Log(stack) || ProtocolClientState.hasDay2Log();
		this.day3Unlocked = PdaItem.hasDay3Log(stack) || ProtocolClientState.hasDay3Log();
		this.systemUnlocked = PdaItem.hasSystemErrorLog(stack) || ProtocolClientState.hasSystemErrorLog();
		this.blueprintUnlocked = PdaItem.hasBlueprints(stack) || ProtocolClientState.hasBlueprints();
		this.coordsLocked = PdaItem.hasDay2Coords(stack) || ProtocolClientState.hasDay2Coords();
		if (PdaItem.hasDay2Coords(stack)) {
			this.coordsText = PdaItem.getFocusX(stack) + " / " + PdaItem.getFocusY(stack) + " / " + PdaItem.getFocusZ(stack);
		} else if (ProtocolClientState.isCoordsDiscovered()) {
			this.coordsText = ProtocolClientState.coordsLabel();
		} else {
			this.coordsText = Component.translatable("hostprotocol.pda.coords.pending").getString();
		}
		this.garbledId = ProtocolClientState.garbledSubjectId();
		if (systemUnlocked) {
			this.section = Section.SYSTEM;
		} else if (day2Unlocked) {
			this.section = Section.DAY2;
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);

		int panelW = Math.min(340, this.width - 32);
		int panelH = Math.min(240, this.height - 32);
		int x = (this.width - panelW) / 2;
		int y = (this.height - panelH) / 2;

		graphics.fill(x - 1, y - 1, x + panelW + 1, y + panelH + 1, BORDER);
		graphics.fill(x, y, x + panelW, y + panelH, PANEL_COLOR);
		graphics.fill(x, y, x + panelW, y + 28, 0xFF140A1C);
		graphics.fill(x, y + 28, x + panelW, y + 29, BORDER);

		graphics.drawString(this.font, Component.translatable("hostprotocol.pda.title"), x + 10, y + 6, HEADER, false);
		graphics.drawString(this.font, Component.translatable("hostprotocol.pda.device", subjectId), x + 10, y + 16, GlitchRenderer.PURPLE, false);

		int tabY = y + 32;
		int tabX = x + 8;
		tabX = renderTab(graphics, tabX, tabY, Component.translatable("hostprotocol.pda.tab.day1"), section == Section.DAY1, mouseX, mouseY);
		if (day2Unlocked) {
			tabX = renderTab(graphics, tabX + 4, tabY, Component.translatable("hostprotocol.pda.tab.day2"), section == Section.DAY2, mouseX, mouseY);
		}
		if (day3Unlocked) {
			tabX = renderTab(graphics, tabX + 4, tabY, Component.translatable("hostprotocol.pda.tab.day3"), section == Section.DAY3, mouseX, mouseY);
		}
		if (systemUnlocked) {
			tabX = renderTab(graphics, tabX + 4, tabY, Component.translatable("hostprotocol.pda.tab.system"), section == Section.SYSTEM, mouseX, mouseY);
		}
		if (blueprintUnlocked) {
			renderTab(graphics, tabX + 4, tabY, Component.translatable("hostprotocol.pda.tab.blueprint"), section == Section.BLUEPRINT, mouseX, mouseY);
		}

		int textX = x + 10;
		int textY = tabY + TAB_H + 8;
		int textW = panelW - 20;

		if (section == Section.BLUEPRINT) {
			renderScannerCraft(graphics, x, y, panelW, panelH, textY);
		} else {
			List<FormattedCharSequence> lines = wrapPage(textW);
			int maxLines = Math.max(1, (panelH - (textY - y) - 28) / 11);
			int shown = Math.min(maxLines, lines.size());
			for (int i = 0; i < shown; i++) {
				graphics.drawString(this.font, lines.get(i), textX, textY + i * 11, BODY, false);
			}
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

	private void renderScannerCraft(GuiGraphics graphics, int panelX, int panelY, int panelW, int panelH, int textY) {
		Component caption = Component.translatable("hostprotocol.pda.blueprint.caption");
		graphics.drawString(this.font, caption, panelX + 10, textY, BODY, false);

		int slot = 18;
		int grid = slot * 3;
		int gap = 22;
		int resultSize = 26;
		int totalW = grid + gap + resultSize;
		int gx = panelX + (panelW - totalW) / 2;
		int gy = textY + 16;

		graphics.blit(CRAFTING_TEX, gx - 1, gy - 1, 29, 16, 54, 54);
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int sx = gx + col * slot;
				int sy = gy + row * slot;
				graphics.renderItem(SCANNER_GRID[row][col], sx + 1, sy + 1);
			}
		}

		int rx = gx + grid + gap;
		int ry = gy + slot;
		graphics.blit(CRAFTING_TEX, rx - 5, ry - 5, 122, 31, 26, 26);
		graphics.renderItem(SCANNER_RESULT, rx, ry);
		Component label = Component.translatable("item.hostprotocol.scanner");
		int labelX = rx + 8 - this.font.width(label) / 2;
		graphics.drawString(this.font, label, labelX, ry + 20, HEADER, false);
	}

	private int renderTab(GuiGraphics graphics, int tx, int ty, Component label, boolean selected, int mouseX, int mouseY) {
		int tw = tabWidth(label);
		boolean hover = mouseX >= tx && mouseX < tx + tw && mouseY >= ty && mouseY < ty + TAB_H;
		int bg = selected ? 0xFF2A1238 : hover ? 0xFF1A0E24 : 0xFF100A14;
		int fg = selected ? GlitchRenderer.PURPLE : MUTED;
		graphics.fill(tx, ty, tx + tw, ty + TAB_H, bg);
		if (selected) {
			graphics.fill(tx, ty + TAB_H - 1, tx + tw, ty + TAB_H, BORDER);
		}
		graphics.drawString(this.font, label, tx + 5, ty + 3, fg, false);
		return tx + tw;
	}

	private int tabWidth(Component label) {
		return this.font.width(label) + 10;
	}

	private String[] currentKeys() {
		return switch (section) {
			case DAY1 -> DAY1_KEYS;
			case DAY2 -> DAY2_KEYS;
			case DAY3 -> DAY3_KEYS;
			case SYSTEM -> SYSTEM_KEYS;
			case BLUEPRINT -> BLUEPRINT_KEYS;
		};
	}

	private List<FormattedCharSequence> wrapPage(int width) {
		String[] keys = currentKeys();
		int index = Math.min(page, keys.length - 1);
		String raw = pageText(keys[index]);
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

	private String pageText(String key) {
		if (section == Section.DAY2 && key.endsWith("page2")) {
			String coords = coordsLocked && coordsText != null
					? coordsText
					: Component.translatable("hostprotocol.pda.coords.pending").getString();
			return Component.translatable(key, coords).getString();
		}
		if (section == Section.DAY3) {
			return Component.translatable(key, subjectId, garbledId).getString();
		}
		if (section == Section.SYSTEM || section == Section.BLUEPRINT) {
			return Component.translatable(key, subjectId).getString();
		}
		return Component.translatable(key, subjectId).getString();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int panelW = Math.min(340, this.width - 32);
		int panelH = Math.min(240, this.height - 32);
		int x = (this.width - panelW) / 2;
		int y = (this.height - panelH) / 2;
		int tabY = y + 32;
		int tabX = x + 8;
		tabX = hitTab(mouseX, mouseY, tabX, tabY, Component.translatable("hostprotocol.pda.tab.day1"), Section.DAY1);
		if (tabX < 0) {
			return true;
		}
		if (day2Unlocked) {
			tabX = hitTab(mouseX, mouseY, tabX + 4, tabY, Component.translatable("hostprotocol.pda.tab.day2"), Section.DAY2);
			if (tabX < 0) {
				return true;
			}
		}
		if (day3Unlocked) {
			tabX = hitTab(mouseX, mouseY, tabX + 4, tabY, Component.translatable("hostprotocol.pda.tab.day3"), Section.DAY3);
			if (tabX < 0) {
				return true;
			}
		}
		if (systemUnlocked) {
			tabX = hitTab(mouseX, mouseY, tabX + 4, tabY, Component.translatable("hostprotocol.pda.tab.system"), Section.SYSTEM);
			if (tabX < 0) {
				return true;
			}
		}
		if (blueprintUnlocked) {
			tabX = hitTab(mouseX, mouseY, tabX + 4, tabY, Component.translatable("hostprotocol.pda.tab.blueprint"), Section.BLUEPRINT);
			if (tabX < 0) {
				return true;
			}
		}
		advance();
		return true;
	}

	private int hitTab(double mouseX, double mouseY, int tabX, int tabY, Component label, Section target) {
		int tw = tabWidth(label);
		if (inside(mouseX, mouseY, tabX, tabY, tw, TAB_H)) {
			selectSection(target);
			return -1;
		}
		return tabX + tw;
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void selectSection(Section next) {
		if (section != next) {
			section = next;
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
			} else {
				Section prev = previousSection();
				if (prev != null) {
					selectSection(prev);
					page = currentKeys().length - 1;
				}
			}
			return true;
		}
		if (keyCode == 265 || keyCode == 87) { // up / W
			Section prev = previousSection();
			if (prev != null) {
				selectSection(prev);
			}
			return true;
		}
		if (keyCode == 264 || keyCode == 83) { // down / S
			Section next = nextSection();
			if (next != null) {
				selectSection(next);
			}
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private Section previousSection() {
		return switch (section) {
			case BLUEPRINT -> systemUnlocked ? Section.SYSTEM : (day3Unlocked ? Section.DAY3 : (day2Unlocked ? Section.DAY2 : Section.DAY1));
			case SYSTEM -> day3Unlocked ? Section.DAY3 : (day2Unlocked ? Section.DAY2 : Section.DAY1);
			case DAY3 -> day2Unlocked ? Section.DAY2 : Section.DAY1;
			case DAY2 -> Section.DAY1;
			case DAY1 -> null;
		};
	}

	private Section nextSection() {
		return switch (section) {
			case DAY1 -> day2Unlocked ? Section.DAY2 : (day3Unlocked ? Section.DAY3 : (systemUnlocked ? Section.SYSTEM : (blueprintUnlocked ? Section.BLUEPRINT : null)));
			case DAY2 -> day3Unlocked ? Section.DAY3 : (systemUnlocked ? Section.SYSTEM : (blueprintUnlocked ? Section.BLUEPRINT : null));
			case DAY3 -> systemUnlocked ? Section.SYSTEM : (blueprintUnlocked ? Section.BLUEPRINT : null);
			case SYSTEM -> blueprintUnlocked ? Section.BLUEPRINT : null;
			case BLUEPRINT -> null;
		};
	}

	private void advance() {
		String[] keys = currentKeys();
		if (page + 1 < keys.length) {
			page++;
			return;
		}
		Section next = nextSection();
		if (next != null) {
			selectSection(next);
			return;
		}
		onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}
