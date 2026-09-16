package ru.hostprotocol.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.client.fx.DayAnnounceClientFx;
import ru.hostprotocol.client.fx.MaterializeClientFx;
import ru.hostprotocol.client.fx.PdaAppearClientFx;
import ru.hostprotocol.client.hud.DayHud;
import ru.hostprotocol.client.screen.IntroScreen;
import ru.hostprotocol.client.screen.PdaScreen;
import ru.hostprotocol.item.ClientItemScreens;
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
					IntroClientState.setFreezeActive(true);
				} else {
					pendingIntro = false;
					pendingSubjectId = null;
					IntroClientState.setFreezeActive(false);
					HostProtocolMod.LOGGER.debug("Intro already completed for subject {}", subjectId);
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.PROTOCOL_STATE_S2C, (client, handler, buf, responseSender) -> {
			String subjectId = buf.readUtf();
			boolean introCompleted = buf.readBoolean();
			boolean day2 = buf.readBoolean();
			boolean pdaGiven = buf.readBoolean();
			int day = buf.readVarInt();
			client.execute(() -> ProtocolClientState.apply(subjectId, introCompleted, day2, pdaGiven, day));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.PDA_APPEAR_S2C, (client, handler, buf, responseSender) -> {
			String subjectId = buf.readUtf();
			client.execute(() -> PdaAppearClientFx.queue(subjectId));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.DAY_ANNOUNCE_S2C, (client, handler, buf, responseSender) -> {
			int day = buf.readVarInt();
			client.execute(() -> {
				if (IntroClientState.isFreezeActive() || client.screen instanceof IntroScreen) {
					return;
				}
				DayAnnounceClientFx.play(client, day);
			});
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			MaterializeClientFx.clientTick(client);
			PdaAppearClientFx.clientTick(client);
			DayAnnounceClientFx.clientTick();
			if (!pendingIntro || pendingSubjectId == null) {
				return;
			}
			if (client.player == null || client.level == null) {
				return;
			}
			if (client.screen instanceof IntroScreen) {
				pendingIntro = false;
				return;
			}
			String id = pendingSubjectId;
			pendingIntro = false;
			pendingSubjectId = null;
			client.setScreen(new IntroScreen(id));
		});

		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.screen instanceof IntroScreen) {
				return;
			}
			DayHud.render(graphics, client);
			MaterializeClientFx.renderHudOverlay(graphics, client);
			PdaAppearClientFx.render(graphics, client);
			DayAnnounceClientFx.render(graphics, client);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			MaterializeClientFx.cancel(client);
			PdaAppearClientFx.cancel(client);
			DayAnnounceClientFx.cancel();
			ProtocolClientState.reset();
			IntroClientState.setFreezeActive(false);
			pendingIntro = false;
			pendingSubjectId = null;
		});

		ClientItemScreens.OPEN_PDA = stack -> Minecraft.getInstance().setScreen(new PdaScreen(stack));

		HostProtocolMod.LOGGER.info("Host Protocol client ready");
	}
}
