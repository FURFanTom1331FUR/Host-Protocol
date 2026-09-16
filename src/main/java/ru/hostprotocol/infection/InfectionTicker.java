package ru.hostprotocol.infection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.block.ModBlocks;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.freeze.IntroFreeze;
import ru.hostprotocol.item.PdaService;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.sound.ModSounds;
import ru.hostprotocol.world.ProtocolTime;

/**
 * Day-2-night infection focus: persistent coordinates, forced chunks, slow radius growth.
 */
public final class InfectionTicker {
	/** Max conversion radius (blocks) for this MVP. */
	public static final int MAX_RADIUS = 16;
	/** Day-time ticks per +1 radius step. */
	public static final int TICKS_PER_RADIUS = 500;
	private static final int CHUNK_RADIUS = 2;
	private static final int FOCUS_DISTANCE_MIN = 40;
	private static final int FOCUS_DISTANCE_SPAN = 24;
	private static final int SPREAD_ATTEMPTS = 4;
	private static final int CATCHUP_ATTEMPTS = 28;

	private InfectionTicker() {}

	public static void tick(MinecraftServer server) {
		if (IntroFreeze.isActive()) {
			return;
		}
		ServerLevel level = server.overworld();
		IntroWorldData data = IntroWorldData.get(level);
		if (!data.isIntroCompleted()) {
			return;
		}

		long dayTime = level.getDayTime();
		if (!data.isInfectionActive() && ProtocolTime.isDay2Night(dayTime)) {
			activate(level, data, dayTime);
		}

		if (!data.isInfectionActive() || !data.hasInfectionFocus()) {
			return;
		}

		keepFocusLoaded(level, data);
		spread(level, data, dayTime);
		maybeUnlockCoords(server, data, dayTime);
	}

	public static void activateIfNeededForSleep(ServerLevel level, IntroWorldData data) {
		if (data.isInfectionActive()) {
			return;
		}
		if (IntroFreeze.isActive() || !data.isIntroCompleted()) {
			return;
		}
		long dayTime = level.getDayTime();
		if (ProtocolTime.dayIndex(dayTime) != 2) {
			return;
		}
		activate(level, data, dayTime);
	}

	private static void activate(ServerLevel level, IntroWorldData data, long dayTime) {
		BlockPos focus = pickFocus(level);
		data.activateInfection(focus, dayTime);
		keepFocusLoaded(level, data);
		infectPos(level, focus, true);
		for (BlockPos near : BlockPos.betweenClosed(focus.offset(-1, -1, -1), focus.offset(1, 1, 1))) {
			infectPos(level, near.immutable(), false);
		}
		HostProtocolMod.LOGGER.info("Infection focus activated at {} (dayTime={})", focus, dayTime);
	}

	private static BlockPos pickFocus(ServerLevel level) {
		BlockPos spawn = level.getSharedSpawnPos();
		RandomSource random = RandomSource.create(level.getSeed() ^ 0x48504E464F43L);
		double angle = random.nextDouble() * Math.PI * 2.0;
		int dist = FOCUS_DISTANCE_MIN + random.nextInt(FOCUS_DISTANCE_SPAN);
		int x = spawn.getX() + (int) Math.round(Math.cos(angle) * dist);
		int z = spawn.getZ() + (int) Math.round(Math.sin(angle) * dist);
		level.getChunk(x >> 4, z >> 4);
		BlockPos approx = surfacePos(level, x, z);
		BlockPos infectable = findInfectable(level, approx, 12);
		return infectable != null ? infectable : approx;
	}

