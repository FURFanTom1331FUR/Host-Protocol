package ru.hostprotocol.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.freeze.IntroFreeze;
import ru.hostprotocol.item.PdaService;
import ru.hostprotocol.network.ModNetworking;

/**
 * Detects dawn / day-index increases after intro and fires wake announcements + PDA logs.
 */
public final class ProtocolDayTracker {
	private ProtocolDayTracker() {}

	public static void tick(MinecraftServer server) {
		IntroWorldData data = IntroWorldData.get(server.overworld());
		if (!data.isIntroCompleted()) {
			return;
		}
		int day = ProtocolTime.dayIndex(server.overworld().getDayTime());
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (IntroFreeze.isFrozen(player.getUUID())) {
				continue;
			}
			tickPlayer(player, data, day);
		}
	}

	public static void onIntroCompleted(ServerPlayer player, IntroWorldData data) {
		int day = ProtocolTime.dayIndex(player.serverLevel().getServer().overworld().getDayTime());
		data.setLastAnnouncedDay(player.getUUID(), day);
		unlockLogs(player, data, day);
		ModNetworking.sendProtocolState(player, data);
	}

	private static void tickPlayer(ServerPlayer player, IntroWorldData data, int day) {
		int last = data.getLastAnnouncedDay(player.getUUID());
		if (last <= 0) {
			// Reconnect / first observation of a completed world: do not spam the current day.
			data.setLastAnnouncedDay(player.getUUID(), day);
			unlockLogs(player, data, day);
			ModNetworking.sendProtocolState(player, data);
			return;
		}
		if (day <= last) {
			return;
		}
		data.setLastAnnouncedDay(player.getUUID(), day);
		ModNetworking.sendDayAnnounce(player, day);
		unlockLogs(player, data, day);
		ModNetworking.sendProtocolState(player, data);
	}

	private static void unlockLogs(ServerPlayer player, IntroWorldData data, int day) {
		if (day >= 2 && data.markDay2Log(player.getUUID())) {
			PdaService.stampDay2Log(player);
		}
		if (day >= 2 && data.markBlueprints(player.getUUID())) {
			PdaService.stampBlueprints(player);
		}
		if (day >= 3 && data.markDay3Log(player.getUUID())) {
			PdaService.stampDay3Log(player);
		}
		if (data.isCoordsDiscovered()) {
			PdaService.stampDay2Coords(player, data);
		}
	}
}
