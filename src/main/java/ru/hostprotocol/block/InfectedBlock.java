package ru.hostprotocol.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Infected world matter: original block fully covered by the overlay layer, unbreakable.
 */
public class InfectedBlock extends Block {
	public InfectedBlock(Properties properties) {
		super(properties);
	}

	@Override
	public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return 0.0F;
	}

	@Override
	public boolean dropFromExplosion(Explosion explosion) {
		return false;
	}
}
