package ru.hostprotocol.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GrassColor;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.block.ModBlocks;
import ru.hostprotocol.client.breach.BreachClientFlags;
import ru.hostprotocol.client.breach.BreachWatchdog;
import ru.hostprotocol.client.fx.BlueprintUpdateClientFx;
import ru.hostprotocol.client.fx.CoordsUnlockClientFx;
import ru.hostprotocol.client.fx.Day3DisconnectClientFx;
import ru.hostprotocol.client.fx.DayAnnounceClientFx;
import ru.hostprotocol.client.fx.InfectionActiveClientFx;
import ru.hostprotocol.client.fx.MaterializeClientFx;
import ru.hostprotocol.client.fx.ModuleOnlineClientFx;
import ru.hostprotocol.client.fx.PdaAppearClientFx;
import ru.hostprotocol.client.fx.ScanBeamRenderer;
import ru.hostprotocol.client.fx.SepticLinkClientFx;
import ru.hostprotocol.client.fx.SepticPresenceClientFx;
import ru.hostprotocol.client.fx.SystemErrorClientFx;
import ru.hostprotocol.client.fx.TransferCompleteClientFx;
import ru.hostprotocol.client.hud.DayHud;
import ru.hostprotocol.client.hud.ScanHud;
import ru.hostprotocol.client.render.SepticRenderer;
import ru.hostprotocol.client.screen.IntroScreen;
import ru.hostprotocol.client.screen.LabTableScreen;
import ru.hostprotocol.client.screen.PdaScreen;
import ru.hostprotocol.client.sound.ScanHumClient;
import ru.hostprotocol.entity.ModEntityTypes;
import ru.hostprotocol.infection.InfectionVisuals;
import ru.hostprotocol.item.ClientItemScreens;
import ru.hostprotocol.item.ModItems;
import ru.hostprotocol.menu.ModMenus;
import ru.hostprotocol.network.ModNetworking;

public class HostProtocolClient implements ClientModInitializer {
	private static String pendingSubjectId;
	private static boolean pendingIntro;

