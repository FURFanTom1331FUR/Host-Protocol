package ru.hostprotocol.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import ru.hostprotocol.HostProtocolMod;

public final class ModNetworking {
	public static final ResourceLocation INTRO_STATE_S2C = HostProtocolMod.id("intro_state");
	public static final ResourceLocation INTRO_COMPLETE_C2S = HostProtocolMod.id("intro_complete");

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

	public static FriendlyByteBuf createIntroCompletePacket() {
		return PacketByteBufs.create();
	}
}
