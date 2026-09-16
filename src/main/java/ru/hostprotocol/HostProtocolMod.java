package ru.hostprotocol;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.sound.ModSounds;

public class HostProtocolMod implements ModInitializer {
	public static final String MOD_ID = "hostprotocol";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModSounds.register();
		ModNetworking.registerServer();

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			ServerLevel overworld = server.overworld();
			IntroWorldData data = IntroWorldData.get(overworld);
			ModNetworking.sendIntroState(player, data.getSubjectId(), data.isIntroCompleted());
		});

		ServerPlayNetworking.registerGlobalReceiver(ModNetworking.INTRO_COMPLETE_C2S, (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				IntroWorldData data = IntroWorldData.get(player.serverLevel().getServer().overworld());
				data.markIntroCompleted();
				LOGGER.info("Intro completed for world; subject={}", data.getSubjectId());
			});
		});

		LOGGER.info("Host Protocol initialized (intro MVP)");
	}
}
