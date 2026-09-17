package ru.hostprotocol.horror;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.entity.ModEntityTypes;
import ru.hostprotocol.entity.SepticEntity;
import ru.hostprotocol.freeze.IntroFreeze;
import ru.hostprotocol.infection.Day2DebugSequence;
import ru.hostprotocol.infection.Day3DisconnectController;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.world.ProtocolTime;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day-gated scare beats. Never kicks, never deletes worlds. Demo commands bypass one-shots.
 */
public final class HorrorEventScheduler {
	private static final Map<UUID, Integer> DEMO_SCREAMER_IN = new ConcurrentHashMap<>();

	private HorrorEventScheduler() {}

	public static void tick(MinecraftServer server) {
		if (IntroFreeze.isActive() || Day2DebugSequence.isRunning()) {
			return;
		}
		ServerLevel level = server.overworld();
		IntroWorldData data = IntroWorldData.get(level);
		if (!data.isIntroCompleted()) {
			return;
		}
		int day = ProtocolTime.dayIndex(level.getDayTime());
		long gameTime = level.getGameTime();
		tickDemo(server);
		if (gameTime % 20L != 0L) {
			return;
		}
		if (day < HorrorEventLogic.FAKE_JOIN_MIN_DAY) {
			return;
		}
		boolean night = ProtocolTime.isNight(level.getDayTime());
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.serverLevel() != level) {
				continue;
			}
			tickPlayer(player, data, day, gameTime, night);
		}
	}

	public static int fireScreamer(ServerPlayer player, boolean secondBeat) {
		return fireScreamer(player, secondBeat, true);
	}

	public static int fireScreamer(ServerPlayer player, boolean secondBeat, boolean persistCooldown) {
		ModNetworking.sendHorrorEvent(player, HorrorKind.SCREAMER, secondBeat ? 1 : 0);
		if (persistCooldown) {
			IntroWorldData data = IntroWorldData.get(player.serverLevel().getServer().overworld());
			data.setHorrorTime(player.getUUID(), HorrorEventLogic.TIME_LAST_SCREAMER, player.serverLevel().getGameTime());
		}
		HostProtocolMod.LOGGER.info("[Host Protocol] HORROR SCREAMER player={} second={} persist={}",
				player.getGameProfile().getName(), secondBeat, persistCooldown);
		return 1;
	}

	public static int fireStalker(ServerPlayer player) {
		return fireStalker(player, true);
	}

	public static int fireStalker(ServerPlayer player, boolean persistCooldown) {
		SepticEntity septic = spawnStalker(player);
		if (septic == null) {
			return 0;
		}
		ModNetworking.sendHorrorEvent(player, HorrorKind.STALKER);
		if (persistCooldown) {
			IntroWorldData data = IntroWorldData.get(player.serverLevel().getServer().overworld());
			data.setHorrorTime(player.getUUID(), HorrorEventLogic.TIME_LAST_STALKER, player.serverLevel().getGameTime());
		}
		HostProtocolMod.LOGGER.info("[Host Protocol] HORROR STALKER player={} at {} persist={}",
				player.getGameProfile().getName(), septic.blockPosition(), persistCooldown);
		return 1;
	}

	public static int fireDemo(ServerPlayer player) {
		fireStalker(player, false);
		DEMO_SCREAMER_IN.put(player.getUUID(), 35);
		ModNetworking.sendHorrorEvent(player, HorrorKind.FAKE_JOIN);
		HostProtocolMod.LOGGER.info("[Host Protocol] HORROR DEMO player={}", player.getGameProfile().getName());
		return 1;
	}

	public static void onPlayerLeave(ServerPlayer player) {
		DEMO_SCREAMER_IN.remove(player.getUUID());
		discardStalkers(player.serverLevel());
	}

	public static void discardStalkers(ServerLevel level) {
		if (level == null) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			AABB box = player.getBoundingBox().inflate(96.0);
			for (SepticEntity septic : level.getEntitiesOfClass(SepticEntity.class, box, SepticEntity::isHorrorStalker)) {
				septic.discard();
			}
		}
	}

	private static void tickDemo(MinecraftServer server) {
		if (DEMO_SCREAMER_IN.isEmpty()) {
			return;
		}
		for (UUID id : DEMO_SCREAMER_IN.keySet().toArray(new UUID[0])) {
			Integer left = DEMO_SCREAMER_IN.merge(id, -1, Integer::sum);
			if (left == null || left > 0) {
				continue;
			}
			DEMO_SCREAMER_IN.remove(id);
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null) {
				fireScreamer(player, false, false);
			}
		}
	}

	private static void tickPlayer(ServerPlayer player, IntroWorldData data, int day, long gameTime, boolean night) {
		UUID id = player.getUUID();
		boolean busy = Day3DisconnectController.isBusy(id);
		boolean kickDone = data.hasDay3DisconnectFired(id) || data.hasDay3LeftBeforeKick(id);
		boolean freeze = IntroFreeze.isActive();

		if (day >= HorrorEventLogic.STALKER_MIN_DAY && data.getHorrorTime(id, HorrorEventLogic.TIME_DAY4_ENTER) <= 0L) {
			data.setHorrorTime(id, HorrorEventLogic.TIME_DAY4_ENTER, gameTime);
		}

		if (HorrorEventLogic.canSocialGlitch(day, kickDone, freeze, true) && !busy) {
			if (!data.hasHorrorFlag(id, HorrorEventLogic.FLAG_FAKE_JOIN)) {
				data.markHorrorFlag(id, HorrorEventLogic.FLAG_FAKE_JOIN);
				data.setHorrorTime(id, HorrorEventLogic.TIME_SOCIAL, gameTime);
				ModNetworking.sendHorrorEvent(player, HorrorKind.FAKE_JOIN);
				HostProtocolMod.LOGGER.info("[Host Protocol] HORROR FAKE JOIN player={}", player.getGameProfile().getName());
				return;
			}
			if (data.hasHorrorFlag(id, HorrorEventLogic.FLAG_FAKE_JOIN)
					&& !data.hasHorrorFlag(id, HorrorEventLogic.FLAG_FAKE_TOAST)
					&& gameTime - data.getHorrorTime(id, HorrorEventLogic.TIME_SOCIAL) >= HorrorEventLogic.TOAST_DELAY_TICKS) {
				data.markHorrorFlag(id, HorrorEventLogic.FLAG_FAKE_TOAST);
				ModNetworking.sendHorrorEvent(player, HorrorKind.FAKE_TOAST);
				HostProtocolMod.LOGGER.info("[Host Protocol] HORROR FAKE TOAST player={}", player.getGameProfile().getName());
				return;
			}
			if (!data.hasHorrorFlag(id, HorrorEventLogic.FLAG_STATIC)
					&& data.hasHorrorFlag(id, HorrorEventLogic.FLAG_FAKE_TOAST)
					&& player.getRandom().nextInt(24) == 0) {
				data.markHorrorFlag(id, HorrorEventLogic.FLAG_STATIC);
				ModNetworking.sendHorrorEvent(player, HorrorKind.STATIC_FLASH);
				HostProtocolMod.LOGGER.info("[Host Protocol] HORROR STATIC player={}", player.getGameProfile().getName());
				return;
			}
		}

		if (HorrorEventLogic.canStalker(day, true, freeze, busy)) {
			maybeStalker(player, data, gameTime);
		}

		if (day >= HorrorEventLogic.PDA_BETRAY_MIN_DAY && !data.hasHorrorFlag(id, HorrorEventLogic.FLAG_PDA_BETRAY)) {
			data.markHorrorFlag(id, HorrorEventLogic.FLAG_PDA_BETRAY);
			ModNetworking.sendHorrorEvent(player, HorrorKind.PDA_BETRAY);
		}

		if (data.hasInfectionFocus() && HorrorEventLogic.screamerCooldownReady(gameTime,
				data.getHorrorTime(id, HorrorEventLogic.TIME_LAST_SCREAMER))) {
			int dx = player.getBlockX() - data.getFocusX();
			int dy = player.getBlockY() - data.getFocusY();
			int dz = player.getBlockZ() - data.getFocusZ();
			boolean near = HorrorEventLogic.nearFocus(dx, dy, dz, HorrorEventLogic.FOCUS_NIGHT_RANGE);
			if (HorrorEventLogic.shouldFocusNightScreamer(day, night, near,
					data.hasHorrorFlag(id, HorrorEventLogic.FLAG_FOCUS_SCREAMER))) {
				data.markHorrorFlag(id, HorrorEventLogic.FLAG_FOCUS_SCREAMER);
				fireScreamer(player, false);
			}
		}
	}

	private static void maybeStalker(ServerPlayer player, IntroWorldData data, long gameTime) {
		UUID id = player.getUUID();
		long last = data.getHorrorTime(id, HorrorEventLogic.TIME_LAST_STALKER);
		long entered = data.getHorrorTime(id, HorrorEventLogic.TIME_DAY4_ENTER);
		boolean firstReady = HorrorEventLogic.firstStalkerReady(gameTime, entered);
		if (!HorrorEventLogic.stalkerCooldownReady(gameTime, last, last <= 0L && firstReady)) {
			return;
		}
		if (last <= 0L && !firstReady) {
			return;
		}
		if (last > 0L && player.getRandom().nextInt(8) != 0) {
			return;
		}
		fireStalker(player);
	}

	private static SepticEntity spawnStalker(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		SepticEntity septic = ModEntityTypes.SEPTIC.create(level);
		if (septic == null) {
			return null;
		}
		BlockPos pos = stalkerPos(player);
		int life = HorrorEventLogic.stalkerLife(level.getSeed(), level.getGameTime());
		septic.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
		double dx = player.getX() - septic.getX();
		double dz = player.getZ() - septic.getZ();
		float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		septic.setYRot(yaw);
		septic.setYHeadRot(yaw);
		septic.setYBodyRot(yaw);
		septic.markHorrorStalker(life);
		if (!level.addFreshEntity(septic)) {
			return null;
		}
		level.playSound(null, pos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.HOSTILE, 0.95F, 0.32F + level.random.nextFloat() * 0.08F);
		level.playSound(null, pos, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.85F, 0.45F);
		return septic;
	}

	private static BlockPos stalkerPos(ServerPlayer player) {
		RandomSource random = player.getRandom();
		Vec3 look = player.getViewVector(1.0F);
		Vec3 side = new Vec3(-look.z, 0.0, look.x);
		if (side.lengthSqr() < 1.0E-4) {
			side = new Vec3(1.0, 0.0, 0.0);
		}
		side = side.normalize();
		ServerLevel level = player.serverLevel();
		for (int attempt = 0; attempt < 10; attempt++) {
			Vec3 off;
			if (random.nextFloat() < 0.7F) {
				off = look.scale(-(2.6 + random.nextDouble() * 1.8)).add(side.scale((random.nextDouble() - 0.5) * 2.4));
			} else {
				double dir = random.nextBoolean() ? 1.0 : -1.0;
				off = look.scale(6.5 + random.nextDouble() * 5.0).add(side.scale(dir * (3.2 + random.nextDouble() * 2.8)));
			}
			Vec3 at = player.position().add(off);
			BlockPos stand = findStandPos(level, Mth.floor(at.x), Mth.floor(at.z), player.getBlockY());
			if (stand != null) {
				return stand;
			}
		}
		BlockPos behind = player.blockPosition().relative(player.getDirection().getOpposite(), 2);
		BlockPos stand = findStandPos(level, behind.getX(), behind.getZ(), player.getBlockY());
		return stand != null ? stand : player.blockPosition();
	}

	private static BlockPos findStandPos(ServerLevel level, int x, int z, int playerY) {
		for (int dy = 2; dy >= -3; dy--) {
			BlockPos feet = new BlockPos(x, playerY + dy, z);
			if (canStand(level, feet)) {
				return feet;
			}
		}
		return null;
	}

	private static boolean canStand(ServerLevel level, BlockPos feet) {
		if (!level.isInWorldBounds(feet) || !level.isLoaded(feet)) {
			return false;
		}
		BlockState at = level.getBlockState(feet);
		BlockState head = level.getBlockState(feet.above());
		BlockState floor = level.getBlockState(feet.below());
		return at.isAir() && head.isAir() && floor.blocksMotion();
	}
}
