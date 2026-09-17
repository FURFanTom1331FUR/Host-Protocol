package ru.hostprotocol.horror;

/**
 * Pure gates for Host Protocol scare beats. Fewer, sharper events; Day 3 kick stays the only
 * hard meta of that day.
 */
public final class HorrorEventLogic {
	public static final String FAKE_PLAYER = "Spryzen";

	public static final int FAKE_JOIN_MIN_DAY = 3;
	public static final int STALKER_MIN_DAY = 4;
	public static final int SCREAMER_MIN_DAY = 5;
	public static final int PDA_BETRAY_MIN_DAY = 6;

	/** 0.5s–1.5s glimpse. */
	public static final int STALKER_LIFE_MIN = 10;
	public static final int STALKER_LIFE_MAX = 30;
	public static final int STALKER_COOLDOWN_TICKS = 20 * 60 * 5;
	public static final int STALKER_FIRST_DELAY_TICKS = 20 * 18;

	public static final int SCREAMER_COOLDOWN_TICKS = 20 * 50;
	public static final int SECOND_STARE_TICKS = 55;
	public static final int SILENCE_TICKS = 8;
	public static final int SCREAMER_DURATION_TICKS = 18;
	public static final int STATIC_DURATION_TICKS = 14;
	public static final int TOAST_DURATION_TICKS = 70;
	public static final int FAKE_LEAVE_DELAY_TICKS = 8;
	public static final int HEARTBEAT_DURATION_TICKS = 16;
	/** 1-in-N per second while standing on infected matter. */
	public static final int HEARTBEAT_ROLL = 48;
	public static final int FOCUS_NIGHT_RANGE = 14;

	public static final String FLAG_FAKE_JOIN = "fakeJoin";
	public static final String FLAG_FAKE_TOAST = "fakeToast";
	public static final String FLAG_STATIC = "static";
	public static final String FLAG_FOCUS_SCREAMER = "focusScreamer";
	public static final String FLAG_PDA_BETRAY = "pdaBetray";
	public static final String TIME_LAST_STALKER = "lastStalker";
	public static final String TIME_LAST_SCREAMER = "lastScreamer";
	public static final String TIME_DAY4_ENTER = "day4Enter";
	public static final String TIME_SOCIAL = "socialAt";
	public static final int TOAST_DELAY_TICKS = 20 * 28;

	private HorrorEventLogic() {}

	public static boolean allowed(int day, HorrorKind kind) {
		return switch (kind) {
			case FAKE_JOIN, FAKE_TOAST, STATIC_FLASH -> day >= FAKE_JOIN_MIN_DAY;
			case STALKER -> day >= STALKER_MIN_DAY;
			case SCREAMER -> day >= SCREAMER_MIN_DAY;
			case HEARTBEAT -> day >= 2;
			case PDA_BETRAY -> day >= PDA_BETRAY_MIN_DAY;
		};
	}

	/**
	 * Fake join/leave and toast wait until the Day-3 kick already happened (or it is Day 4+).
	 * The kick is the one hard meta beat of Day 3.
	 */
	public static boolean canSocialGlitch(int day, boolean day3KickDone, boolean freeze, boolean introDone) {
		if (freeze || !introDone || day < FAKE_JOIN_MIN_DAY) {
			return false;
		}
		if (day == FAKE_JOIN_MIN_DAY) {
			return day3KickDone;
		}
		return true;
	}

	public static boolean canStalker(int day, boolean introDone, boolean freeze, boolean day3Busy) {
		return day >= STALKER_MIN_DAY && introDone && !freeze && !day3Busy;
	}

	public static boolean stalkerCooldownReady(long gameTime, long lastStalkerAt, boolean firstOnDay4) {
		if (lastStalkerAt <= 0L) {
			return firstOnDay4;
		}
		return gameTime - lastStalkerAt >= STALKER_COOLDOWN_TICKS;
	}

	public static boolean firstStalkerReady(long gameTime, long day4EnterAt) {
		if (day4EnterAt <= 0L) {
			return false;
		}
		return gameTime - day4EnterAt >= STALKER_FIRST_DELAY_TICKS;
	}

	public static int stalkerLife(long seed, long gameTime) {
		int span = STALKER_LIFE_MAX - STALKER_LIFE_MIN + 1;
		return STALKER_LIFE_MIN + (int) Math.floorMod(seed ^ gameTime ^ 0x53544C4BL, span);
	}

	public static boolean shouldLookScreamer(int day, boolean lookingNow, boolean wasLooking, int cooldownTicks) {
		return day >= SCREAMER_MIN_DAY && lookingNow && !wasLooking && cooldownTicks <= 0;
	}

	public static boolean shouldSecondStare(
			int day,
			boolean looking,
			int continuousLookTicks,
			int cooldownTicks,
			boolean secondUsed
	) {
		return day >= SCREAMER_MIN_DAY
				&& looking
				&& !secondUsed
				&& continuousLookTicks >= SECOND_STARE_TICKS
				&& cooldownTicks <= 0;
	}

	public static boolean shouldFocusNightScreamer(int day, boolean night, boolean nearFocus, boolean alreadyFired) {
		return day >= SCREAMER_MIN_DAY && night && nearFocus && !alreadyFired;
	}

	public static boolean nearFocus(int dx, int dy, int dz, int range) {
		long r = range;
		return (long) dx * dx + (long) dy * dy + (long) dz * dz <= r * r;
	}

	public static boolean shouldHeartbeat(boolean onInfected, int roll) {
		return onInfected && roll == 0;
	}

	public static boolean shouldPdaBetray(int day, boolean alreadyFlashedThisOpen) {
		return day >= PDA_BETRAY_MIN_DAY && !alreadyFlashedThisOpen;
	}

	public static boolean screamerCooldownReady(long gameTime, long lastAt) {
		if (lastAt <= 0L) {
			return true;
		}
		return gameTime - lastAt >= SCREAMER_COOLDOWN_TICKS;
	}
}