	private static BlockPos surfacePos(ServerLevel level, int x, int z) {
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
		int min = level.getMinBuildHeight() + 1;
		if (y < min) {
			y = Math.max(min, level.getSharedSpawnPos().getY());
		}
		for (int dy = 0; dy < 24; dy++) {
			BlockPos pos = new BlockPos(x, y - dy, z);
			if (pos.getY() < min) {
				break;
			}
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || !state.getFluidState().isEmpty()) {
				continue;
			}
			return pos;
		}
		return new BlockPos(x, y, z);
	}

	private static BlockPos findInfectable(ServerLevel level, BlockPos origin, int radius) {
		if (InfectionMapping.canInfect(level.getBlockState(origin))) {
			return origin;
		}
		for (int r = 1; r <= radius; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.abs(dx) != r && Math.abs(dz) != r) {
						continue;
					}
					for (int dy = -4; dy <= 4; dy++) {
						BlockPos pos = origin.offset(dx, dy, dz);
						if (InfectionMapping.canInfect(level.getBlockState(pos))) {
							return pos;
						}
					}
				}
			}
		}
		return null;
	}

	public static void keepFocusLoaded(ServerLevel level, IntroWorldData data) {
		if (!data.hasInfectionFocus()) {
			return;
		}
		if (data.isForcedChunksArmed() && level.getGameTime() % 200L != 0L) {
			return;
		}
		int cx = data.getFocusX() >> 4;
		int cz = data.getFocusZ() >> 4;
		for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
			for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
				level.setChunkForced(cx + dx, cz + dz, true);
			}
		}
		data.markForcedChunksArmed();
	}

	private static void spread(ServerLevel level, IntroWorldData data, long dayTime) {
		long elapsed = Math.max(0L, dayTime - data.getInfectionStartDayTime());
		int radius = currentRadius(elapsed);
		int attempts = elapsed >= ProtocolTime.COORDS_LOCK_DELAY ? CATCHUP_ATTEMPTS : SPREAD_ATTEMPTS;
		if (elapsed < 40L) {
			attempts = 16;
		}
		RandomSource random = level.random;
		BlockPos focus = data.getFocusPos();
		for (int i = 0; i < attempts; i++) {
			int dx = random.nextInt(radius * 2 + 1) - radius;
			int dz = random.nextInt(radius * 2 + 1) - radius;
			if (dx * dx + dz * dz > radius * radius) {
				continue;
			}
			int dy = random.nextInt(9) - 4;
			infectPos(level, focus.offset(dx, dy, dz), random.nextInt(12) == 0);
		}
		if (level.getGameTime() % 80L == 0L) {
			level.sendParticles(ParticleTypes.REVERSE_PORTAL,
					focus.getX() + 0.5, focus.getY() + 1.2, focus.getZ() + 0.5,
					6, 0.35, 0.4, 0.35, 0.02);
		}
	}

	public static int currentRadius(long elapsedDayTime) {
		return (int) Math.min(MAX_RADIUS, 1L + elapsedDayTime / TICKS_PER_RADIUS);
	}

	private static boolean infectPos(ServerLevel level, BlockPos pos, boolean fx) {
		if (!level.isLoaded(pos)) {
			return false;
		}
		BlockState current = level.getBlockState(pos);
		if (ModBlocks.isInfected(current.getBlock())) {
			return false;
		}
		if (!current.getFluidState().isEmpty()) {
			return false;
		}
		BlockState infected = InfectionMapping.toInfected(current);
		if (infected == null) {
			return false;
		}
		level.setBlock(pos, infected, 3);
		if (fx) {
			level.sendParticles(ParticleTypes.PORTAL,
					pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
					8, 0.25, 0.3, 0.25, 0.15);
			if (level.random.nextInt(5) == 0) {
				level.playSound(null, pos, ModSounds.VOICE_GLITCH_HIT, SoundSource.BLOCKS, 0.18F, 0.45F + level.random.nextFloat() * 0.2F);
			}
		}
		return true;
	}

	private static void maybeUnlockCoords(MinecraftServer server, IntroWorldData data, long dayTime) {
		if (data.isCoordsDiscovered()) {
			stampOnlinePlayers(server, data);
			return;
		}
		long elapsed = dayTime - data.getInfectionStartDayTime();
		int day = ProtocolTime.dayIndex(dayTime);
		if (elapsed < ProtocolTime.COORDS_LOCK_DELAY && day < 3) {
			return;
		}
		if (!data.markCoordsDiscovered()) {
			return;
		}
		Component notice = Component.translatable(
				"hostprotocol.infection.coords.notice",
				data.getFocusX(), data.getFocusY(), data.getFocusZ()
		);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.sendSystemMessage(notice);
			PdaService.stampDay2Coords(player, data);
			ModNetworking.sendProtocolState(player, data);
		}
		HostProtocolMod.LOGGER.info("Infection coordinates locked: {} {} {}",
				data.getFocusX(), data.getFocusY(), data.getFocusZ());
	}

	private static void stampOnlinePlayers(MinecraftServer server, IntroWorldData data) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (data.hasDay2CoordsLog(player.getUUID())) {
				continue;
			}
			PdaService.stampDay2Coords(player, data);
			ModNetworking.sendProtocolState(player, data);
		}
	}
}
