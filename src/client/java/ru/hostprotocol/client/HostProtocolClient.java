package ru.hostprotocol.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GrassColor;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.block.ModBlocks;
import ru.hostprotocol.client.fx.DayAnnounceClientFx;
import ru.hostprotocol.client.fx.MaterializeClientFx;
import ru.hostprotocol.client.fx.PdaAppearClientFx;
import ru.hostprotocol.client.fx.SepticLinkClientFx;
import ru.hostprotocol.client.hud.DayHud;
import ru.hostprotocol.client.screen.IntroScreen;
import ru.hostprotocol.client.screen.PdaScreen;
import ru.hostprotocol.item.ClientItemScreens;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.world.ProtocolTime;

public class HostProtocolClient implements ClientModInitializer {
	private static String pendingSubjectId;
	private static boolean pendingIntro;

	@Override
	public void onInitializeClient() {
		BlockRenderLayerMap.INSTANCE.putBlocks(
				RenderType.cutoutMipped(),
				ModBlocks.INFECTED_STONE,
				ModBlocks.INFECTED_COBBLESTONE,
				ModBlocks.INFECTED_DIRT,
				ModBlocks.INFECTED_GRASS_BLOCK,
				ModBlocks.INFECTED_OAK_LOG,
				ModBlocks.INFECTED_OAK_PLANKS
		);
		ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
			if (tintIndex != 0) {
				return -1;
			}
			if (world == null || pos == null) {
				return GrassColor.getDefaultColor();
			}
			return BiomeColors.getAverageGrassColor(world, pos);
		}, ModBlocks.INFECTED_GRASS_BLOCK);

		EntitySleepEvents.ALLOW_SLEEPING.register((player, pos) -> {
			if (!player.level().isClientSide()) {
				return null;
			}
			if (ProtocolClientState.hasSepticLinkAttempted() || !ProtocolClientState.isInfectionActive()) {
				return null;
			}
			if (ProtocolTime.dayIndex(player.level().getDayTime()) != 2) {
				return null;
			}
			return Player.BedSleepingProblem.OTHER_PROBLEM;
		});

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
			boolean day2Coords = buf.readBoolean();
			boolean day3 = buf.readBoolean();
			boolean pdaGiven = buf.readBoolean();
			int day = buf.readVarInt();
			boolean infection = buf.readBoolean();
			boolean septicLink = buf.readBoolean();
			boolean hasCoords = buf.readBoolean();
			BlockPos focus = hasCoords ? buf.readBlockPos() : null;
			String garbled = buf.readUtf();
			client.execute(() -> ProtocolClientState.apply(
					subjectId, introCompleted, day2, day2Coords, day3, pdaGiven, day,
					infection, septicLink, hasCoords, focus, garbled));
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

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SEPTIC_LINK_S2C, (client, handler, buf, responseSender) -> {
			String subjectId = buf.readUtf();
			String garbled = buf.readUtf();
			int duration = buf.readVarInt();
			client.execute(() -> SepticLinkClientFx.play(client, subjectId, garbled, duration));
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			MaterializeClientFx.clientTick(client);
			PdaAppearClientFx.clientTick(client);
			DayAnnounceClientFx.clientTick();
			SepticLinkClientFx.clientTick(client);
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
			SepticLinkClientFx.render(graphics, client);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			MaterializeClientFx.cancel(client);
			PdaAppearClientFx.cancel(client);
			DayAnnounceClientFx.cancel();
			SepticLinkClientFx.cancel(client);
			ProtocolClientState.reset();
			IntroClientState.setFreezeActive(false);
			pendingIntro = false;
			pendingSubjectId = null;
		});

		ClientItemScreens.OPEN_PDA = stack -> Minecraft.getInstance().setScreen(new PdaScreen(stack));

		HostProtocolMod.LOGGER.info("Host Protocol client ready");
	}
}
