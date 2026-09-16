package ru.hostprotocol;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.freeze.IntroFreeze;
import ru.hostprotocol.item.ModItems;
import ru.hostprotocol.item.PdaService;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.sound.ModSounds;
import ru.hostprotocol.world.ProtocolDayTracker;
import ru.hostprotocol.world.ProtocolTime;

public class HostProtocolMod implements ModInitializer {
	public static final String MOD_ID = "hostprotocol";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModSounds.register();
		ModItems.register();
		ModNetworking.registerServer();
		registerCommands();

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			IntroFreeze.tick(server);
			PdaService.tick(server);
			ProtocolDayTracker.tick(server);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			IntroWorldData data = IntroWorldData.get(server.overworld());
			if (!data.isIntroCompleted()) {
				IntroFreeze.begin(player);
			} else {
				PdaService.grantIfNeeded(player, data, !data.hasReceivedPda(player.getUUID()));
			}
			ModNetworking.sendIntroState(player, data.getSubjectId(), data.isIntroCompleted());
			ModNetworking.sendProtocolState(player, data);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> IntroFreeze.end(handler.player));

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

		LOGGER.info("Host Protocol initialized (intro freeze + PDA + day tracker)");
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
											PdaService.countPdas(player)
									), false);
									return day;
								}))
		));
	}
}
