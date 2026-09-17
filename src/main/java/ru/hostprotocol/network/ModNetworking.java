package ru.hostprotocol.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.world.ProtocolTime;

public final class ModNetworking {
	public static final ResourceLocation INTRO_STATE_S2C = HostProtocolMod.id("intro_state");
	public static final ResourceLocation INTRO_COMPLETE_C2S = HostProtocolMod.id("intro_complete");
	public static final ResourceLocation PROTOCOL_STATE_S2C = HostProtocolMod.id("protocol_state");
	public static final ResourceLocation PDA_APPEAR_S2C = HostProtocolMod.id("pda_appear");
	public static final ResourceLocation DAY_ANNOUNCE_S2C = HostProtocolMod.id("day_announce");
	public static final ResourceLocation SEPTIC_LINK_S2C = HostProtocolMod.id("septic_link");
	public static final ResourceLocation COORDS_UNLOCK_S2C = HostProtocolMod.id("coords_unlock");
	public static final ResourceLocation BREACH_S2C = HostProtocolMod.id("protocol_breach");
	public static final ResourceLocation SYSTEM_ERROR_S2C = HostProtocolMod.id("system_error");
	public static final ResourceLocation INFECTION_ACTIVE_S2C = HostProtocolMod.id("infection_active");
	public static final ResourceLocation DAY3_DISCONNECT_S2C = HostProtocolMod.id("day3_disconnect");
	public static final ResourceLocation BLUEPRINT_UPDATE_S2C = HostProtocolMod.id("blueprint_update");
	public static final ResourceLocation TRANSFER_COMPLETE_S2C = HostProtocolMod.id("transfer_complete");
	public static final ResourceLocation MODULE_ONLINE_S2C = HostProtocolMod.id("module_online");

	private ModNetworking() {}

	public static void registerServer() {
		// Receivers registered in HostProtocolMod
	}

	public static void sendIntroState(ServerPlayer player, String subjectId, boolean introCompleted) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeUtf(subjectId);
		buf.writeBoolean(introCompleted);
		ServerPlayNetworking.send(player, INTRO_STATE_S2C, buf);
	}

	public static void sendProtocolState(ServerPlayer player, IntroWorldData data) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeUtf(data.getSubjectId());
		buf.writeBoolean(data.isIntroCompleted());
		buf.writeBoolean(data.hasDay2Log(player.getUUID()));
		buf.writeBoolean(data.hasDay2CoordsLog(player.getUUID()) && data.isCoordsDiscovered());
		buf.writeBoolean(data.hasDay3Log(player.getUUID()));
		buf.writeBoolean(data.hasReceivedPda(player.getUUID()));
		long dayTime = player.serverLevel().getServer().overworld().getDayTime();
		buf.writeVarInt(ProtocolTime.dayIndex(dayTime));
		buf.writeBoolean(data.isInfectionActive());
		buf.writeBoolean(data.hasSepticLinkAttempted());
		boolean coords = data.isCoordsDiscovered() && data.hasInfectionFocus();
		buf.writeBoolean(coords);
		if (coords) {
			buf.writeBlockPos(new BlockPos(data.getFocusX(), data.getFocusY(), data.getFocusZ()));
		}
		buf.writeUtf(data.garbledSubjectId());
		buf.writeBoolean(data.isProtocolBreached());
		buf.writeBoolean(data.hasSystemErrorLog(player.getUUID()));
		buf.writeBoolean(data.hasBlueprints(player.getUUID()));
		buf.writeBoolean(data.hasLabBlueprints(player.getUUID()));
		buf.writeBoolean(data.hasTransferred(player.getUUID()));
		buf.writeBoolean(data.hasBaseBlueprints(player.getUUID()));
		buf.writeBoolean(data.hasMk2Booted(player.getUUID()));
		buf.writeVarInt(ru.hostprotocol.infection.InfectedMobs.playerStage(player, data));
		buf.writeBoolean(data.isSepticSpawned() && ProtocolTime.dayIndex(dayTime) >= 5);
		java.util.List<String> scans = data.getRecentScans(player.getUUID());
		buf.writeVarInt(scans.size());
		for (String scan : scans) {
			buf.writeUtf(scan);
		}
		ServerPlayNetworking.send(player, PROTOCOL_STATE_S2C, buf);
	}

	public static void sendPdaAppear(ServerPlayer player, String subjectId) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeUtf(subjectId);
		ServerPlayNetworking.send(player, PDA_APPEAR_S2C, buf);
	}

	public static void sendDayAnnounce(ServerPlayer player, int day) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(day);
		ServerPlayNetworking.send(player, DAY_ANNOUNCE_S2C, buf);
	}

	public static void sendSepticLink(ServerPlayer player, String subjectId, String garbledId, int durationTicks) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeUtf(subjectId);
		buf.writeUtf(garbledId);
		buf.writeVarInt(durationTicks);
		ServerPlayNetworking.send(player, SEPTIC_LINK_S2C, buf);
	}

	public static void sendCoordsUnlock(ServerPlayer player, int x, int y, int z) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBlockPos(new BlockPos(x, y, z));
		ServerPlayNetworking.send(player, COORDS_UNLOCK_S2C, buf);
	}

	public static void sendBreach(ServerPlayer player) {
		ServerPlayNetworking.send(player, BREACH_S2C, PacketByteBufs.create());
	}

	public static void sendSystemError(ServerPlayer player) {
		ServerPlayNetworking.send(player, SYSTEM_ERROR_S2C, PacketByteBufs.create());
	}

	public static void sendInfectionActive(ServerPlayer player) {
		ServerPlayNetworking.send(player, INFECTION_ACTIVE_S2C, PacketByteBufs.create());
	}

	public static void sendDay3Disconnect(ServerPlayer player) {
		ServerPlayNetworking.send(player, DAY3_DISCONNECT_S2C, PacketByteBufs.create());
	}

	public static void sendBlueprintUpdate(ServerPlayer player) {
		ServerPlayNetworking.send(player, BLUEPRINT_UPDATE_S2C, PacketByteBufs.create());
	}

	public static void sendTransferComplete(ServerPlayer player) {
		ServerPlayNetworking.send(player, TRANSFER_COMPLETE_S2C, PacketByteBufs.create());
	}

	public static void sendModuleOnline(ServerPlayer player) {
		ServerPlayNetworking.send(player, MODULE_ONLINE_S2C, PacketByteBufs.create());
	}

	public static FriendlyByteBuf createIntroCompletePacket() {
		return PacketByteBufs.create();
	}
}
