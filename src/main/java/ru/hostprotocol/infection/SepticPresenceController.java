package ru.hostprotocol.infection;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.entity.ModEntityTypes;
import ru.hostprotocol.entity.SepticEntity;
import ru.hostprotocol.freeze.IntroFreeze;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.progress.SepticPresenceLogic;
import ru.hostprotocol.world.ProtocolTime;

import java.util.List;
import java.util.UUID;

/**
 * From day 5, keep a single Septic presence in the overworld. No boss AI.
 */
public final class SepticPresenceController {
	private SepticPresenceController() {}

	public static void tick(MinecraftServer server) {
		if (IntroFreeze.isActive()) {
			return;
		}
		ServerLevel level = server.overworld();
		IntroWorldData data = IntroWorldData.get(level);
		if (!data.isIntroCompleted()) {
			return;
		}
		int day = ProtocolTime.dayIndex(level.getDayTime());
		if (day < SepticPresenceLogic.FIRST_DAY) {
			return;
		}
		if (level.getGameTime() % 40L != 0L) {
			return;
		}
		ensure(level, data);
	}

	public static SepticEntity spawnNow(ServerLevel level, IntroWorldData data, BlockPos near) {
		removeTracked(level, data);
		BlockPos at = findSpawn(level, near);
		SepticEntity septic = ModEntityTypes.SEPTIC.create(level);
		if (septic == null) {
			return null;
		}
		septic.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(septic);
		data.setSepticEntityId(septic.getUUID());
		data.markSepticSpawned();
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			ModNetworking.sendProtocolState(player, data);
		}
		HostProtocolMod.LOGGER.info("Septic presence spawned at {}", at);
		return septic;
	}

	public static boolean isPresent(ServerLevel level, IntroWorldData data) {
		return findAlive(level, data) != null;
	}

	private static void ensure(ServerLevel level, IntroWorldData data) {
		if (findAlive(level, data) != null) {
			data.markSepticSpawned();
			return;
		}
		ServerPlayer lead = null;
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			if (player.serverLevel() == level) {
				lead = player;
				break;
			}
		}
		if (lead == null) {
			return;
		}
		spawnNow(level, data, lead.blockPosition());
	}

	private static SepticEntity findAlive(ServerLevel level, IntroWorldData data) {
		UUID id = data.getSepticEntityId();
		if (id != null) {
			Entity entity = level.getEntity(id);
			if (entity instanceof SepticEntity septic && septic.isAlive()) {
				return septic;
			}
		}
		for (ServerPlayer player : level.players()) {
			AABB box = player.getBoundingBox().inflate(96.0);
			List<SepticEntity> found = level.getEntitiesOfClass(SepticEntity.class, box, Living -> Living.isAlive());
			if (!found.isEmpty()) {
				SepticEntity septic = found.get(0);
				data.setSepticEntityId(septic.getUUID());
				return septic;
			}
		}
		if (data.hasInfectionFocus()) {
			AABB box = new AABB(data.getFocusPos()).inflate(96.0);
			List<SepticEntity> found = level.getEntitiesOfClass(SepticEntity.class, box, e -> e.isAlive());
			if (!found.isEmpty()) {
				SepticEntity septic = found.get(0);
				data.setSepticEntityId(septic.getUUID());
				return septic;
			}
		}
		return null;
	}

	private static void removeTracked(ServerLevel level, IntroWorldData data) {
		SepticEntity existing = findAlive(level, data);
		if (existing != null) {
			existing.discard();
		}
		data.clearSepticEntityId();
	}

	private static BlockPos findSpawn(ServerLevel level, BlockPos near) {
		int dist = 16 + level.random.nextInt(12);
		double angle = level.random.nextDouble() * Math.PI * 2.0;
		int x = near.getX() + (int) Math.round(Math.cos(angle) * dist);
		int z = near.getZ() + (int) Math.round(Math.sin(angle) * dist);
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		return new BlockPos(x, y, z);
	}
}
