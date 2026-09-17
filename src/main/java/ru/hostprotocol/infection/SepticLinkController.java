package ru.hostprotocol.infection;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.network.ProtocolBroadcast;
import ru.hostprotocol.network.TitlePackets;
import ru.hostprotocol.world.ProtocolTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Day-2 Septic handshake: auto-plays ~5–10s after coords even without sleep.
 * Sleep on Day 2+ still triggers if not yet done. Night skip cancelled for that first attempt.
 */
public final class SepticLinkController {
	/** ~7 seconds at 20 tps. */
	public static final int DURATION_TICKS = 140;

	private static int ticksLeft;
	private static final List<UUID> audience = new ArrayList<>();
	private static String garbledId = "С000Х";

	private SepticLinkController() {}

	public static boolean isCinematicRunning() {
		return ticksLeft > 0;
	}

	/**
	 * Bed click / {@code startSleepInBed}. Returns true if the sleep should be cancelled
	 * and the handshake cinematic started instead.
	 */
	public static boolean tryInterceptSleep(Player player) {
		if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		if (serverPlayer.getServer() == null) {
			return false;
		}
		ServerLevel overworld = serverPlayer.getServer().overworld();
		if (serverPlayer.serverLevel() != overworld) {
			HostProtocolMod.LOGGER.info("[Host Protocol] SLEEP intercept skipped (not overworld) player={}",
					serverPlayer.getGameProfile().getName());
			return false;
		}
		IntroWorldData data = IntroWorldData.get(overworld);
		if (!data.isIntroCompleted()) {
			HostProtocolMod.LOGGER.info("[Host Protocol] SLEEP intercept skipped (intro incomplete) player={}",
					serverPlayer.getGameProfile().getName());
			return false;
		}
		long dayTime = overworld.getDayTime();
		int day = ProtocolTime.dayIndex(dayTime);
		if (day < 2) {
			HostProtocolMod.LOGGER.info("[Host Protocol] SLEEP intercept skipped (day={}) player={}",
					day, serverPlayer.getGameProfile().getName());
			return false;
		}
		if (data.hasSepticLinkAttempted()) {
			return false;
		}
		InfectionTicker.activateIfNeededForSleep(overworld, data);
		if (!data.isInfectionActive()) {
			HostProtocolMod.LOGGER.info("[Host Protocol] SLEEP intercept skipped (infection still inactive) player={}",
					serverPlayer.getGameProfile().getName());
			return false;
		}
		HostProtocolMod.LOGGER.info("[Host Protocol] SLEEP intercept firing septic player={} day={} dayTime={}",
				serverPlayer.getGameProfile().getName(), day, dayTime);
		return beginIfNeeded(serverPlayer, data, "sleep");
	}

	public static boolean shouldBlockTimeReset(Player player) {
		if (player.level().isClientSide()) {
			return false;
		}
		if (ticksLeft > 0) {
			return true;
		}
		ServerLevel overworld = player.getServer() == null ? null : player.getServer().overworld();
		if (overworld == null) {
			return false;
		}
		IntroWorldData data = IntroWorldData.get(overworld);
		return ProtocolTime.isDay2OrLater(overworld.getDayTime())
				&& data.isInfectionActive()
				&& !data.hasSepticLinkAttempted();
	}

	public static boolean shouldBlockNightSkip(ServerLevel level) {
		if (ticksLeft > 0) {
			return true;
		}
		IntroWorldData data = IntroWorldData.get(level);
		return ProtocolTime.isDay2OrLater(level.getDayTime())
				&& data.isInfectionActive()
				&& !data.hasSepticLinkAttempted();
	}

