package ru.hostprotocol.progress;

/**
 * Minimal tagged infection near the Day-2 focus. Stage 2 after a long stay in radius.
 */
public final class InfectedMobLogic {
	/** Extra blocks beyond the overlay radius. */
	public static final int RADIUS_PADDING = 6;
	/** ~10 minutes at 20 tps, matching the design brief. */
	public static final int STAGE2_TICKS = 20 * 60 * 10;
	public static final int MAX_SCAN_RANGE = 48;

	private InfectedMobLogic() {}

	public static int affectRadius(int infectionRadius) {
		return Math.min(MAX_SCAN_RANGE, Math.max(1, infectionRadius) + RADIUS_PADDING);
	}

	public static boolean inRadius(int dx, int dy, int dz, int radius) {
		long r = radius;
		return (long) dx * dx + (long) dy * dy + (long) dz * dz <= r * r;
	}

	public static int stageForTicks(int infectedTicks) {
		if (infectedTicks <= 0) {
			return 0;
		}
		return infectedTicks >= STAGE2_TICKS ? 2 : 1;
	}

	public static boolean shouldDropFlesh(boolean infected, boolean player) {
		return infected && !player;
	}
}
