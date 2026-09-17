package ru.hostprotocol.item;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.network.ModNetworking;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * One PDA per player. Never drops a second copy at the player's feet.
 * First grant plays a short client cinematic, then a single stack is placed in inventory.
 */
public final class PdaService {
	/** ~2.4s — matches {@code PdaAppearClientFx} so the hologram settles before the stack appears. */
	private static final int APPEAR_DELAY_TICKS = 48;
	private static final double STRAY_DESPAWN_RANGE = 12.0;

	private static final Map<UUID, Integer> PENDING_DELIVER_TICKS = new ConcurrentHashMap<>();

	private PdaService() {}

	public static void grantIfNeeded(ServerPlayer player, IntroWorldData data, boolean playAppearCinematic) {
		despawnStrayPdaEntities(player);

		int held = countPdas(player);
		if (held >= 1) {
			PENDING_DELIVER_TICKS.remove(player.getUUID());
			data.markPdaReceived(player.getUUID());
			data.markPdaDelivered(player.getUUID());
			stampLogs(player, data);
			return;
		}

		if (PENDING_DELIVER_TICKS.containsKey(player.getUUID())) {
			return;
		}

		if (data.hasDeliveredPda(player.getUUID())) {
			return;
		}

		if (data.hasReceivedPda(player.getUUID())) {
			deliverToInventory(player, data);
			return;
		}

		data.markPdaReceived(player.getUUID());
		if (playAppearCinematic) {
			PENDING_DELIVER_TICKS.put(player.getUUID(), APPEAR_DELAY_TICKS);
			ModNetworking.sendPdaAppear(player, data.getSubjectId());
			HostProtocolMod.LOGGER.info("PDA appear cinematic for {} (subject={})",
					player.getGameProfile().getName(), data.getSubjectId());
		} else {
			deliverToInventory(player, data);
		}
	}

	public static void tick(MinecraftServer server) {
		if (PENDING_DELIVER_TICKS.isEmpty()) {
			return;
		}
		for (UUID id : Set.copyOf(PENDING_DELIVER_TICKS.keySet())) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null) {
				PENDING_DELIVER_TICKS.remove(id);
				continue;
			}
			int left = PENDING_DELIVER_TICKS.merge(id, -1, Integer::sum);
			if (left <= 0) {
				PENDING_DELIVER_TICKS.remove(id);
				IntroWorldData data = IntroWorldData.get(player.serverLevel().getServer().overworld());
				deliverToInventory(player, data);
			}
		}
	}

	public static void stampDay2Log(ServerPlayer player) {
		forEachPda(player, PdaItem::markDay2Log);
	}

	public static void stampDay2Coords(ServerPlayer player, IntroWorldData data) {
		if (!data.isCoordsDiscovered() || !data.hasInfectionFocus()) {
			return;
		}
		data.markDay2Log(player.getUUID());
		data.markDay2CoordsLog(player.getUUID());
		forEachPda(player, stack -> PdaItem.markDay2Coords(stack, data.getFocusX(), data.getFocusY(), data.getFocusZ()));
	}

	public static void stampDay3Log(ServerPlayer player) {
		forEachPda(player, PdaItem::markDay3Log);
	}

	public static void stampSystemError(ServerPlayer player) {
		forEachPda(player, PdaItem::markSystemErrorLog);
	}

	public static void stampBlueprints(ServerPlayer player) {
		forEachPda(player, PdaItem::markBlueprints);
	}

	public static void stampLabBlueprints(ServerPlayer player) {
		forEachPda(player, PdaItem::markLabBlueprints);
	}

	public static void stampTransferred(ServerPlayer player) {
		forEachPda(player, PdaItem::markTransferred);
	}

	public static void stampBaseBlueprints(ServerPlayer player) {
		forEachPda(player, PdaItem::markBaseBlueprints);
	}

	public static void appendSensorLog(ServerPlayer player, String line) {
		forEachPda(player, stack -> PdaItem.appendSensorLog(stack, line));
	}

	public static void stampLogs(ServerPlayer player, IntroWorldData data) {
		forEachPda(player, stack -> stampOnto(stack, data, player.getUUID()));
	}

	public static void stampOnto(ItemStack stack, IntroWorldData data, UUID playerId) {
		if (data.hasDay2Log(playerId)) {
			PdaItem.markDay2Log(stack);
		}
		if (data.hasDay2CoordsLog(playerId) && data.isCoordsDiscovered() && data.hasInfectionFocus()) {
			PdaItem.markDay2Coords(stack, data.getFocusX(), data.getFocusY(), data.getFocusZ());
		}
		if (data.hasDay3Log(playerId)) {
			PdaItem.markDay3Log(stack);
		}
		if (data.hasSystemErrorLog(playerId)) {
			PdaItem.markSystemErrorLog(stack);
		}
		if (data.hasBlueprints(playerId)) {
			PdaItem.markBlueprints(stack);
		}
		if (data.hasLabBlueprints(playerId)) {
			PdaItem.markLabBlueprints(stack);
		}
		if (data.hasTransferred(playerId)) {
			PdaItem.markTransferred(stack);
		}
		if (data.hasBaseBlueprints(playerId)) {
			PdaItem.markBaseBlueprints(stack);
		}
	}

	private static void forEachPda(ServerPlayer player, Consumer<ItemStack> consumer) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(ModItems.PDA) || stack.is(ModItems.PDA_MK2)) {
				consumer.accept(stack);
			}
		}
		player.getInventory().setChanged();
	}

	public static int countPdas(ServerPlayer player) {
		int n = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(ModItems.PDA)) {
				n += stack.getCount();
			}
		}
		return n;
	}

	private static void deliverToInventory(ServerPlayer player, IntroWorldData data) {
		despawnStrayPdaEntities(player);
		if (countPdas(player) >= 1) {
			data.markPdaDelivered(player.getUUID());
			stampLogs(player, data);
			return;
		}
		if (data.hasDeliveredPda(player.getUUID())) {
			return;
		}

		ItemStack pda = PdaItem.createForSubject(data.getSubjectId());
		stampOnto(pda, data, player.getUUID());
		boolean added = player.addItem(pda);
		if (!added) {
			player.drop(pda, false);
		}
		player.getInventory().setChanged();
		data.markPdaDelivered(player.getUUID());
		HostProtocolMod.LOGGER.info("Delivered Host Protocol PDA to {} (subject={})",
				player.getGameProfile().getName(), data.getSubjectId());
	}

	/**
	 * Leftover ground copies from older builds (inventory grant + feet drop). Discard nearby
	 * {@code hostprotocol:pda} item entities so the cinematic cannot be picked up as a second stack.
	 */
	private static void despawnStrayPdaEntities(ServerPlayer player) {
		AABB box = player.getBoundingBox().inflate(STRAY_DESPAWN_RANGE);
		List<ItemEntity> strays = player.serverLevel().getEntitiesOfClass(
				ItemEntity.class,
				box,
				entity -> entity.isAlive() && entity.getItem().is(ModItems.PDA)
		);
		for (ItemEntity stray : strays) {
			stray.discard();
		}
	}
}
