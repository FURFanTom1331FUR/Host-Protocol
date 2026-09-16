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
 * Infection focus activates on the <strong>first night while {@code dayIndex == 2}</strong>:
 * {@code 13000 <= (dayTime % 24000) < 23000}, i.e. day-time in {@code [37000, 47000)}.
 * {@code /time set night} jumps to {@code 13000} (Day 1 night) and does <em>not</em> start Day 2
 * infection. Use {@code /time set 37000} or {@code /time add 13000} from dawn of Day 2.
 *
 * <p>Coordinate lock delay after activation is {@link #COORDS_LOCK_DELAY} ticks
 * (¼ of a solar day, ~5 minutes / 6000 ticks). {@code /time add 6000} after activation unlocks it.
 *
 * <p>Testing notes:
 * <ul>
 *   <li>{@code /time add 24000} — advance exactly one solar day (Day N → Day N+1 at the same clock phase).</li>
 *   <li>{@code /time set 24000} — jump to dawn of Day 2 from a fresh world (day-time 0).</li>
 *   <li>{@code /time set 37000} — jump to night of Day 2 (infection starts).</li>
 *   <li>{@code /time set day} — sets the clock to {@code 1000} (morning) <em>without</em> changing the day index.</li>
 *   <li>{@code /time set night} — sets the clock to {@code 13000} <em>of day 1's cycle</em> if used as an absolute set.</li>
 *   <li>Sleeping through the night jumps to dawn of the next day (vanilla {@code setDayTime} skip),
 *       except the first Day-2-night sleep while infection is active (Septic link cinematic; night is not skipped).</li>
 * </ul>
 */
public final class ProtocolTime {
	public static final long TICKS_PER_DAY = 24000L;
	/** Sunset / start of night in the solar cycle. Vanilla {@code /time set night} uses this value. */
	public static final long NIGHT_START = 13000L;
	/** Morning: sun is up enough that vanilla treats the cycle as day again. */
	public static final long DAY_START = 23000L;
	/** After infection activates, wait this long (¼ MC day) before locking focus coordinates. */
	public static final long COORDS_LOCK_DELAY = TICKS_PER_DAY / 4L;

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
	 * Absolute day-time range: {@code [37000, 47000)}.
	 */
	public static boolean isDay2Night(long dayTime) {
		return dayIndex(dayTime) == 2 && isNight(dayTime);
	}
}
