package ru.hostprotocol.scan;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.item.ModItems;
import ru.hostprotocol.item.PdaService;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.network.ProtocolBroadcast;
import ru.hostprotocol.sound.ModSounds;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side hold-to-scan: lock a target, cancel on look-away / move / release, complete once.
 */
public final class ScanService {
	private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

	private ScanService() {}

	public static boolean isScanning(ServerPlayer player) {
		return SESSIONS.containsKey(player.getUUID());
	}

	public static void tryBegin(ServerPlayer player, ScanHit hit) {
		if (hit == null || hit.isEmpty()) {
			fail(player, "hostprotocol.scan.fail.none");
			return;
		}
		IntroWorldData data = IntroWorldData.get(player.serverLevel().getServer().overworld());
		if (hit.isOre() && data.isOreScanned(player.getUUID(), hit.blockPos)) {
			fail(player, "hostprotocol.scan.ore.already");
			return;
		}
		SESSIONS.put(player.getUUID(), new Session(hit, player.position(), ScanTiming.durationTicks(hit.isOre())));
	}

	public static boolean canBegin(ServerPlayer player, ScanHit hit) {
		if (hit == null || hit.isEmpty()) {
			return false;
		}
		if (!hit.isOre()) {
			return true;
		}
		IntroWorldData data = IntroWorldData.get(player.serverLevel().getServer().overworld());
		return !data.isOreScanned(player.getUUID(), hit.blockPos);
	}

	public static void onUseTick(ServerPlayer player) {
		ItemStack used = player.getUseItem();
		if (!used.is(ModItems.SCANNER)) {
			clear(player);
			return;
		}
		Session session = SESSIONS.get(player.getUUID());
		ScanHit live = ScanRay.pick(player);
		if (session == null) {
			if (!canBegin(player, live)) {
				if (live.isOre()) {
					fail(player, "hostprotocol.scan.ore.already");
				} else {
					fail(player, "hostprotocol.scan.fail.none");
				}
				player.stopUsingItem();
				return;
			}
			tryBegin(player, live);
			session = SESSIONS.get(player.getUUID());
			if (session == null) {
				player.stopUsingItem();
				return;
			}
		}
		if (player.position().distanceToSqr(session.startPos) > ScanTiming.MAX_MOVE * ScanTiming.MAX_MOVE) {
			cancel(player, true);
			player.stopUsingItem();
			return;
		}
		if (!session.target.sameTarget(live)) {
			cancel(player, true);
			player.stopUsingItem();
			return;
		}
		if (session.target.isMob()) {
			LivingEntity entity = session.target.entity;
			if (entity == null || !entity.isAlive()) {
				cancel(player, true);
				player.stopUsingItem();
				return;
			}
		} else if (session.target.isOre()) {
			BlockPos pos = session.target.blockPos;
			if (pos == null || player.level().getBlockState(pos).isAir()) {
				cancel(player, true);
				player.stopUsingItem();
				return;
			}
		}
		session.ticks++;
		if (session.ticks >= session.required) {
			complete(player, session);
		}
	}

	public static void onReleased(ServerPlayer player) {
		Session session = SESSIONS.get(player.getUUID());
		if (session == null) {
			return;
		}
		if (session.ticks >= session.required) {
			SESSIONS.remove(player.getUUID());
			return;
		}
		cancel(player, true);
	}

	public static void clear(ServerPlayer player) {
		SESSIONS.remove(player.getUUID());
	}

	private static void complete(ServerPlayer player, Session session) {
		SESSIONS.remove(player.getUUID());
		ScanHit hit = session.target;
		IntroWorldData data = IntroWorldData.get(player.serverLevel().getServer().overworld());
		play(player, true);
		if (hit.isOre()) {
			BlockPos pos = hit.blockPos;
			if (!data.markOreScanned(player.getUUID(), pos)) {
				player.displayClientMessage(Component.translatable("hostprotocol.scan.ore.already")
						.withStyle(ChatFormatting.DARK_PURPLE), true);
				player.stopUsingItem();
				return;
			}
			Component oreName = hit.block != null
					? hit.block.getName()
					: player.level().getBlockState(pos).getBlock().getName();
			player.displayClientMessage(Component.translatable("hostprotocol.scan.ore.success", oreName)
					.withStyle(ChatFormatting.LIGHT_PURPLE), true);
			PdaService.appendSensorLog(player, Component.translatable("hostprotocol.scan.sensor.ore", oreName).getString());
			if (isIronOre(player, pos, hit.block) && data.markLabBlueprints(player.getUUID())) {
				PdaService.stampLabBlueprints(player);
				ProtocolBroadcast.chat(player, ProtocolBroadcast.boldPurple("hostprotocol.scan.iron.unlock.chat"));
				ProtocolBroadcast.chat(player, ProtocolBroadcast.boldGold("hostprotocol.scan.iron.unlock.chat2"));
				player.displayClientMessage(Component.translatable("hostprotocol.scan.iron.unlock.bar")
						.withStyle(ChatFormatting.GOLD), true);
				ModNetworking.sendProtocolState(player, data);
			}
		} else if (hit.isMob() && hit.entity != null) {
			Component name = hit.entity.getDisplayName();
			player.displayClientMessage(Component.translatable("hostprotocol.scan.mob.success", name)
					.withStyle(ChatFormatting.AQUA), true);
			ProtocolBroadcast.chat(player, Component.translatable("hostprotocol.scan.mob.chat", name)
					.withStyle(ChatFormatting.DARK_PURPLE));
			PdaService.appendSensorLog(player, Component.translatable("hostprotocol.scan.sensor.mob", name).getString());
		}
		player.stopUsingItem();
	}

	private static boolean isIronOre(ServerPlayer player, BlockPos pos, Block block) {
		if (player.level().getBlockState(pos).is(BlockTags.IRON_ORES)) {
			return true;
		}
		return block != null && block.defaultBlockState().is(BlockTags.IRON_ORES);
	}

	private static void cancel(ServerPlayer player, boolean message) {
		SESSIONS.remove(player.getUUID());
		if (message) {
			fail(player, "hostprotocol.scan.fail.cancel");
		}
	}

	private static void fail(ServerPlayer player, String key) {
		player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE), true);
		play(player, false);
	}

	private static void play(ServerPlayer player, boolean complete) {
		player.playNotifySound(
				complete ? ModSounds.SCAN_COMPLETE : ModSounds.SCAN_FAIL,
				SoundSource.PLAYERS,
				0.7F,
				complete ? 1.15F : 0.75F);
	}

	private static final class Session {
		final ScanHit target;
		final Vec3 startPos;
		final int required;
		int ticks;

		Session(ScanHit target, Vec3 startPos, int required) {
			this.target = target;
			this.startPos = startPos;
			this.required = required;
		}
	}
}
