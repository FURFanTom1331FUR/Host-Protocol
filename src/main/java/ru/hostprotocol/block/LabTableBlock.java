package ru.hostprotocol.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import ru.hostprotocol.menu.LabTableMenu;

/**
 * Laboratory table: 5×5 crafting grid (larger than a vanilla 3×3 table).
 */
public class LabTableBlock extends Block {
	private static final Component TITLE = Component.translatable("hostprotocol.lab_table.title");

	public LabTableBlock(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		player.openMenu(state.getMenuProvider(level, pos));
		return InteractionResult.CONSUME;
	}

	@Override
	public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return new SimpleMenuProvider(
				(id, inventory, player) -> new LabTableMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
				TITLE);
	}
}
