package ru.hostprotocol.infection;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.freeze.IntroFreeze;
import ru.hostprotocol.item.PdaService;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.network.ProtocolBroadcast;
import ru.hostprotocol.world.ProtocolDayTracker;
import ru.hostprotocol.world.ProtocolTime;

import java.util.UUID;

/**
 * One-command Day 2 verification: complete intro, jump to Day 2, force infection → coords → Septic.
 */
public final class Day2DebugSequence {
	private static final int COORDS_AFTER = 25;
	private static final int SEPTIC_AFTER = 90;

	private static int ticks = -1;
	private static UUID playerId;
	private static boolean infectionDone;
	private static boolean coordsDone;
	private static boolean septicDone;

	private Day2DebugSequence() {}

	public static boolean isRunning() {
		return ticks >= 0 && (!infectionDone || !coordsDone || !septicDone);
	}

	public static int start(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return 0;
		}
		ServerLevel overworld = server.overworld();
		IntroWorldData data = IntroWorldData.get(overworld);

		if (!data.isIntroCompleted()) {
			data.markIntroCompleted();
			IntroFreeze.end(player);
			ProtocolDayTracker.onIntroCompleted(player, data);
			PdaService.grantIfNeeded(player, data, true);
			ModNetworking.sendIntroState(player, data.getSubjectId(), true);
			HostProtocolMod.LOGGER.info("[Host Protocol] DAY2 DEBUG completed intro for {}",
					player.getGameProfile().getName());
		} else {
			IntroFreeze.end(player);
		}

		if (ProtocolTime.dayIndex(overworld.getDayTime()) < 2) {
			overworld.setDayTime(24000L);
			HostProtocolMod.LOGGER.info("[Host Protocol] DAY2 DEBUG setDayTime=24000");
		}

		playerId = player.getUUID();
		ticks = 0;
		infectionDone = true;
		coordsDone = false;
		septicDone = false;
		InfectionTicker.forceActivate(overworld, data);
		player.sendSystemMessage(ProtocolBroadcast.boldGold("hostprotocol.command.day2.started"));
		HostProtocolMod.LOGGER.info("[Host Protocol] DAY2 DEBUG SEQUENCE start player={} dayTime={} gameTime={} focus={}",
				player.getGameProfile().getName(), overworld.getDayTime(), overworld.getGameTime(),
				data.hasInfectionFocus() ? data.getFocusX() + "," + data.getFocusY() + "," + data.getFocusZ() : "none");
		return 1;
	}

	public static void tick(MinecraftServer server) {
		if (ticks < 0) {
			return;
		}
		ServerPlayer player = playerId == null ? null : server.getPlayerList().getPlayer(playerId);
		if (player == null) {
			HostProtocolMod.LOGGER.info("[Host Protocol] DAY2 DEBUG aborted (player offline)");
			ticks = -1;
			return;
		}
		ServerLevel overworld = server.overworld();
		IntroWorldData data = IntroWorldData.get(overworld);

		if (!infectionDone && ticks >= 1) {
			InfectionTicker.forceActivate(overworld, data);
			infectionDone = true;
			HostProtocolMod.LOGGER.info("[Host Protocol] DAY2 DEBUG infection forced");
		}
		if (!coordsDone && ticks >= COORDS_AFTER) {
			InfectionTicker.unlockCoords(server, data, true);
			coordsDone = true;
			HostProtocolMod.LOGGER.info("[Host Protocol] DAY2 DEBUG coords forced");
		}
		if (!septicDone && ticks >= SEPTIC_AFTER) {
			SepticLinkController.forcePlay(player);
			septicDone = true;
			HostProtocolMod.LOGGER.info("[Host Protocol] DAY2 DEBUG septic forced");
			player.sendSystemMessage(Component.translatable("hostprotocol.command.day2.done"));
			ticks = -1;
			return;
		}
		ticks++;
	}
}
