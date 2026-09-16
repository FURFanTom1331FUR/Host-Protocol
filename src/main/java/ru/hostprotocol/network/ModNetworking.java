package ru.hostprotocol.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
		buf.writeBoolean(data.hasReceivedPda(player.getUUID()));
		long dayTime = player.serverLevel().getServer().overworld().getDayTime();
		buf.writeVarInt(ProtocolTime.dayIndex(dayTime));
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

	public static FriendlyByteBuf createIntroCompletePacket() {
		return PacketByteBufs.create();
	}
}
