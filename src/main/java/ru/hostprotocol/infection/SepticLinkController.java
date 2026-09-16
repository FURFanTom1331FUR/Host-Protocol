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
import ru.hostprotocol.network.TitlePackets;
import ru.hostprotocol.world.ProtocolTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * First Day-2-night sleep while infection is active: Septic tries to link, then drops.
 * Night is not skipped. Once per world. If the player never sleeps, an auto-cinematic fires
 * ~10–20s after coordinates unlock.
 */
public final class SepticLinkController {
	/** ~7 seconds at 20 tps (within the 6–8s brief). */
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
			return false;
		}
		IntroWorldData data = IntroWorldData.get(overworld);
		if (!data.isIntroCompleted()) {
			return false;
		}
		long dayTime = overworld.getDayTime();
		if (ProtocolTime.dayIndex(dayTime) != 2 || !ProtocolTime.isNight(dayTime)) {
			return false;
		}
		if (data.hasSepticLinkAttempted()) {
			return false;
		}
		InfectionTicker.activateIfNeededForSleep(overworld, data);
		if (!data.isInfectionActive()) {
			return false;
		}
		return beginIfNeeded(serverPlayer, data);
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
		long dayTime = overworld.getDayTime();
		return ProtocolTime.dayIndex(dayTime) == 2
				&& ProtocolTime.isNight(dayTime)
				&& data.isInfectionActive()
				&& !data.hasSepticLinkAttempted();
	}

	public static boolean shouldBlockNightSkip(ServerLevel level) {
		if (ticksLeft > 0) {
			return true;
		}
		IntroWorldData data = IntroWorldData.get(level);
		long dayTime = level.getDayTime();
		return ProtocolTime.dayIndex(dayTime) == 2
				&& ProtocolTime.isNight(dayTime)
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
		data.markSepticLinkAttempted();
		begin(player, data);
		MetaBreachController.tryArm(server, data);
		return true;
	}

	private static boolean beginIfNeeded(ServerPlayer sleeper, IntroWorldData data) {
		if (!data.markSepticLinkAttempted()) {
			return false;
		}
		begin(sleeper, data);
		MinecraftServer server = sleeper.getServer();
		if (server != null) {
			MetaBreachController.tryArm(server, data);
		}
		return true;
	}

	private static void begin(ServerPlayer sleeper, IntroWorldData data) {
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
					Component.translatable("hostprotocol.septic.title.link").withStyle(ChatFormatting.DARK_PURPLE),
					Component.translatable("hostprotocol.septic.subtitle.link").withStyle(ChatFormatting.LIGHT_PURPLE),
					8, 40, 12
			);
			player.sendSystemMessage(Component.translatable("hostprotocol.septic.chat.channel")
					.withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
		}
		HostProtocolMod.LOGGER.info("Septic link cinematic started by {} at {}",
				sleeper.getGameProfile().getName(), BlockPos.containing(sleeper.position()));
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
					.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		} else if (elapsed == 88) {
			broadcast(server, "hostprotocol.septic.chat.reject");
			title(server, "hostprotocol.septic.title.drop", "hostprotocol.septic.subtitle.drop");
		} else if (elapsed == 116) {
			broadcast(server, "hostprotocol.septic.chat.torn");
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
		HostProtocolMod.LOGGER.info("Septic auto-cinematic (no sleep) at gameTime={}", overworld.getGameTime());
		begin(lead, data);
		MetaBreachController.tryArm(server, data);
	}

	private static void title(MinecraftServer server, String titleKey, String subtitleKey) {
		for (ServerPlayer player : players(server)) {
			TitlePackets.send(
					player,
					Component.translatable(titleKey).withStyle(ChatFormatting.DARK_PURPLE),
					Component.translatable(subtitleKey).withStyle(ChatFormatting.GRAY),
					6, 36, 10
			);
		}
	}

	private static void broadcast(MinecraftServer server, String key) {
		broadcastRaw(server, Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE));
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
