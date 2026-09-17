package ru.hostprotocol.infection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Shared cheap infection look: purple multiply tint and sparse surface motes.
 */
public final class InfectionVisuals {
	/** Multiply tint on the original block under the overlay (0xRRGGBB). */
	public static final int BLOCK_TINT = 0x6A2488;
	/** Darker multiply for overlay-less faces that still need to read as sick. */
	public static final int BLOCK_TINT_DEEP = 0x3A1038;

	private InfectionVisuals() {}

	public static int stainGrass(int grassRgb) {
		int r = (grassRgb >> 16) & 0xFF;
		int g = (grassRgb >> 8) & 0xFF;
		int b = grassRgb & 0xFF;
		int nr = clampColor((int) (r * 0.28 + 92 * 0.72));
		int ng = clampColor((int) (g * 0.22 + 16 * 0.78));
		int nb = clampColor((int) (b * 0.30 + 118 * 0.70));
		return (nr << 16) | (ng << 8) | nb;
	}

	public static int stageTintShade(int stage) {
		return stage >= 2 ? 18 : 30;
	}

	public static void animate(Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(2) != 0) {
			return;
		}
		double x = pos.getX() + 0.12 + random.nextDouble() * 0.76;
		double y = pos.getY() + 0.08 + random.nextDouble() * 0.92;
		double z = pos.getZ() + 0.12 + random.nextDouble() * 0.76;
		level.addParticle(ParticleTypes.WITCH, x, y, z, 0.0, 0.02, 0.0);
		if (random.nextBoolean()) {
			level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y + 0.15, z, 0.0, 0.04, 0.0);
		}
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.SQUID_INK, x, y + 0.2, z, 0.0, 0.01, 0.0);
		}
		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, x, y + 0.95, z, 0.0, 0.0, 0.0);
		}
		if (random.nextInt(40) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
					ru.hostprotocol.sound.ModSounds.HORROR_DRIP,
					net.minecraft.sounds.SoundSource.AMBIENT,
					0.22F, 0.65F + random.nextFloat() * 0.35F, false);
		}
	}

	private static int clampColor(int v) {
		return Math.max(0, Math.min(255, v));
	}
}
