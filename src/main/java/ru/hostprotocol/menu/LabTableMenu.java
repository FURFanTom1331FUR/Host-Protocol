package ru.hostprotocol.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import ru.hostprotocol.block.ModBlocks;
import ru.hostprotocol.lab.LabRecipes;

/**
 * Laboratory table screen handler: 5×5 craft grid + result + player inventory.
 * Slot map: 0 result, 1–25 grid, 26–52 main inventory, 53–61 hotbar.
 */
public class LabTableMenu extends AbstractContainerMenu {
	public static final int GRID = 5;
	public static final int GRID_SLOTS = GRID * GRID;
	public static final int RESULT_SLOT = 0;
	public static final int GRID_START = 1;
	public static final int GRID_END = GRID_START + GRID_SLOTS;
	public static final int INV_START = GRID_END;
	public static final int INV_END = INV_START + 27;
	public static final int HOTBAR_END = INV_END + 9;

	public static final int GRID_X = 16;
	public static final int GRID_Y = 18;
	public static final int RESULT_X = 142;
	public static final int RESULT_Y = 54;
	public static final int PLAYER_INV_Y = 120;
	public static final int HOTBAR_Y = 178;
	public static final int IMAGE_WIDTH = 176;
	public static final int IMAGE_HEIGHT = 202;

	private final CraftingContainer craftSlots;
	private final ResultContainer resultSlots;
	private final ContainerLevelAccess access;
	private final Player player;

	public LabTableMenu(int id, Inventory inventory) {
		this(id, inventory, ContainerLevelAccess.NULL);
	}

	public LabTableMenu(int id, Inventory inventory, ContainerLevelAccess access) {
		super(ModMenus.LAB_TABLE, id);
		this.access = access;
		this.player = inventory.player;
		this.craftSlots = new TransientCraftingContainer(this, GRID, GRID);
		this.resultSlots = new ResultContainer();

		this.addSlot(new LabResultSlot(inventory.player, this.craftSlots, this.resultSlots, RESULT_SLOT, RESULT_X, RESULT_Y));
		for (int row = 0; row < GRID; row++) {
			for (int col = 0; col < GRID; col++) {
				this.addSlot(new Slot(this.craftSlots, col + row * GRID, GRID_X + col * 18, GRID_Y + row * 18));
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(inventory, col, 8 + col * 18, HOTBAR_Y));
		}
	}

	@Override
	public void slotsChanged(net.minecraft.world.Container container) {
		this.resultSlots.setItem(0, LabRecipes.match(this.craftSlots));
		this.broadcastChanges();
		super.slotsChanged(container);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.access.execute((level, pos) -> this.clearContainer(player, this.craftSlots));
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, ModBlocks.LAB_TABLE);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack copy = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		copy = stack.copy();
		if (index == RESULT_SLOT) {
			if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickCraft(stack, copy);
		} else if (index >= INV_START && index < HOTBAR_END) {
			if (!this.moveItemStackTo(stack, GRID_START, GRID_END, false)) {
				if (index < INV_END) {
					if (!this.moveItemStackTo(stack, INV_END, HOTBAR_END, false)) {
						return ItemStack.EMPTY;
					}
				} else if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
					return ItemStack.EMPTY;
				}
			}
		} else if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == copy.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		if (index == RESULT_SLOT) {
			player.drop(stack, false);
		}
		return copy;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
	}

	private static final class LabResultSlot extends Slot {
		private final Player player;
		private final CraftingContainer craftSlots;

		LabResultSlot(Player player, CraftingContainer craftSlots, ResultContainer result, int slot, int x, int y) {
			super(result, slot, x, y);
			this.player = player;
			this.craftSlots = craftSlots;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public void onTake(Player player, ItemStack stack) {
			LabRecipes.stampCrafted(player, stack);
			for (int i = 0; i < this.craftSlots.getContainerSize(); i++) {
				ItemStack in = this.craftSlots.getItem(i);
				if (!in.isEmpty()) {
					this.craftSlots.removeItem(i, 1);
				}
			}
			super.onTake(player, stack);
		}

		@Override
		protected void onQuickCraft(ItemStack stack, int amount) {
			super.onQuickCraft(stack, amount);
		}
	}
}