	public static boolean forcePlay(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return false;
		}
		ServerLevel overworld = server.overworld();
		IntroWorldData data = IntroWorldData.get(overworld);
		InfectionTicker.forceActivate(overworld, data);
		begin(player, data, "force");
		data.markSepticLinkAttempted();
		MetaBreachController.tryArm(server, data);
		return true;
	}

	private static boolean beginIfNeeded(ServerPlayer sleeper, IntroWorldData data, String reason) {
		if (!data.markSepticLinkAttempted()) {
			return false;
		}
		begin(sleeper, data, reason);
		MinecraftServer server = sleeper.getServer();
		if (server != null) {
			MetaBreachController.tryArm(server, data);
		}
		return true;
	}

	private static void begin(ServerPlayer sleeper, IntroWorldData data, String reason) {
		ticksLeft = DURATION_TICKS;
		audience.clear();
		garbledId = data.garbledSubjectId();
		MinecraftServer server = sleeper.getServer();
		if (sleeper.isSleeping()) {
			sleeper.stopSleepInBed(true, true);
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.serverLevel() != server.overworld()) {
				continue;
			}
			if (player.isSleeping()) {
				player.stopSleepInBed(true, true);
			}
			audience.add(player.getUUID());
			ModNetworking.sendSepticLink(player, data.getSubjectId(), garbledId, DURATION_TICKS);
			ModNetworking.sendProtocolState(player, data);
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DURATION_TICKS + 10, 0, false, false, false));
			TitlePackets.send(
					player,
					Component.translatable("hostprotocol.septic.title.link").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
					Component.translatable("hostprotocol.septic.subtitle.link").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
					8, 50, 12
			);
			player.sendSystemMessage(ProtocolBroadcast.boldGold("hostprotocol.septic.chat.alert"));
			player.sendSystemMessage(ProtocolBroadcast.boldPurple("hostprotocol.septic.chat.knock"));
			player.sendSystemMessage(ProtocolBroadcast.boldWhite("hostprotocol.septic.chat.channel"));
		}
		HostProtocolMod.LOGGER.info(
				"[Host Protocol] SEPTIC LINK FIRED reason={} by={} at={} dayTime={} gameTime={}",
				reason,
				sleeper.getGameProfile().getName(),
				BlockPos.containing(sleeper.position()),
				server.overworld().getDayTime(),
				server.overworld().getGameTime());
	}

	public static void tick(MinecraftServer server) {
		tickAuto(server);
		if (ticksLeft <= 0) {
			return;
		}
		int elapsed = DURATION_TICKS - ticksLeft;
		if (elapsed == 28) {
			broadcast(server, "hostprotocol.septic.chat.node");
			title(server, "hostprotocol.septic.title.presence", "hostprotocol.septic.subtitle.presence");
		} else if (elapsed == 56) {
			broadcastRaw(server, Component.translatable("hostprotocol.septic.chat.echo", garbledId)
					.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC, ChatFormatting.BOLD));
		} else if (elapsed == 88) {
			broadcast(server, "hostprotocol.septic.chat.reject");
			title(server, "hostprotocol.septic.title.drop", "hostprotocol.septic.subtitle.drop");
		} else if (elapsed == 116) {
			broadcast(server, "hostprotocol.septic.chat.torn");
			HostProtocolMod.LOGGER.info("[Host Protocol] SEPTIC DROP / disconnect chat");
		}
		ticksLeft--;
		if (ticksLeft <= 0) {
			audience.clear();
		}
	}

	private static void tickAuto(MinecraftServer server) {
		if (ticksLeft > 0) {
			return;
		}
		ServerLevel overworld = server.overworld();
		IntroWorldData data = IntroWorldData.get(overworld);
		if (data.hasSepticLinkAttempted() || !data.isCoordsDiscovered()) {
			return;
		}
		long at = data.getSepticAutoAtGameTime();
		if (at <= 0L) {
			long delay = ProtocolTime.septicAutoGameDelay(overworld.getSeed());
			data.setSepticAutoAtGameTime(overworld.getGameTime() + delay);
			HostProtocolMod.LOGGER.info("[Host Protocol] SEPTIC AUTO catch-up armed in {} ticks", delay);
			return;
		}
		if (overworld.getGameTime() < at) {
			return;
		}
		ServerPlayer lead = null;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.serverLevel() == overworld) {
				lead = player;
				break;
			}
		}
		if (lead == null) {
			return;
		}
		if (!data.markSepticLinkAttempted()) {
			return;
		}
		HostProtocolMod.LOGGER.info("[Host Protocol] SEPTIC AUTO (no sleep) at gameTime={}", overworld.getGameTime());
		begin(lead, data, "auto");
		MetaBreachController.tryArm(server, data);
	}

	private static void title(MinecraftServer server, String titleKey, String subtitleKey) {
		for (ServerPlayer player : players(server)) {
			TitlePackets.send(
					player,
					Component.translatable(titleKey).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
					Component.translatable(subtitleKey).withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD),
					6, 36, 10
			);
		}
	}

	private static void broadcast(MinecraftServer server, String key) {
		broadcastRaw(server, Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
	}

	private static void broadcastRaw(MinecraftServer server, Component message) {
		for (ServerPlayer player : players(server)) {
			player.sendSystemMessage(message);
		}
	}

	private static List<ServerPlayer> players(MinecraftServer server) {
		List<ServerPlayer> out = new ArrayList<>();
		for (UUID id : audience) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null) {
				out.add(player);
			}
		}
		return out;
	}
}
