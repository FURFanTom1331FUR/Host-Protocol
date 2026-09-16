package ru.hostprotocol.infection;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.world.ProtocolTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * First Day-2-night sleep while infection is active: Septic tries to link, then drops.
 * Night is not skipped. Once per world.
 */
public final class SepticLinkController {
	public static final int DURATION_TICKS = 120; // 6s

	private static int ticksLeft;
	private static final List<UUID> audience = new ArrayList<>();
	private static String garbledId = "С000Х";

	private SepticLinkController() {}

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
		if (ProtocolTime.dayIndex(dayTime) != 2) {
			return false;
		}
		if (data.hasSepticLinkAttempted()) {
			return false;
		}
		InfectionTicker.activateIfNeededForSleep(overworld, data);
		if (!data.isInfectionActive()) {
			return false;
		}
		if (!data.markSepticLinkAttempted()) {
			return false;
		}
		begin(serverPlayer, data);
		return true;
	}

	public static boolean shouldBlockTimeReset(Player player) {
		if (player.level().isClientSide()) {
			return false;
		}
		ServerLevel overworld = player.getServer() == null ? null : player.getServer().overworld();
		if (overworld == null) {
			return false;
		}
		IntroWorldData data = IntroWorldData.get(overworld);
		long dayTime = overworld.getDayTime();
		return ProtocolTime.dayIndex(dayTime) == 2
				&& data.isInfectionActive()
				&& !data.hasSepticLinkAttempted();
	}

	private static void begin(ServerPlayer sleeper, IntroWorldData data) {
		ticksLeft = DURATION_TICKS;
		audience.clear();
		garbledId = data.garbledSubjectId();
		MinecraftServer server = sleeper.getServer();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.serverLevel() != server.overworld()) {
				continue;
			}
			audience.add(player.getUUID());
			ModNetworking.sendSepticLink(player, data.getSubjectId(), garbledId, DURATION_TICKS);
			ModNetworking.sendProtocolState(player, data);
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DURATION_TICKS + 10, 0, false, false, false));
			player.connection.send(new ClientboundSetTitlesAnimationPacket(8, 36, 12));
			player.connection.send(new ClientboundSetTitleTextPacket(
					Component.translatable("hostprotocol.septic.title.link").withStyle(ChatFormatting.DARK_PURPLE)));
			player.connection.send(new ClientboundSetSubtitleTextPacket(
					Component.translatable("hostprotocol.septic.subtitle.link").withStyle(ChatFormatting.GRAY)));
		}
		sleeper.displayClientMessage(Component.translatable("hostprotocol.septic.subtitle.link"), true);
		HostProtocolMod.LOGGER.info("Septic link cinematic started by {} at {}",
				sleeper.getGameProfile().getName(), BlockPos.containing(sleeper.position()));
	}

	public static void tick(MinecraftServer server) {
		if (ticksLeft <= 0) {
			return;
		}
		int elapsed = DURATION_TICKS - ticksLeft;
		if (elapsed == 0) {
			broadcast(server, "hostprotocol.septic.chat.channel");
		} else if (elapsed == 24) {
			broadcast(server, "hostprotocol.septic.chat.node");
			title(server, "hostprotocol.septic.title.presence", "hostprotocol.septic.subtitle.presence");
		} else if (elapsed == 52) {
			broadcastRaw(server, Component.translatable("hostprotocol.septic.chat.echo", garbledId)
					.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		} else if (elapsed == 78) {
			broadcast(server, "hostprotocol.septic.chat.reject");
			title(server, "hostprotocol.septic.title.drop", "hostprotocol.septic.subtitle.drop");
		} else if (elapsed == 104) {
			broadcast(server, "hostprotocol.septic.chat.torn");
		}
		ticksLeft--;
		if (ticksLeft <= 0) {
			audience.clear();
		}
	}

	private static void title(MinecraftServer server, String titleKey, String subtitleKey) {
		for (ServerPlayer player : players(server)) {
			player.connection.send(new ClientboundSetTitlesAnimationPacket(6, 32, 10));
			player.connection.send(new ClientboundSetTitleTextPacket(
					Component.translatable(titleKey).withStyle(ChatFormatting.DARK_PURPLE)));
			player.connection.send(new ClientboundSetSubtitleTextPacket(
					Component.translatable(subtitleKey).withStyle(ChatFormatting.GRAY)));
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
