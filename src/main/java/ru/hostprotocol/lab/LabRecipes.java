package ru.hostprotocol.lab;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.item.ModItems;
import ru.hostprotocol.item.PdaItem;
import ru.hostprotocol.item.PdaService;

/**
 * Recipes that only match on the laboratory table (5×5). The MK-II PDA pattern is 3×3
 * and may sit in any sub-rectangle; leftover occupied cells reject the craft.
 *
 * <pre>
 * I G I
 * I R I
 * I G I
 * I = iron ingot, G = glass, R = redstone dust
 * </pre>
 */
public final class LabRecipes {
	public static final String[][] PDA_MK2_PATTERN = {
			{"minecraft:iron_ingot", "minecraft:glass", "minecraft:iron_ingot"},
			{"minecraft:iron_ingot", "minecraft:redstone", "minecraft:iron_ingot"},
			{"minecraft:iron_ingot", "minecraft:glass", "minecraft:iron_ingot"}
	};

	private LabRecipes() {}

	public static ItemStack match(CraftingContainer container) {
		int width = container.getWidth();
		int height = container.getHeight();
		String[][] grid = new String[height][width];
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				grid[row][col] = idOf(container.getItem(col + row * width));
			}
		}
		if (ShapedGridMatcher.matches(grid, PDA_MK2_PATTERN)) {
			return new ItemStack(ModItems.PDA_MK2);
		}
		return ru.hostprotocol.progress.PdaTransfer.craft(container);
	}

	public static void stampCrafted(Player player, ItemStack stack) {
		if (stack.isEmpty() || player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		if (!stack.is(ModItems.PDA_MK2)) {
			return;
		}
		IntroWorldData data = IntroWorldData.get(serverPlayer.serverLevel().getServer().overworld());
		if (stack.getOrCreateTag().getString(PdaItem.TAG_SUBJECT).isEmpty()) {
			stack.getOrCreateTag().putString(PdaItem.TAG_SUBJECT, data.getSubjectId());
		}
		PdaService.stampOnto(stack, data, serverPlayer.getUUID());
		if (ru.hostprotocol.progress.PdaTransfer.isTransferResult(stack)) {
			ru.hostprotocol.progress.ProgressionService.onTransferComplete(serverPlayer, stack);
		}
	}

	private static String idOf(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "";
		}
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}
}
