package ru.hostprotocol.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import ru.hostprotocol.infection.InfectionVisuals;

/**
 * Infected pillar (logs): keeps AXIS, overlay covers every face, unbreakable.
 */
public class InfectedPillarBlock extends RotatedPillarBlock {
	public InfectedPillarBlock(Properties properties) {
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

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		InfectionVisuals.animate(level, pos, random);
	}
}
