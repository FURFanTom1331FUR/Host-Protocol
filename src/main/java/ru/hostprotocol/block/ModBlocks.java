package ru.hostprotocol.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import ru.hostprotocol.HostProtocolMod;

public final class ModBlocks {
	public static final Block INFECTED_STONE = infected(SoundType.STONE, MapColor.COLOR_PURPLE);
	public static final Block INFECTED_COBBLESTONE = infected(SoundType.STONE, MapColor.COLOR_PURPLE);
	public static final Block INFECTED_DIRT = infected(SoundType.GRAVEL, MapColor.COLOR_PURPLE);
	public static final Block INFECTED_GRASS_BLOCK = infected(SoundType.GRASS, MapColor.COLOR_PURPLE);
	public static final Block INFECTED_OAK_LOG = new InfectedPillarBlock(unbreakable(SoundType.WOOD, MapColor.COLOR_PURPLE));
	public static final Block INFECTED_OAK_PLANKS = infected(SoundType.WOOD, MapColor.COLOR_PURPLE);

	private ModBlocks() {}

	public static void register() {
		register("infected_stone", INFECTED_STONE);
		register("infected_cobblestone", INFECTED_COBBLESTONE);
		register("infected_dirt", INFECTED_DIRT);
		register("infected_grass_block", INFECTED_GRASS_BLOCK);
		register("infected_oak_log", INFECTED_OAK_LOG);
		register("infected_oak_planks", INFECTED_OAK_PLANKS);
	}

	public static boolean isInfected(Block block) {
		return block instanceof InfectedBlock || block instanceof InfectedPillarBlock;
	}

	private static InfectedBlock infected(SoundType sound, MapColor color) {
		return new InfectedBlock(unbreakable(sound, color));
	}

	private static BlockBehaviour.Properties unbreakable(SoundType sound, MapColor color) {
		return BlockBehaviour.Properties.of()
				.mapColor(color)
				.strength(-1.0F, 3600000.0F)
				.sound(sound)
				.noLootTable()
				.pushReaction(PushReaction.BLOCK);
	}

	private static void register(String path, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, HostProtocolMod.id(path), block);
	}
}
