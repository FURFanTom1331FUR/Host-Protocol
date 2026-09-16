package ru.hostprotocol.infection;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import ru.hostprotocol.block.ModBlocks;

/**
 * Starter infection set: stone, cobblestone, dirt, grass, oak log/planks.
 * Water / biomes / cure are out of scope.
 */
public final class InfectionMapping {
	private InfectionMapping() {}

	public static boolean canInfect(BlockState state) {
		return toInfected(state) != null;
	}

	public static BlockState toInfected(BlockState original) {
		Block block = original.getBlock();
		if (block == Blocks.STONE) {
			return ModBlocks.INFECTED_STONE.defaultBlockState();
		}
		if (block == Blocks.COBBLESTONE) {
			return ModBlocks.INFECTED_COBBLESTONE.defaultBlockState();
		}
		if (block == Blocks.DIRT || block == Blocks.COARSE_DIRT) {
			return ModBlocks.INFECTED_DIRT.defaultBlockState();
		}
		if (block == Blocks.GRASS_BLOCK) {
			return ModBlocks.INFECTED_GRASS_BLOCK.defaultBlockState();
		}
		if (block == Blocks.OAK_LOG || block == Blocks.OAK_WOOD) {
			BlockState infected = ModBlocks.INFECTED_OAK_LOG.defaultBlockState();
			if (original.hasProperty(RotatedPillarBlock.AXIS) && infected.hasProperty(RotatedPillarBlock.AXIS)) {
				return infected.setValue(RotatedPillarBlock.AXIS, original.getValue(RotatedPillarBlock.AXIS));
			}
			return infected;
		}
		if (block == Blocks.OAK_PLANKS) {
			return ModBlocks.INFECTED_OAK_PLANKS.defaultBlockState();
		}
		return null;
	}
}
