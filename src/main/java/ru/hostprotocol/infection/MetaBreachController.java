package ru.hostprotocol.infection;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.item.PdaService;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.progress.ProgressionService;

/**
 * When coordinates are locked <em>and</em> the Septic handshake has run, the protocol is
 * considered breached. Client persists a flag, corrupts the title menu, and arms force-close
 * side effects. The bunker siren / PDA system-error beat waits for the next world join.
 */
public final class MetaBreachController {
	private MetaBreachController() {}

	public static void tryArm(MinecraftServer server, IntroWorldData data) {
		if (data.isProtocolBreached()) {
			return;
		}
		if (!data.isCoordsDiscovered() || !data.hasSepticLinkAttempted()) {
			return;
		}
		if (!data.markProtocolBreached()) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ModNetworking.sendBreach(player);
			ModNetworking.sendProtocolState(player, data);
			player.sendSystemMessage(Component.translatable("hostprotocol.breach.chat")
					.withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
		}
		HostProtocolMod.LOGGER.info("Protocol breached: coords + septic handshake both complete");
	}

	public static void onPlayerJoin(ServerPlayer player, IntroWorldData data) {
		if (!data.isProtocolBreached()) {
			return;
		}
		ModNetworking.sendBreach(player);
		if (data.markBreachAcknowledged(player.getUUID())) {
			data.markSystemErrorLog(player.getUUID());
			data.markBlueprints(player.getUUID());
			PdaService.stampSystemError(player);
			PdaService.stampBlueprints(player);
			ProgressionService.grantKnownRecipes(player, data);
			ModNetworking.sendSystemError(player);
			HostProtocolMod.LOGGER.info("Breach acknowledgement (siren + PDA fault) for {}",
					player.getGameProfile().getName());
		}
		ModNetworking.sendProtocolState(player, data);
	}
}
