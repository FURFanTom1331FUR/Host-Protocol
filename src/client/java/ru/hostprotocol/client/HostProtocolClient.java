package ru.hostprotocol.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.client.screen.IntroScreen;
import ru.hostprotocol.network.ModNetworking;

public class HostProtocolClient implements ClientModInitializer {
	private static String pendingSubjectId;
	private static boolean pendingIntro;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.INTRO_STATE_S2C, (client, handler, buf, responseSender) -> {
			String subjectId = buf.readUtf();
			boolean introCompleted = buf.readBoolean();
			client.execute(() -> {
				if (!introCompleted) {
					pendingSubjectId = subjectId;
					pendingIntro = true;
				} else {
					pendingIntro = false;
					pendingSubjectId = null;
					HostProtocolMod.LOGGER.debug("Intro already completed for subject {}", subjectId);
				}
			});
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!pendingIntro || pendingSubjectId == null) {
				return;
			}
			if (client.player == null || client.level == null) {
				return;
			}
			// Don't stack if already showing
			if (client.screen instanceof IntroScreen) {
				pendingIntro = false;
				return;
			}
			String id = pendingSubjectId;
			pendingIntro = false;
			pendingSubjectId = null;
			client.setScreen(new IntroScreen(id));
		});

		HostProtocolMod.LOGGER.info("Host Protocol client ready");
	}
}
