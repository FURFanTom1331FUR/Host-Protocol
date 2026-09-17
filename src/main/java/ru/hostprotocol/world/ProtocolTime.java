package ru.hostprotocol.world;

/**
 * Minecraft / Host Protocol day indexing.
 *
 * <p>Vanilla solar day is {@link #TICKS_PER_DAY} ticks. {@code Level#getDayTime()} is the
 * clock the sun follows (changed by {@code /time}, sleep, etc.). It is <em>not</em> the same
 * as {@code getGameTime()} (ticks since world creation).
 *
 * <p>Dawn of the first day is day-time {@code 0}. Day 1 is the first day after spawn/intro:
 * {@code 0 <= dayTime < 24000}. When the sun rises into the next cycle
 * ({@code dayTime} reaches {@code 24000}, {@code 48000}, …) the index becomes 2, 3, …
 *
 * <pre>
 *   dayIndex = floor(dayTime / 24000) + 1
 * </pre>
 *
 * <h2>Day 2 infection trigger</h2>
 *
 * Infection focus activates as soon as {@code dayIndex >= 2} (dawn of Day 2), not only at night.
 * Night of Day 2 remains an extra catch-up trigger. {@code /time add 24000} from Day 1 is enough.
 *
 * <p>Coordinate lock uses <strong>gameTime</strong> (~20–30 real seconds at 20 tps). If the
 * world is loaded already past that delay, coords unlock immediately. Auto-Septic fires
 * ~5–10s of gameTime after coords (sleep is a backup, not the only path).
 *
 * <p>Day 3: if the player is still in the world, a 10s stay timer leads to a hard disconnect.
 *
 * <p>Testing: {@code /hostprotocol day2} completes intro, jumps to Day 2, and force-plays
 * infection → coords → Septic in one command.
 */
public final class ProtocolTime {
	public static final long TICKS_PER_DAY = 24000L;
	/** Sunset / start of night in the solar cycle. Vanilla {@code /time set night} uses this value. */
	public static final long NIGHT_START = 13000L;
	/** Morning: sun is up enough that vanilla treats the cycle as day again. */
	public static final long DAY_START = 23000L;
	/** 20 ticks = 1 second of real/game time while the server is ticking. */
	public static final int TICKS_PER_SECOND = 20;
	/** Minimum real-time wait after focus before coords unlock (20s). */
	public static final long COORDS_LOCK_GAME_TICKS_MIN = TICKS_PER_SECOND * 20L;
	/** Maximum real-time wait after focus before coords unlock (30s). */
	public static final long COORDS_LOCK_GAME_TICKS_MAX = TICKS_PER_SECOND * 30L;
	/** Kept for spread-radius catch-up; not the coords gate. */
	public static final long COORDS_LOCK_DAYTIME_FALLBACK = COORDS_LOCK_GAME_TICKS_MAX;
	@Deprecated
	public static final long COORDS_LOCK_DELAY = COORDS_LOCK_DAYTIME_FALLBACK;
	/** Auto Septic cinematic 5s after coords if the player never slept. */
	public static final long SEPTIC_AUTO_GAME_TICKS_MIN = TICKS_PER_SECOND * 5L;
	/** Auto Septic cinematic 10s after coords if the player never slept. */
	public static final long SEPTIC_AUTO_GAME_TICKS_MAX = TICKS_PER_SECOND * 10L;
	/** Day-3 stay-in-world timer before the disconnect beat. */
	public static final long DAY3_DISCONNECT_DELAY_TICKS = TICKS_PER_SECOND * 10L;
	/** Client FX / VO length after the 10s stay, before the kick. */
	public static final int DAY3_KICK_FX_TICKS = TICKS_PER_SECOND * 5;

	private ProtocolTime() {}

	public static int dayIndex(long dayTime) {
		if (dayTime < 0L) {
			dayTime = 0L;
		}
		return (int) (dayTime / TICKS_PER_DAY) + 1;
	}

	public static long timeOfDay(long dayTime) {
		if (dayTime < 0L) {
			dayTime = 0L;
		}
		return dayTime % TICKS_PER_DAY;
	}

	/** Night window: {@code [13000, 23000)} in the 24000-tick solar cycle. */
	public static boolean isNight(long dayTime) {
		long t = timeOfDay(dayTime);
		return t >= NIGHT_START && t < DAY_START;
	}

	/**
	 * First night of Day 2: {@code dayIndex == 2} and {@link #isNight(long)}.
	 * Absolute day-time range: {@code [37000, 47000)}. Kept as a catch-up trigger.
	 */
	public static boolean isDay2Night(long dayTime) {
		return dayIndex(dayTime) == 2 && isNight(dayTime);
	}

	/** Day 2 has begun (or later). Infection should be armed. */
	public static boolean isDay2OrLater(long dayTime) {
		return dayIndex(dayTime) >= 2;
	}

	/** Seed-stable delay in {@code [20s, 30s]} of game ticks. */
	public static long coordsLockGameDelay(long seed) {
		long span = COORDS_LOCK_GAME_TICKS_MAX - COORDS_LOCK_GAME_TICKS_MIN + 1L;
		return COORDS_LOCK_GAME_TICKS_MIN + Math.floorMod(seed ^ 0x434F4F524453L, span);
	}

	/** Seed-stable delay in {@code [5s, 10s]} of game ticks after coords unlock. */
	public static long septicAutoGameDelay(long seed) {
		long span = SEPTIC_AUTO_GAME_TICKS_MAX - SEPTIC_AUTO_GAME_TICKS_MIN + 1L;
		return SEPTIC_AUTO_GAME_TICKS_MIN + Math.floorMod(seed ^ 0x534550544943L, span);
	}
}
