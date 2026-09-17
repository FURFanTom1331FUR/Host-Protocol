package ru.hostprotocol.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import ru.hostprotocol.HostProtocolMod;

public final class ModBlockTags {
	public static final TagKey<Block> INFECTED = TagKey.create(Registries.BLOCK, HostProtocolMod.id("infected"));
	public static final TagKey<Block> SCANNABLE_ORES = TagKey.create(Registries.BLOCK, HostProtocolMod.id("scannable_ores"));

	private ModBlockTags() {}
}
