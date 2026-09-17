package ru.hostprotocol.progress;

/**
 * Day-5 Septic presence (not a full boss). Spawn once the solar day index reaches 5.
 */
public final class SepticPresenceLogic {
	public static final int FIRST_DAY = 5;
	/** ~44° cone: body still counts, glancing past a wall does not. */
	public static final double LOOK_DOT = 0.72;
	public static final double LOOK_RANGE = 64.0;
	public static final double LOOK_BOX_INFLATE = 0.45;
	/** 30s between look-at chat bursts so a stare cannot spam. */
	public static final int LOOK_CHAT_COOLDOWN_TICKS = 20 * 30;
	/** Brief punch (FOV / shake / flicker) on the rising edge of a look. */
	public static final int LOOK_IMPACT_TICKS = 24;
	/** HUD fragment lifetime after a look-at chat fire. */
	public static final int LOOK_HUD_TICKS = 70;
	@Deprecated
	public static final int WHAT_COOLDOWN_TICKS = LOOK_CHAT_COOLDOWN_TICKS;

	private SepticPresenceLogic() {}

	public static boolean shouldSpawn(int dayIndex, boolean entityAlive) {
		return dayIndex >= FIRST_DAY && !entityAlive;
	}

	public static boolean isLookingAt(double dot, double distance) {
		return distance > 0.2 && distance <= LOOK_RANGE && dot >= LOOK_DOT;
	}

	/**
	 * Look-at slam needs a living presence (not a hallway stalker), a real line of sight,
	 * and the cone/range gate. Wall-through and peripheral false positives were slapping the HUD.
	 */
	public static boolean shouldLookOverlay(boolean presenceSeptic, boolean lineOfSight, double dot, double distance) {
		return presenceSeptic && lineOfSight && isLookingAt(dot, distance);
	}

	/**
	 * Fire the look-at chat once per gaze: rising edge, and only if the encounter cooldown is idle.
	 */
	public static boolean shouldAnnounceLook(boolean lookingNow, boolean wasLooking, int cooldownTicks) {
		return lookingNow && !wasLooking && cooldownTicks <= 0;
	}
}
