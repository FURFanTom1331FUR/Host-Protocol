package ru.hostprotocol.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.hostprotocol.client.ProtocolClientState;
import ru.hostprotocol.client.fx.GlitchRenderer;
import ru.hostprotocol.item.ModItems;
import ru.hostprotocol.item.PdaItem;
import ru.hostprotocol.item.PdaMk2Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Host Protocol PDA: day logs, scanner/lab/MK-II/serum blueprints, and the MK-II assistant pages.
 */
public class PdaScreen extends Screen {
	private enum Section {
		DAY1, DAY2, DAY3, SYSTEM, BLUEPRINT, MODULE, HEALTH, SCANS, ASSISTANT
	}

	private static final ItemStack[][] SCANNER_GRID = {
			{new ItemStack(Items.DIAMOND), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COPPER_INGOT)},
			{new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GLASS), new ItemStack(Items.COPPER_INGOT)},
			{new ItemStack(Items.DIAMOND), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COPPER_INGOT)}
	};
	private static final ItemStack[][] LAB_GRID = {
			{new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT)},
			{new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT)},
			{new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT)}
	};
	private static final ItemStack[][] PDA_MK2_GRID = {
			{new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GLASS), new ItemStack(Items.IRON_INGOT)},
			{new ItemStack(Items.IRON_INGOT), new ItemStack(Items.REDSTONE), new ItemStack(Items.IRON_INGOT)},
			{new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GLASS), new ItemStack(Items.IRON_INGOT)}
	};
	private static final ItemStack[][] SERUM_GRID = {
			{ItemStack.EMPTY, new ItemStack(ModItems.INFECTED_FLESH), ItemStack.EMPTY},
			{new ItemStack(ModItems.INFECTED_FLESH), new ItemStack(Items.GLASS_BOTTLE), new ItemStack(ModItems.INFECTED_FLESH)},
			{ItemStack.EMPTY, new ItemStack(Items.GOLD_NUGGET), ItemStack.EMPTY}
	};
	private static final ItemStack[][] SUIT_GRID = {
			{new ItemStack(ModItems.INFECTED_FLESH), ItemStack.EMPTY, new ItemStack(ModItems.INFECTED_FLESH)},
			{new ItemStack(ModItems.INFECTED_FLESH), new ItemStack(Items.IRON_INGOT), new ItemStack(ModItems.INFECTED_FLESH)},
			{new ItemStack(ModItems.INFECTED_FLESH), new ItemStack(ModItems.INFECTED_FLESH), new ItemStack(ModItems.INFECTED_FLESH)}
	};
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
	private static final String[] MODULE_KEYS = {
			"hostprotocol.pda.mk2.page1",
			"hostprotocol.pda.mk2.page2"
	};
	private static final String[] HEALTH_KEYS = {
			"hostprotocol.pda.health.page1"
	};
	private static final String[] SCANS_KEYS = {
			"hostprotocol.pda.scans.page1"
	};
	private static final String[] ASSISTANT_KEYS = {
			"hostprotocol.pda.assistant.page1",
			"hostprotocol.pda.assistant.page2"
	};

	private static final int PANEL_COLOR = 0xE80A0A0E;
	private static final int BORDER = GlitchRenderer.PURPLE;
	private static final int ACCENT_MK2 = 0xFF80E8E0;
	private static final int HEADER = 0xFFEDEDED;
	private static final int MUTED = 0xFF8A8A96;
	private static final int BODY = 0xFFC8C8C8;
	private static final int TAB_H = 14;

	private final String subjectId;
	private final boolean day2Unlocked;
	private final boolean day3Unlocked;
	private final boolean systemUnlocked;
	private final boolean blueprintUnlocked;
	private final boolean labBlueprintUnlocked;
	private final boolean baseUnlocked;
	private final boolean mk2;
	private final boolean mk2Activated;
	private final boolean coordsLocked;
	private final String coordsText;
	private final String garbledId;
	private final List<String> sensorLogs;
	private Section section = Section.DAY1;
	private int page;

	public PdaScreen(ItemStack stack) {
		super(Component.translatable("hostprotocol.pda.title"));
		this.subjectId = PdaItem.getSubjectId(stack);
		this.mk2 = stack.is(ModItems.PDA_MK2);
		this.mk2Activated = mk2 && PdaMk2Item.isActivated(stack);
		this.day2Unlocked = PdaItem.hasDay2Log(stack) || ProtocolClientState.hasDay2Log();
		this.day3Unlocked = PdaItem.hasDay3Log(stack) || ProtocolClientState.hasDay3Log();
		this.systemUnlocked = PdaItem.hasSystemErrorLog(stack) || ProtocolClientState.hasSystemErrorLog();
		this.blueprintUnlocked = PdaItem.hasBlueprints(stack) || ProtocolClientState.hasBlueprints();
		this.labBlueprintUnlocked = PdaItem.hasLabBlueprints(stack) || ProtocolClientState.hasLabBlueprints();
		this.baseUnlocked = PdaItem.hasBaseBlueprints(stack) || ProtocolClientState.hasBaseBlueprints()
				|| PdaItem.hasTransferred(stack) || ProtocolClientState.hasTransferred();
		this.coordsLocked = PdaItem.hasDay2Coords(stack) || ProtocolClientState.hasDay2Coords();
		this.sensorLogs = PdaItem.getSensorLogs(stack);
		if (PdaItem.hasDay2Coords(stack)) {
			this.coordsText = PdaItem.getFocusX(stack) + " / " + PdaItem.getFocusY(stack) + " / " + PdaItem.getFocusZ(stack);
		} else if (ProtocolClientState.isCoordsDiscovered()) {
			this.coordsText = ProtocolClientState.coordsLabel();
		} else {
			this.coordsText = Component.translatable("hostprotocol.pda.coords.pending").getString();
		}
		this.garbledId = ProtocolClientState.garbledSubjectId();
		if (mk2Activated) {
			this.section = Section.ASSISTANT;
		} else if (mk2) {
			this.section = Section.MODULE;
		} else if (systemUnlocked) {
			this.section = Section.SYSTEM;
		} else if (day2Unlocked) {
			this.section = Section.DAY2;
		}
	}

	private boolean showBlueprintTab() {
		return blueprintUnlocked || labBlueprintUnlocked || baseUnlocked;
	}

	private int accent() {
		return mk2Activated ? ACCENT_MK2 : BORDER;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);

		int panelW = Math.min(360, this.width - 32);
		int panelH = Math.min(248, this.height - 32);
		int x = (this.width - panelW) / 2;
		int y = (this.height - panelH) / 2;

		graphics.fill(x - 1, y - 1, x + panelW + 1, y + panelH + 1, accent());
		graphics.fill(x, y, x + panelW, y + panelH, PANEL_COLOR);
		graphics.fill(x, y, x + panelW, y + 28, 0xFF140A1C);
		graphics.fill(x, y + 28, x + panelW, y + 29, accent());

		Component title = mk2Activated
				? Component.translatable("hostprotocol.pda.mk2.title")
				: mk2
				? Component.translatable("hostprotocol.pda.mk2.title")
				: Component.translatable("hostprotocol.pda.title");
		graphics.drawString(this.font, title, x + 10, y + 6, HEADER, false);
		Component device = mk2Activated
				? Component.translatable("hostprotocol.pda.mk2.device", subjectId)
				: mk2
				? Component.translatable("hostprotocol.pda.mk2.device", subjectId)
				: Component.translatable("hostprotocol.pda.device", subjectId);
		graphics.drawString(this.font, device, x + 10, y + 16, mk2Activated ? ACCENT_MK2 : GlitchRenderer.PURPLE, false);

		int tabY = y + 32;
		renderTabs(graphics, x, tabY, panelW, mouseX, mouseY, false);

		int textX = x + 10;
		int textY = tabY + tabRowCount() * (TAB_H + 2) + 8;
		int textW = panelW - 20;

		if (section == Section.BLUEPRINT) {
			renderBlueprintCraft(graphics, x, panelW, textY);
		} else {
			List<FormattedCharSequence> lines = wrapPage(textW);
			int maxLines = Math.max(1, (panelH - (textY - y) - 28) / 11);
			int shown = Math.min(maxLines, lines.size());
			for (int i = 0; i < shown; i++) {
				graphics.drawString(this.font, lines.get(i), textX, textY + i * 11, BODY, false);
			}
		}

		int pages = pageCount();
		String pageLabel = Component.translatable("hostprotocol.pda.page_of", page + 1, pages).getString();
		graphics.drawString(this.font, pageLabel, x + 10, y + panelH - 16, MUTED, false);

		Component hint = page + 1 < pages
				? Component.translatable("hostprotocol.pda.page_hint")
				: Component.translatable("hostprotocol.pda.close_hint");
		int hintW = this.font.width(hint);
		graphics.drawString(this.font, hint, x + panelW - 10 - hintW, y + panelH - 16, MUTED, false);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private int tabRowCount() {
		return visibleTabs().size() > 5 ? 2 : 1;
	}

	private List<Section> visibleTabs() {
		List<Section> tabs = new ArrayList<>();
		tabs.add(Section.DAY1);
		if (day2Unlocked) {
			tabs.add(Section.DAY2);
		}
		if (day3Unlocked) {
			tabs.add(Section.DAY3);
		}
		if (systemUnlocked) {
			tabs.add(Section.SYSTEM);
		}
		if (showBlueprintTab()) {
			tabs.add(Section.BLUEPRINT);
		}
		if (mk2Activated) {
			tabs.add(Section.HEALTH);
			tabs.add(Section.SCANS);
			tabs.add(Section.ASSISTANT);
		} else if (mk2) {
			tabs.add(Section.MODULE);
		}
		return tabs;
	}

	private Component tabLabel(Section s) {
		return switch (s) {
			case DAY1 -> Component.translatable("hostprotocol.pda.tab.day1");
			case DAY2 -> Component.translatable("hostprotocol.pda.tab.day2");
			case DAY3 -> Component.translatable("hostprotocol.pda.tab.day3");
			case SYSTEM -> Component.translatable("hostprotocol.pda.tab.system");
			case BLUEPRINT -> Component.translatable("hostprotocol.pda.tab.blueprint");
			case MODULE -> Component.translatable("hostprotocol.pda.tab.module");
			case HEALTH -> Component.translatable("hostprotocol.pda.tab.health");
			case SCANS -> Component.translatable("hostprotocol.pda.tab.scans");
			case ASSISTANT -> Component.translatable("hostprotocol.pda.tab.assistant");
		};
	}

	private int renderTabs(GuiGraphics graphics, int x, int tabY, int panelW, int mouseX, int mouseY, boolean hitOnly) {
		int tabX = x + 8;
		int row = 0;
		int maxX = x + panelW - 8;
		for (Section s : visibleTabs()) {
			Component label = tabLabel(s);
			int tw = tabWidth(label);
			if (tabX + tw > maxX) {
				row++;
				tabX = x + 8;
			}
			int ty = tabY + row * (TAB_H + 2);
			if (!hitOnly) {
				renderTab(graphics, tabX, ty, label, section == s, mouseX, mouseY);
			} else if (inside(mouseX, mouseY, tabX, ty, tw, TAB_H)) {
				selectSection(s);
				return -1;
			}
			tabX += tw + 4;
		}
		return 0;
	}

	private void renderBlueprintCraft(GuiGraphics graphics, int panelX, int panelW, int textY) {
		List<BlueprintPage> pages = blueprintPages();
		if (pages.isEmpty()) {
			graphics.drawString(this.font, Component.translatable("hostprotocol.pda.blueprint.empty"), panelX + 10, textY, BODY, false);
			return;
		}
		BlueprintPage current = pages.get(Math.min(page, pages.size() - 1));
		graphics.drawString(this.font, Component.translatable(current.captionKey), panelX + 10, textY, BODY, false);

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
				ItemStack cell = current.grid[row][col];
				if (!cell.isEmpty()) {
					graphics.renderItem(cell, sx + 1, sy + 1);
				}
			}
		}

		int rx = gx + grid + gap;
		int ry = gy + slot;
		graphics.blit(CRAFTING_TEX, rx - 5, ry - 5, 122, 31, 26, 26);
		graphics.renderItem(current.result, rx, ry);
		Component label = Component.translatable(current.resultKey);
		int labelX = rx + 8 - this.font.width(label) / 2;
		graphics.drawString(this.font, label, labelX, ry + 20, HEADER, false);
	}

	private List<BlueprintPage> blueprintPages() {
		List<BlueprintPage> pages = new ArrayList<>();
		if (blueprintUnlocked) {
			pages.add(new BlueprintPage(
					"hostprotocol.pda.blueprint.caption.scanner",
					SCANNER_GRID,
					new ItemStack(ModItems.SCANNER),
					"item.hostprotocol.scanner"));
		}
		if (labBlueprintUnlocked) {
			pages.add(new BlueprintPage(
					"hostprotocol.pda.blueprint.caption.lab",
					LAB_GRID,
					new ItemStack(ModItems.LAB_TABLE),
					"block.hostprotocol.lab_table"));
			pages.add(new BlueprintPage(
					"hostprotocol.pda.blueprint.caption.pda_mk2",
					PDA_MK2_GRID,
					new ItemStack(ModItems.PDA_MK2),
					"item.hostprotocol.pda_mk2"));
		}
		if (baseUnlocked) {
			pages.add(new BlueprintPage(
					"hostprotocol.pda.blueprint.caption.serum",
					SERUM_GRID,
					new ItemStack(ModItems.INFECTION_SERUM),
					"item.hostprotocol.infection_serum"));
			pages.add(new BlueprintPage(
					"hostprotocol.pda.blueprint.caption.suit",
					SUIT_GRID,
					new ItemStack(ModItems.PROTECTIVE_CHESTPLATE),
					"item.hostprotocol.protective_chestplate"));
		}
		return pages;
	}

	private int renderTab(GuiGraphics graphics, int tx, int ty, Component label, boolean selected, int mouseX, int mouseY) {
		int tw = tabWidth(label);
		boolean hover = mouseX >= tx && mouseX < tx + tw && mouseY >= ty && mouseY < ty + TAB_H;
		int bg = selected ? 0xFF2A1238 : hover ? 0xFF1A0E24 : 0xFF100A14;
		int fg = selected ? (mk2Activated ? ACCENT_MK2 : GlitchRenderer.PURPLE) : MUTED;
		graphics.fill(tx, ty, tx + tw, ty + TAB_H, bg);
		if (selected) {
			graphics.fill(tx, ty + TAB_H - 1, tx + tw, ty + TAB_H, accent());
		}
		graphics.drawString(this.font, label, tx + 5, ty + 3, fg, false);
		return tx + tw;
	}

	private int tabWidth(Component label) {
		return this.font.width(label) + 10;
	}

	private int pageCount() {
		if (section == Section.BLUEPRINT) {
			return Math.max(1, blueprintPages().size());
		}
		return currentKeys().length;
	}

	private String[] currentKeys() {
		return switch (section) {
			case DAY1 -> DAY1_KEYS;
			case DAY2 -> DAY2_KEYS;
			case DAY3 -> DAY3_KEYS;
			case SYSTEM -> SYSTEM_KEYS;
			case BLUEPRINT -> new String[] {"hostprotocol.pda.blueprint.page1"};
			case MODULE -> MODULE_KEYS;
			case HEALTH -> HEALTH_KEYS;
			case SCANS -> SCANS_KEYS;
			case ASSISTANT -> ASSISTANT_KEYS;
		};
	}

	private List<FormattedCharSequence> wrapPage(int width) {
		if (section == Section.BLUEPRINT) {
			return List.of();
		}
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
		if (section == Section.MODULE && key.endsWith("page2")) {
			String log = sensorLogs.isEmpty()
					? Component.translatable("hostprotocol.pda.mk2.sensor.empty").getString()
					: String.join("\n", sensorLogs);
			return Component.translatable(key, subjectId, log).getString();
		}
		if (section == Section.HEALTH) {
			Player player = Minecraft.getInstance().player;
			float hp = player == null ? 0 : player.getHealth();
			float max = player == null ? 20 : player.getMaxHealth();
			int hunger = player == null ? 0 : player.getFoodData().getFoodLevel();
			int stage = ProtocolClientState.infectionStage();
			String stageLabel = Component.translatable("hostprotocol.infection.stage." + Math.min(2, stage)).getString();
			return Component.translatable(key, subjectId,
					String.format("%.1f / %.1f", hp, max),
					Integer.toString(hunger),
					stageLabel).getString();
		}
		if (section == Section.SCANS) {
			List<String> scans = ProtocolClientState.recentScans();
			if (scans.isEmpty() && !sensorLogs.isEmpty()) {
				scans = sensorLogs;
			}
			StringBuilder body = new StringBuilder();
			if (scans.isEmpty()) {
				body.append(Component.translatable("hostprotocol.pda.scans.empty").getString());
			} else {
				int n = 1;
				for (String scan : scans) {
					body.append(n++).append(". ").append(scan).append('\n');
				}
			}
			return Component.translatable(key, subjectId, body.toString().trim()).getString();
		}
		return Component.translatable(key, subjectId).getString();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int panelW = Math.min(360, this.width - 32);
		int panelH = Math.min(248, this.height - 32);
		int x = (this.width - panelW) / 2;
		int y = (this.height - panelH) / 2;
		int tabY = y + 32;
		if (renderTabs(null, x, tabY, panelW, (int) mouseX, (int) mouseY, true) < 0) {
			return true;
		}
		advance();
		return true;
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
		if (keyCode == 262 || keyCode == 32 || keyCode == 257) {
			advance();
			return true;
		}
		if (keyCode == 263) {
			if (page > 0) {
				page--;
			} else {
				Section prev = previousSection();
				if (prev != null) {
					selectSection(prev);
					page = pageCount() - 1;
				}
			}
			return true;
		}
		if (keyCode == 265 || keyCode == 87) {
			Section prev = previousSection();
			if (prev != null) {
				selectSection(prev);
			}
			return true;
		}
		if (keyCode == 264 || keyCode == 83) {
			Section next = nextSection();
			if (next != null) {
				selectSection(next);
			}
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private Section previousSection() {
		List<Section> tabs = visibleTabs();
		int i = tabs.indexOf(section);
		if (i > 0) {
			return tabs.get(i - 1);
		}
		return null;
	}

	private Section nextSection() {
		List<Section> tabs = visibleTabs();
		int i = tabs.indexOf(section);
		if (i >= 0 && i + 1 < tabs.size()) {
			return tabs.get(i + 1);
		}
		return null;
	}

	private void advance() {
		if (page + 1 < pageCount()) {
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

	private record BlueprintPage(String captionKey, ItemStack[][] grid, ItemStack result, String resultKey) {
	}
}