	@Override
	public void onInitializeClient() {
		BlockRenderLayerMap.INSTANCE.putBlocks(
				RenderType.cutout(),
				ModBlocks.INFECTED_STONE,
				ModBlocks.INFECTED_COBBLESTONE,
				ModBlocks.INFECTED_DIRT,
				ModBlocks.INFECTED_GRASS_BLOCK,
				ModBlocks.INFECTED_OAK_LOG,
				ModBlocks.INFECTED_OAK_PLANKS
		);
		ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
			if (tintIndex == 1) {
				return InfectionVisuals.BLOCK_TINT;
			}
			if (tintIndex != 0) {
				return -1;
			}
			int grass;
			if (world == null || pos == null) {
				grass = GrassColor.getDefaultColor();
			} else {
				grass = BiomeColors.getAverageGrassColor(world, pos);
			}
			return InfectionVisuals.stainGrass(grass);
		}, ModBlocks.INFECTED_GRASS_BLOCK);
		ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
			if (tintIndex == 1) {
				return InfectionVisuals.BLOCK_TINT;
			}
			return -1;
		},
				ModBlocks.INFECTED_STONE,
				ModBlocks.INFECTED_COBBLESTONE,
				ModBlocks.INFECTED_DIRT,
				ModBlocks.INFECTED_OAK_LOG,
				ModBlocks.INFECTED_OAK_PLANKS);

		MenuScreens.register(ModMenus.LAB_TABLE, LabTableScreen::new);
		EntityRendererRegistry.register(ModEntityTypes.SEPTIC, SepticRenderer::new);
		ResourceLocation suit1 = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/models/armor/protective_suit_layer_1.png");
		ResourceLocation suit2 = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/models/armor/protective_suit_layer_2.png");
		ArmorRenderer.register((matrices, vertexConsumers, stack, entity, slot, light, contextModel) -> {
			ResourceLocation tex = slot == net.minecraft.world.entity.EquipmentSlot.LEGS ? suit2 : suit1;
			ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, contextModel, tex);
		}, ModItems.PROTECTIVE_HELMET, ModItems.PROTECTIVE_CHESTPLATE, ModItems.PROTECTIVE_LEGGINGS, ModItems.PROTECTIVE_BOOTS);

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			BreachClientFlags.load(client);
			if (BreachClientFlags.isBreached()) {
				BreachWatchdog.arm(client);
			}
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
			boolean breached = buf.readBoolean();
			boolean systemError = buf.readBoolean();
			boolean blueprints = buf.readBoolean();
			boolean labBlueprints = buf.readBoolean();
			boolean transferred = buf.readBoolean();
			boolean baseBp = buf.readBoolean();
			boolean mk2Booted = buf.readBoolean();
			int infectionStage = buf.readVarInt();
			boolean septicPresent = buf.readBoolean();
			int scanCount = buf.readVarInt();
			java.util.List<String> scans = new java.util.ArrayList<>();
			for (int i = 0; i < scanCount; i++) {
				scans.add(buf.readUtf());
			}
			client.execute(() -> {
				ProtocolClientState.apply(
						subjectId, introCompleted, day2, day2Coords, day3, pdaGiven, day,
						infection, septicLink, hasCoords, focus, garbled,
						breached, systemError, blueprints, labBlueprints, transferred, baseBp,
						mk2Booted, infectionStage, septicPresent, scans);
				if (breached) {
					BreachWatchdog.arm(client);
				}
			});
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

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.COORDS_UNLOCK_S2C, (client, handler, buf, responseSender) -> {
			BlockPos pos = buf.readBlockPos();
			client.execute(() -> CoordsUnlockClientFx.play(client, pos.getX(), pos.getY(), pos.getZ()));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.BREACH_S2C, (client, handler, buf, responseSender) -> {
			client.execute(() -> BreachWatchdog.arm(client));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SYSTEM_ERROR_S2C, (client, handler, buf, responseSender) -> {
			client.execute(() -> SystemErrorClientFx.play(client));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.INFECTION_ACTIVE_S2C, (client, handler, buf, responseSender) -> {
			client.execute(() -> InfectionActiveClientFx.play(client));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.DAY3_DISCONNECT_S2C, (client, handler, buf, responseSender) -> {
			client.execute(() -> Day3DisconnectClientFx.play(client));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.BLUEPRINT_UPDATE_S2C, (client, handler, buf, responseSender) -> {
			client.execute(() -> BlueprintUpdateClientFx.play(client));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.TRANSFER_COMPLETE_S2C, (client, handler, buf, responseSender) -> {
			client.execute(() -> TransferCompleteClientFx.play(client));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.MODULE_ONLINE_S2C, (client, handler, buf, responseSender) -> {
			client.execute(() -> ModuleOnlineClientFx.play(client));
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			MaterializeClientFx.clientTick(client);
			PdaAppearClientFx.clientTick(client);
			DayAnnounceClientFx.clientTick();
			InfectionActiveClientFx.clientTick();
			CoordsUnlockClientFx.clientTick();
			SepticLinkClientFx.clientTick(client);
			SystemErrorClientFx.clientTick(client);
			Day3DisconnectClientFx.clientTick(client);
			ScanBeamRenderer.clientTick(client);
			ScanHumClient.clientTick(client);
			BlueprintUpdateClientFx.clientTick();
			TransferCompleteClientFx.clientTick();
			ModuleOnlineClientFx.clientTick(client);
			SepticPresenceClientFx.clientTick(client);
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
			InfectionActiveClientFx.render(graphics, client);
			SepticLinkClientFx.render(graphics, client);
			CoordsUnlockClientFx.render(graphics, client);
			SystemErrorClientFx.render(graphics, client);
			Day3DisconnectClientFx.render(graphics, client);
			ScanHud.render(graphics, client, tickDelta);
			BlueprintUpdateClientFx.render(graphics, client);
			TransferCompleteClientFx.render(graphics, client);
			ModuleOnlineClientFx.render(graphics, client);
			SepticPresenceClientFx.render(graphics, client);
		});

		WorldRenderEvents.AFTER_TRANSLUCENT.register(ScanBeamRenderer::render);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			MaterializeClientFx.cancel(client);
			PdaAppearClientFx.cancel(client);
			DayAnnounceClientFx.cancel();
			InfectionActiveClientFx.cancel();
			CoordsUnlockClientFx.cancel();
			SepticLinkClientFx.cancel(client);
			SystemErrorClientFx.cancel(client);
			Day3DisconnectClientFx.cancel(client);
			ScanHumClient.stop();
			BlueprintUpdateClientFx.cancel();
			TransferCompleteClientFx.cancel();
			ModuleOnlineClientFx.cancel();
			SepticPresenceClientFx.cancel(client);
			ProtocolClientState.reset();
			IntroClientState.setFreezeActive(false);
			pendingIntro = false;
			pendingSubjectId = null;
		});

		ClientItemScreens.OPEN_PDA = stack -> Minecraft.getInstance().setScreen(new PdaScreen(stack));

		HostProtocolMod.LOGGER.info("Host Protocol client ready");
	}
}
