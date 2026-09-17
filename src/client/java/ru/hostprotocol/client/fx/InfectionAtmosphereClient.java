package ru.hostprotocol.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import ru.hostprotocol.block.ModBlockTags;
import ru.hostprotocol.client.ProtocolClientState;
import ru.hostprotocol.horror.HorrorEventLogic;
import ru.hostprotocol.world.ProtocolTime;

/**
 * Purple night fog near the focus, rare heartbeat while standing on infected blocks.
 */
public final class InfectionAtmosphereClient {
	private static float fogBlend;
	private static int heartbeatCooldown;

	private InfectionAtmosphereClient() {}

	public static float fogAmount() {
		return fogBlend;
	}

	public static void cancel() {
		fogBlend = 0.0F;
		heartbeatCooldown = 0;
	}

	public static void clientTick(Minecraft minecraft) {
		if (heartbeatCooldown > 0) {
			heartbeatCooldown--;
		}
		if (minecraft == null || minecraft.player == null || minecraft.level == null) {
			fogBlend = 0.0F;
			return;
		}
		Player player = minecraft.player;
		Level level = minecraft.level;
		if (level.dimension() != Level.OVERWORLD) {
			fogBlend = 0.0F;
			return;
		}
		boolean onInfected = standingOnInfected(player, level);
		boolean night = ProtocolTime.isNight(level.getDayTime());
		int day = ProtocolClientState.syncedDay();
		float target = 0.0F;
		if (ProtocolClientState.isInfectionActive()) {
			if (onInfected) {
				target = night ? 0.42F : 0.18F;
			}
			if (ProtocolClientState.isCoordsDiscovered()) {
				int dx = player.getBlockX() - ProtocolClientState.focusX();
				int dy = player.getBlockY() - ProtocolClientState.focusY();
				int dz = player.getBlockZ() - ProtocolClientState.focusZ();
				if (HorrorEventLogic.nearFocus(dx, dy, dz, 28)) {
					float dist = Mth.sqrt((float) (dx * dx + dy * dy + dz * dz));
					float near = Mth.clamp(1.0F - dist / 28.0F, 0.0F, 1.0F);
					target = Math.max(target, night ? 0.22F + 0.55F * near : 0.08F + 0.18F * near);
				}
			}
		}
		fogBlend = Mth.lerp(0.12F, fogBlend, target);

		if (onInfected && day >= 2 && heartbeatCooldown <= 0) {
			RandomSource random = player.getRandom();
			if (HorrorEventLogic.shouldHeartbeat(true, random.nextInt(HorrorEventLogic.HEARTBEAT_ROLL))) {
				heartbeatCooldown = 20 * 40;
				HorrorClientFx.playHeartbeat(minecraft);
			}
		}
	}

	public static boolean standingOnInfected(Player player, Level level) {
		BlockPos feet = player.blockPosition();
		return level.getBlockState(feet).is(ModBlockTags.INFECTED)
				|| level.getBlockState(feet.below()).is(ModBlockTags.INFECTED);
	}
}
