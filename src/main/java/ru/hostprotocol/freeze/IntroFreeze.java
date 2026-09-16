package ru.hostprotocol.freeze;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import ru.hostprotocol.HostProtocolMod;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side intro freeze. {@link net.minecraft.client.gui.screens.Screen#isPauseScreen()} only pauses
 * an unpublished integrated server; this gate also covers LAN / dedicated and the ticks before the overlay opens.
 * Disconnect or a missing player always clears the freeze so a dropped client cannot softlock the world.
 */
public final class IntroFreeze {
	private static final Set<UUID> FROZEN = ConcurrentHashMap.newKeySet();
	private static final Map<UUID, FrozenPose> POSES = new ConcurrentHashMap<>();

	private IntroFreeze() {}

	public static void begin(ServerPlayer player) {
		UUID id = player.getUUID();
		boolean first = FROZEN.add(id);
		POSES.put(id, new FrozenPose(
				player.getX(),
				player.getY(),
				player.getZ(),
				player.getYRot(),
				player.getXRot(),
				player.isInvulnerable()
		));
		hold(player, POSES.get(id));
		if (first) {
			HostProtocolMod.LOGGER.info("Intro freeze enabled for {}", player.getGameProfile().getName());
		}
	}

	public static void end(ServerPlayer player) {
		UUID id = player.getUUID();
		if (!FROZEN.remove(id)) {
			POSES.remove(id);
			return;
		}
		FrozenPose pose = POSES.remove(id);
		if (pose != null) {
			player.setInvulnerable(pose.invulnerable);
		} else {
			player.setInvulnerable(false);
		}
		HostProtocolMod.LOGGER.info("Intro freeze released for {}", player.getGameProfile().getName());
	}

	public static boolean isActive() {
		return !FROZEN.isEmpty();
	}

	public static boolean isFrozen(UUID playerId) {
		return FROZEN.contains(playerId);
	}

	/** Re-snap frozen players and drop entries for vanished connections. */
	public static void tick(MinecraftServer server) {
		if (FROZEN.isEmpty()) {
			return;
		}
		for (UUID id : Set.copyOf(FROZEN)) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null) {
				FROZEN.remove(id);
				POSES.remove(id);
				HostProtocolMod.LOGGER.info("Intro freeze cleared for disconnected player {}", id);
				continue;
			}
			hold(player, POSES.get(id));
		}
	}

	private static void hold(ServerPlayer player, FrozenPose pose) {
		if (pose != null) {
			player.teleportTo(pose.x, pose.y, pose.z);
			player.setYRot(pose.yRot);
			player.setXRot(pose.xRot);
			player.setYHeadRot(pose.yRot);
		}
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0.0F;
		player.setInvulnerable(true);
		player.hurtMarked = true;
	}

	private record FrozenPose(double x, double y, double z, float yRot, float xRot, boolean invulnerable) {}
}
