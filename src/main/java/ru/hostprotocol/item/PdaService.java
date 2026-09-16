package ru.hostprotocol.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.data.IntroWorldData;

/**
 * One-time PDA grant per player, plus a single physical copy at spawn on first world completion.
 */
public final class PdaService {
	private PdaService() {}

	public static void grantIfNeeded(ServerPlayer player, IntroWorldData data, boolean allowWorldDrop) {
		ItemStack pda = PdaItem.createForSubject(data.getSubjectId());

		if (!data.hasReceivedPda(player.getUUID())) {
			ItemStack toGive = pda.copy();
			boolean added = player.addItem(toGive);
			if (!added) {
				player.drop(toGive, false);
			}
			player.getInventory().setChanged();
			data.markPdaReceived(player.getUUID());
			HostProtocolMod.LOGGER.info("Granted Host Protocol PDA to {} (subject={})",
					player.getGameProfile().getName(), data.getSubjectId());
		}

		if (allowWorldDrop && !data.isPdaPlacedAtSpawn()) {
			ItemEntity dropped = new ItemEntity(
					player.serverLevel(),
					player.getX(),
					player.getY() + 0.25,
					player.getZ(),
					pda.copy()
			);
			dropped.setDefaultPickUpDelay();
			player.serverLevel().addFreshEntity(dropped);
			data.markPdaPlacedAtSpawn();
			HostProtocolMod.LOGGER.info("Placed spawn PDA near {}", player.getGameProfile().getName());
		}
	}
}
