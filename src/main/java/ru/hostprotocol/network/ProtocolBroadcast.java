package ru.hostprotocol.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.hostprotocol.HostProtocolMod;

/**
 * Always system-chat (never actionbar). Over-communicate beats; they must survive F1 / sleep HUD.
 */
public final class ProtocolBroadcast {
	private ProtocolBroadcast() {}

	public static void chat(ServerPlayer player, Component message) {
		player.sendSystemMessage(message);
	}

	public static void chatAll(MinecraftServer server, Component message) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			chat(player, message);
		}
	}

	public static Component boldPurple(String key, Object... args) {
		return Component.translatable(key, args).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
	}

	public static Component boldGold(String key, Object... args) {
		return Component.translatable(key, args).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
	}

	public static Component boldWhite(String key, Object... args) {
		return Component.translatable(key, args).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
	}

	public static void log(String step, String detail) {
		HostProtocolMod.LOGGER.info("[Host Protocol] {} — {}", step, detail);
	}
}
