package ru.hostprotocol.progress;

/**
 * Day-5 Septic presence (not a full boss). Spawn once the solar day index reaches 5.
 */
public final class SepticPresenceLogic {
	public static final int FIRST_DAY = 5;
	/** ~23° cone. */
	public static final double LOOK_DOT = 0.92;
	public static final double LOOK_RANGE = 40.0;
	public static final int WHAT_COOLDOWN_TICKS = 200;

	private SepticPresenceLogic() {}

	public static boolean shouldSpawn(int dayIndex, boolean entityAlive) {
		return dayIndex >= FIRST_DAY && !entityAlive;
	}

	public static boolean isLookingAt(double dot, double distance) {
		return distance > 0.4 && distance <= LOOK_RANGE && dot >= LOOK_DOT;
	}
}
