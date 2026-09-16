package ru.hostprotocol.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.freeze.IntroFreeze;
import ru.hostprotocol.item.PdaService;
import ru.hostprotocol.network.ModNetworking;

/**
 * Detects dawn / day-index increases after intro and fires wake announcements + Day-2 PDA log.
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
		if (day >= 2) {
			unlockDay2(player, data);
		}
		ModNetworking.sendProtocolState(player, data);
	}

	private static void tickPlayer(ServerPlayer player, IntroWorldData data, int day) {
		int last = data.getLastAnnouncedDay(player.getUUID());
		if (last <= 0) {
			// Reconnect / first observation of a completed world: do not spam the current day.
			data.setLastAnnouncedDay(player.getUUID(), day);
			if (day >= 2) {
				unlockDay2(player, data);
			}
			ModNetworking.sendProtocolState(player, data);
			return;
		}
		if (day <= last) {
			return;
		}
		data.setLastAnnouncedDay(player.getUUID(), day);
		ModNetworking.sendDayAnnounce(player, day);
		if (day >= 2) {
			unlockDay2(player, data);
		}
		ModNetworking.sendProtocolState(player, data);
	}

	private static void unlockDay2(ServerPlayer player, IntroWorldData data) {
		if (data.markDay2Log(player.getUUID())) {
			PdaService.stampDay2Log(player);
		}
	}
}
