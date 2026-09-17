package ru.hostprotocol.progress;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.hostprotocol.item.ModItems;
import ru.hostprotocol.item.PdaMk2Item;

public final class PdaTransfer {
	private PdaTransfer() {}

	public static ItemStack craft(CraftingContainer grid) {
		ItemStack oldPda = ItemStack.EMPTY;
		ItemStack mk2 = ItemStack.EMPTY;
		int extras = 0;
		for (int i = 0; i < grid.getContainerSize(); i++) {
			ItemStack stack = grid.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (stack.is(ModItems.PDA) && oldPda.isEmpty()) {
				oldPda = stack;
			} else if (stack.is(ModItems.PDA_MK2) && !PdaMk2Item.isActivated(stack) && mk2.isEmpty()) {
				mk2 = stack;
			} else {
				extras++;
			}
		}
		if (PdaTransferLogic.matches(!oldPda.isEmpty(), !mk2.isEmpty(), extras)) {
			return PdaMk2Item.createActivatedFrom(oldPda);
		}
		return ItemStack.EMPTY;
	}

	public static boolean isTransferResult(ItemStack stack) {
		return !stack.isEmpty() && stack.is(ModItems.PDA_MK2) && PdaMk2Item.isActivated(stack);
	}

	public static void consumeGrid(CraftingContainer grid, Player player) {
		for (int i = 0; i < grid.getContainerSize(); i++) {
			ItemStack stack = grid.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			Item remainderItem = stack.getItem().hasCraftingRemainingItem() ? stack.getItem().getCraftingRemainingItem() : null;
			grid.removeItem(i, 1);
			if (remainderItem != null && player != null) {
				ItemStack remainder = new ItemStack(remainderItem);
				if (!player.getInventory().add(remainder)) {
					player.drop(remainder, false);
				}
			}
		}
	}
}
