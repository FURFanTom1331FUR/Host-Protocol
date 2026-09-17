package ru.hostprotocol;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hostprotocol.block.ModBlockTags;
import ru.hostprotocol.block.ModBlocks;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.freeze.IntroFreeze;
import ru.hostprotocol.infection.Day2DebugSequence;
import ru.hostprotocol.infection.Day3DisconnectController;
import ru.hostprotocol.infection.InfectionTicker;
import ru.hostprotocol.infection.MetaBreachController;
import ru.hostprotocol.infection.SepticLinkController;
import ru.hostprotocol.item.ModItems;
import ru.hostprotocol.item.PdaService;
import ru.hostprotocol.menu.ModMenus;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.scan.ScanService;
import ru.hostprotocol.sound.ModSounds;
import ru.hostprotocol.world.ProtocolDayTracker;
import ru.hostprotocol.world.ProtocolTime;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HostProtocolMod implements ModInitializer {
	public static final String MOD_ID = "hostprotocol";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Map<UUID, Long> UNBREAKABLE_NOTICE_AT = new ConcurrentHashMap<>();

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModSounds.register();
		ModBlocks.register();
		ModItems.register();
		ModMenus.register();
		ModNetworking.registerServer();
		registerCommands();
		registerInfectionGuards();
		registerSleepIntercept();

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			IntroFreeze.tick(server);
			PdaService.tick(server);
			ProtocolDayTracker.tick(server);
			Day2DebugSequence.tick(server);
			InfectionTicker.tick(server);
			SepticLinkController.tick(server);
			Day3DisconnectController.tick(server);
			MetaBreachController.tryArm(server, IntroWorldData.get(server.overworld()));
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			IntroWorldData data = IntroWorldData.get(server.overworld());
			if (!data.isIntroCompleted()) {
				IntroFreeze.begin(player);
			} else {
				PdaService.grantIfNeeded(player, data, !data.hasReceivedPda(player.getUUID()));
			}
			if (data.isInfectionActive()) {
				InfectionTicker.keepFocusLoaded(server.overworld(), data);
			}
			ModNetworking.sendIntroState(player, data.getSubjectId(), data.isIntroCompleted());
			ModNetworking.sendProtocolState(player, data);
			MetaBreachController.onPlayerJoin(player, data);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			IntroFreeze.end(handler.player);
			Day3DisconnectController.onDisconnect(handler.player, IntroWorldData.get(server.overworld()));
			ScanService.clear(handler.player);
		});

		ServerPlayNetworking.registerGlobalReceiver(ModNetworking.INTRO_COMPLETE_C2S, (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				IntroWorldData data = IntroWorldData.get(player.serverLevel().getServer().overworld());
				data.markIntroCompleted();
				IntroFreeze.end(player);
				ProtocolDayTracker.onIntroCompleted(player, data);
				PdaService.grantIfNeeded(player, data, true);
				LOGGER.info("Intro completed for world; subject={}", data.getSubjectId());
			});
		});

		LOGGER.info("Host Protocol initialized (Day-2 dawn infection, auto coords/septic, Day-3 kick)");
	}

	private static void registerInfectionGuards() {
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (state.is(ModBlockTags.INFECTED)) {
				noticeUnbreakable(player, world.getGameTime());
				return false;
			}
			return true;
		});
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (!world.getBlockState(pos).is(ModBlockTags.INFECTED)) {
				return InteractionResult.PASS;
			}
			noticeUnbreakable(player, world.getGameTime());
			return InteractionResult.FAIL;
		});
	}

	private static void noticeUnbreakable(Player player, long gameTime) {
		if (player.level().isClientSide()) {
			return;
		}
		Long last = UNBREAKABLE_NOTICE_AT.get(player.getUUID());
		if (last != null && gameTime - last < 20L) {
			return;
		}
		UNBREAKABLE_NOTICE_AT.put(player.getUUID(), gameTime);
		player.displayClientMessage(Component.translatable("hostprotocol.infection.unbreakable"), true);
	}

	private static void registerSleepIntercept() {
		EntitySleepEvents.ALLOW_SLEEPING.register((player, sleepingPos) -> {
			if (SepticLinkController.tryInterceptSleep(player)) {
				return Player.BedSleepingProblem.OTHER_PROBLEM;
			}
			return null;
		});
		EntitySleepEvents.ALLOW_RESETTING_TIME.register(player ->
				!SepticLinkController.shouldBlockTimeReset(player));
	}

	private static void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("hostprotocol")
						.then(Commands.literal("replayintro")
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									IntroWorldData data = IntroWorldData.get(player.serverLevel().getServer().overworld());
									data.resetIntroCompleted();
									IntroFreeze.begin(player);
									ModNetworking.sendIntroState(player, data.getSubjectId(), false);
									ModNetworking.sendProtocolState(player, data);
									ctx.getSource().sendSuccess(() -> Component.translatable("hostprotocol.command.replayintro"), true);
									LOGGER.info("Intro replay requested by {}; subject={}", player.getGameProfile().getName(), data.getSubjectId());
									return 1;
								}))
						.then(Commands.literal("day2")
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									int result = Day2DebugSequence.start(player);
									ctx.getSource().sendSuccess(() -> Component.translatable("hostprotocol.command.day2"), true);
									LOGGER.info("[Host Protocol] /hostprotocol day2 by {}", player.getGameProfile().getName());
									return result;
								}))
						.then(Commands.literal("forcecoords")
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									var server = player.serverLevel().getServer();
									IntroWorldData data = IntroWorldData.get(server.overworld());
									InfectionTicker.forceActivate(server.overworld(), data);
									InfectionTicker.unlockCoords(server, data, true);
									ctx.getSource().sendSuccess(() -> Component.translatable("hostprotocol.command.forcecoords",
											data.getFocusX(), data.getFocusY(), data.getFocusZ()), true);
									return 1;
								}))
						.then(Commands.literal("forceseptic")
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									SepticLinkController.forcePlay(player);
									ctx.getSource().sendSuccess(() -> Component.translatable("hostprotocol.command.forceseptic"), true);
									return 1;
								}))
						.then(Commands.literal("status")
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									IntroWorldData data = IntroWorldData.get(player.serverLevel().getServer().overworld());
									int day = ProtocolTime.dayIndex(player.serverLevel().getServer().overworld().getDayTime());
									ctx.getSource().sendSuccess(() -> Component.translatable(
											"hostprotocol.command.status",
											day,
											data.getSubjectId(),
											data.hasReceivedPda(player.getUUID()),
											data.hasDeliveredPda(player.getUUID()),
											data.hasDay2Log(player.getUUID()),
											data.hasDay3Log(player.getUUID()),
											PdaService.countPdas(player)
									), false);
									String focus = data.hasInfectionFocus()
											? data.getFocusX() + " " + data.getFocusY() + " " + data.getFocusZ()
											: "—";
									ctx.getSource().sendSuccess(() -> Component.translatable(
											"hostprotocol.command.status.infection",
											data.isInfectionActive(),
											focus,
											data.isCoordsDiscovered(),
											data.hasSepticLinkAttempted()
									), false);
									long gameTime = player.serverLevel().getServer().overworld().getGameTime();
									long coordsIn = data.isCoordsDiscovered() || data.getCoordsUnlockAtGameTime() <= 0L
											? 0L
											: Math.max(0L, data.getCoordsUnlockAtGameTime() - gameTime);
									long septicIn = data.hasSepticLinkAttempted() || data.getSepticAutoAtGameTime() <= 0L
											? 0L
											: Math.max(0L, data.getSepticAutoAtGameTime() - gameTime);
									ctx.getSource().sendSuccess(() -> Component.translatable(
											"hostprotocol.command.status.breach",
											data.isProtocolBreached(),
											data.hasSystemErrorLog(player.getUUID()),
											data.hasBlueprints(player.getUUID()),
											coordsIn,
											septicIn
									), false);
									ctx.getSource().sendSuccess(() -> Component.translatable(
											"hostprotocol.command.status.lab",
											data.hasLabBlueprints(player.getUUID()),
											data.scannedOreCount(player.getUUID())
									), false);
									return day;
								}))
		));
	}
}
