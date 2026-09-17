package ru.hostprotocol.scan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public final class ScanHit {
	public enum Kind {
		NONE, ORE, MOB
	}

	public final Kind kind;
	public final BlockPos blockPos;
	public final Block block;
	public final int entityId;
	public final LivingEntity entity;
	public final Vec3 location;

	private ScanHit(Kind kind, BlockPos blockPos, Block block, LivingEntity entity, Vec3 location) {
		this.kind = kind;
		this.blockPos = blockPos;
		this.block = block;
		this.entity = entity;
		this.entityId = entity == null ? -1 : entity.getId();
		this.location = location;
	}

	public static ScanHit none(Vec3 location) {
		return new ScanHit(Kind.NONE, null, null, null, location);
	}

	public static ScanHit ore(BlockPos pos, Block block, Vec3 location) {
		return new ScanHit(Kind.ORE, pos.immutable(), block, null, location);
	}

	public static ScanHit mob(LivingEntity entity, Vec3 location) {
		return new ScanHit(Kind.MOB, entity.blockPosition(), null, entity, location);
	}

	public boolean isEmpty() {
		return kind == Kind.NONE;
	}

	public boolean isOre() {
		return kind == Kind.ORE;
	}

	public boolean isMob() {
		return kind == Kind.MOB;
	}

	public boolean sameTarget(ScanHit other) {
		if (other == null || kind != other.kind) {
			return false;
		}
		return switch (kind) {
			case NONE -> false;
			case ORE -> blockPos != null && blockPos.equals(other.blockPos);
			case MOB -> entityId >= 0 && entityId == other.entityId;
		};
	}
}
