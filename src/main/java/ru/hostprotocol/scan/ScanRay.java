package ru.hostprotocol.scan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.hostprotocol.block.ModBlockTags;

/**
 * Shared client/server raycast: living entities first if closer, otherwise scannable ore.
 */
public final class ScanRay {
	private ScanRay() {}

	public static ScanHit pick(Player player) {
		return pick(player, 1.0F);
	}

	public static ScanHit pick(Player player, float partialTick) {
		Level level = player.level();
		Vec3 start = player.getEyePosition(partialTick);
		Vec3 look = player.getViewVector(partialTick);
		Vec3 end = start.add(look.scale(ScanTiming.RANGE));

		BlockHitResult blockHit = level.clip(new ClipContext(
				start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

		AABB box = player.getBoundingBox().expandTowards(look.scale(ScanTiming.RANGE)).inflate(1.0);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
				player,
				start,
				end,
				box,
				entity -> entity instanceof LivingEntity living
						&& living.isAlive()
						&& !entity.isSpectator()
						&& entity != player,
				ScanTiming.RANGE * ScanTiming.RANGE);

		double blockDist = blockHit.getType() == HitResult.Type.MISS
				? Double.MAX_VALUE
				: blockHit.getLocation().distanceToSqr(start);
		double entityDist = entityHit == null
				? Double.MAX_VALUE
				: entityHit.getLocation().distanceToSqr(start);

		if (entityHit != null && entityDist <= blockDist) {
			return ScanHit.mob((LivingEntity) entityHit.getEntity(), entityHit.getLocation());
		}
		if (blockHit.getType() == HitResult.Type.BLOCK) {
			BlockPos pos = blockHit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (state.is(ModBlockTags.SCANNABLE_ORES)) {
				return ScanHit.ore(pos, state.getBlock(), blockHit.getLocation());
			}
			return ScanHit.none(blockHit.getLocation());
		}
		return ScanHit.none(end);
	}

	public static Vec3 beamEnd(Player player, float partialTick) {
		ScanHit hit = pick(player, partialTick);
		if (hit.location != null) {
			return hit.location;
		}
		Vec3 start = player.getEyePosition(partialTick);
		return start.add(player.getViewVector(partialTick).scale(ScanTiming.RANGE));
	}
}
