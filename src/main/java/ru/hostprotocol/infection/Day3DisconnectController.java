package ru.hostprotocol.infection;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.freeze.IntroFreeze;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.network.ProtocolBroadcast;
import ru.hostprotocol.network.TitlePackets;
import ru.hostprotocol.world.ProtocolTime;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 3: if the player stays in the world, after 10 seconds play interference + female glitch VO
 * + long busy tone, write a Desktop note, then hard-disconnect to title. Once per player.
 */
public final class Day3DisconnectController {
	private static final Map<UUID, Integer> FX_TICKS_LEFT = new ConcurrentHashMap<>();

	public static boolean isBusy(java.util.UUID playerId) {
		return FX_TICKS_LEFT.containsKey(playerId);
	}

	public static void tick(MinecraftServer server) {
		if (IntroFreeze.isActive() || Day2DebugSequence.isRunning()) {
			return;
		}
		ServerLevel overworld = server.overworld();
		IntroWorldData data = IntroWorldData.get(overworld);
		if (!data.isIntroCompleted()) {
			return;
		}
		int day = ProtocolTime.dayIndex(overworld.getDayTime());
		if (day < 3) {
			return;
		}
		long gameTime = overworld.getGameTime();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			tickPlayer(player, data, gameTime);
		}
		tickKicks(server, data);
	}

	public static void onDisconnect(ServerPlayer player, IntroWorldData data) {
		UUID id = player.getUUID();
		if (FX_TICKS_LEFT.remove(id) != null) {
			data.markDay3DisconnectFired(id);
			HostProtocolMod.LOGGER.info("[Host Protocol] DAY3 FX interrupted by leave; marked fired name={}",
					player.getGameProfile().getName());
			return;
		}
		if (data.hasDay3DisconnectFired(id)) {
			return;
		}
		if (data.getDay3ArmedAtGameTime(id) > 0L) {
			data.markDay3LeftBeforeKick(id);
			HostProtocolMod.LOGGER.info(
					"[Host Protocol] DAY3 player left before kick — will not punish on rejoin name={}",
					player.getGameProfile().getName());
		}
	}

	private static void tickPlayer(ServerPlayer player, IntroWorldData data, long gameTime) {
		UUID id = player.getUUID();
		if (data.hasDay3DisconnectFired(id) || data.hasDay3LeftBeforeKick(id)) {
			return;
		}
		if (FX_TICKS_LEFT.containsKey(id)) {
			return;
		}
		if (data.getDay3ArmedAtGameTime(id) <= 0L) {
			data.armDay3Stay(id, gameTime);
			HostProtocolMod.LOGGER.info(
					"[Host Protocol] DAY3 STAY ARMED player={} gameTime={} kickAt={}",
					player.getGameProfile().getName(), gameTime,
					gameTime + ProtocolTime.DAY3_DISCONNECT_DELAY_TICKS);
			player.sendSystemMessage(ProtocolBroadcast.boldGold("hostprotocol.day3.chat.warn"));
			return;
		}
		long armed = data.getDay3ArmedAtGameTime(id);
		if (gameTime - armed < ProtocolTime.DAY3_DISCONNECT_DELAY_TICKS) {
			return;
		}
		beginFx(player, data);
	}

	private static void beginFx(ServerPlayer player, IntroWorldData data) {
		UUID id = player.getUUID();
		if (FX_TICKS_LEFT.putIfAbsent(id, ProtocolTime.DAY3_KICK_FX_TICKS) != null) {
			return;
		}
		HostProtocolMod.LOGGER.info("[Host Protocol] DAY3 DISCONNECT FX start player={}",
				player.getGameProfile().getName());
		player.sendSystemMessage(ProtocolBroadcast.boldGold("hostprotocol.day3.chat.lost"));
		player.sendSystemMessage(ProtocolBroadcast.boldPurple("hostprotocol.day3.chat.what"));
		player.sendSystemMessage(ProtocolBroadcast.boldWhite("hostprotocol.day3.chat.term"));
		TitlePackets.send(
				player,
				Component.translatable("hostprotocol.day3.title").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
				Component.translatable("hostprotocol.day3.subtitle").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD),
				6, 50, 10
		);
		ModNetworking.sendDay3Disconnect(player);
		ModNetworking.sendProtocolState(player, data);
	}

	private static void tickKicks(MinecraftServer server, IntroWorldData data) {
		if (FX_TICKS_LEFT.isEmpty()) {
			return;
		}
		for (UUID id : FX_TICKS_LEFT.keySet().toArray(new UUID[0])) {
			Integer left = FX_TICKS_LEFT.merge(id, -1, Integer::sum);
			if (left == null || left > 0) {
				continue;
			}
			FX_TICKS_LEFT.remove(id);
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			data.markDay3DisconnectFired(id);
			if (player == null) {
				HostProtocolMod.LOGGER.info("[Host Protocol] DAY3 kick skipped (offline) uuid={}", id);
				continue;
			}
			HostProtocolMod.LOGGER.info("[Host Protocol] DAY3 KICK player={} to title",
					player.getGameProfile().getName());
			player.connection.disconnect(Component.translatable("hostprotocol.day3.kick")
					.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
		}
	}
}
