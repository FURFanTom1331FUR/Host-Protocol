package ru.hostprotocol.progress;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.item.PdaMk2Item;
import ru.hostprotocol.item.PdaService;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.network.TitlePackets;
import ru.hostprotocol.sound.ModSounds;

/**
 * Scanner unlocks, laboratory transfer, and MK-II first-boot.
 */
public final class ProgressionService {
	private ProgressionService() {}

	public static void scanBlock(ServerPlayer player, BlockState state, BlockPos pos) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		IntroWorldData data = IntroWorldData.get(player.serverLevel().getServer().overworld());
		data.pushScan(player.getUUID(), id.toString());
		boolean iron = IronScanLogic.isIronOrePath(id.getPath());
		if (IronScanLogic.shouldUnlockLabBlueprints(iron, data.hasLabBlueprints(player.getUUID()))) {
			data.markLabBlueprints(player.getUUID());
			onLabBlueprintsUnlocked(player);
			HostProtocolMod.LOGGER.info("Lab + MK-II blueprints unlocked for {} via iron scan at {}",
					player.getGameProfile().getName(), pos);
		}
		PdaService.stampLogs(player, data);
		ModNetworking.sendProtocolState(player, data);
	}

	/** First iron-ore scan: VO, titles, lab recipe book, protocol chat. Assumes the flag is already marked. */
	public static void onLabBlueprintsUnlocked(ServerPlayer player) {
		PdaService.stampLabBlueprints(player);
		ProgressionRecipes.unlockLab(player);
		TitlePackets.send(
				player,
				Component.translatable("hostprotocol.scan.iron.title").withStyle(ChatFormatting.AQUA),
				Component.translatable("hostprotocol.scan.iron.subtitle").withStyle(ChatFormatting.WHITE),
				8, 70, 16
		);
		player.sendSystemMessage(Component.translatable("hostprotocol.scan.iron.chat")
				.withStyle(ChatFormatting.DARK_AQUA));
		ru.hostprotocol.network.ProtocolBroadcast.chat(player,
				ru.hostprotocol.network.ProtocolBroadcast.boldPurple("hostprotocol.scan.iron.unlock.chat"));
		ru.hostprotocol.network.ProtocolBroadcast.chat(player,
				ru.hostprotocol.network.ProtocolBroadcast.boldGold("hostprotocol.scan.iron.unlock.chat2"));
		player.displayClientMessage(Component.translatable("hostprotocol.scan.iron.unlock.bar")
				.withStyle(ChatFormatting.GOLD), true);
		ModNetworking.sendBlueprintUpdate(player);
		IntroWorldData data = IntroWorldData.get(player.serverLevel().getServer().overworld());
		ModNetworking.sendProtocolState(player, data);
	}

	public static void onTransferComplete(ServerPlayer player, ItemStack result) {
		IntroWorldData data = IntroWorldData.get(player.serverLevel().getServer().overworld());
		boolean first = data.markTransferred(player.getUUID());
		data.markBaseBlueprints(player.getUUID());
		if (result.isEmpty()) {
			// still stamp even if stack moved
		} else if (result.getItem() instanceof PdaMk2Item) {
			PdaMk2Item.markActivated(result);
			PdaMk2Item.markBlueprints(result);
			result.getOrCreateTag().putBoolean(ru.hostprotocol.item.PdaItem.TAG_LAB_BLUEPRINTS, true);
			result.getOrCreateTag().putBoolean(ru.hostprotocol.item.PdaItem.TAG_TRANSFERRED, true);
			result.getOrCreateTag().putBoolean(ru.hostprotocol.item.PdaItem.TAG_BASE_BLUEPRINTS, true);
		}
		PdaService.stampTransferred(player);
		PdaService.stampBaseBlueprints(player);
		PdaService.stampLogs(player, data);
		ProgressionRecipes.unlockBase(player);
		if (first) {
			TitlePackets.send(
					player,
					Component.translatable("hostprotocol.transfer.title").withStyle(ChatFormatting.AQUA),
					Component.translatable("hostprotocol.transfer.subtitle").withStyle(ChatFormatting.WHITE),
					8, 50, 12
			);
			player.sendSystemMessage(Component.translatable("hostprotocol.transfer.chat")
					.withStyle(ChatFormatting.DARK_AQUA));
		}
		ModNetworking.sendTransferComplete(player);
		ModNetworking.sendProtocolState(player, data);
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
				24, 0.5, 0.7, 0.5, 0.04);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
				16, 0.4, 0.5, 0.4, 0.08);
		level.playSound(null, player.blockPosition(), ModSounds.VOICE_TRANSFER_COMPLETE,
				SoundSource.PLAYERS, 1.0F, 1.0F);
		HostProtocolMod.LOGGER.info("PDA transfer complete for {} (first={})", player.getGameProfile().getName(), first);
	}

	public static void onMk2Boot(ServerPlayer player, ItemStack stack) {
		IntroWorldData data = IntroWorldData.get(player.serverLevel().getServer().overworld());
		boolean first = data.markMk2Booted(player.getUUID());
		PdaMk2Item.markBooted(stack);
		PdaService.stampLogs(player, data);
		if (first) {
			TitlePackets.send(
					player,
					Component.translatable("hostprotocol.module.online.title").withStyle(ChatFormatting.AQUA),
					Component.translatable("hostprotocol.module.online.subtitle").withStyle(ChatFormatting.WHITE),
					6, 40, 12
			);
		}
		ModNetworking.sendModuleOnline(player);
		ModNetworking.sendProtocolState(player, data);
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.05, player.getZ(),
				28, 0.6, 0.15, 0.6, 0.01);
		level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 0.2, player.getZ(),
				18, 0.4, 0.05, 0.4, 0.02);
		level.playSound(null, player.blockPosition(), ModSounds.VOICE_MODULE_ONLINE,
				SoundSource.PLAYERS, 1.0F, 1.0F);
		HostProtocolMod.LOGGER.info("MK-II assistant online for {}", player.getGameProfile().getName());
	}

	public static void grantKnownRecipes(ServerPlayer player, IntroWorldData data) {
		if (data.hasBlueprints(player.getUUID())) {
			ProgressionRecipes.unlockScanner(player);
		}
		if (data.hasLabBlueprints(player.getUUID())) {
			ProgressionRecipes.unlockLab(player);
		}
		if (data.hasBaseBlueprints(player.getUUID()) || data.hasTransferred(player.getUUID())) {
			ProgressionRecipes.unlockBase(player);
		}
	}
}
