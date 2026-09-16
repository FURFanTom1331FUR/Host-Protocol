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
 * <p>Testing notes:
 * <ul>
 *   <li>{@code /time add 24000} — advance exactly one solar day (Day N → Day N+1 at the same clock phase).</li>
 *   <li>{@code /time set 24000} — jump to dawn of Day 2 from a fresh world (day-time 0).</li>
 *   <li>{@code /time set day} — sets the clock to {@code 1000} (morning) <em>without</em> changing the day index.</li>
 *   <li>Sleeping through the night jumps to dawn of the next day (vanilla {@code setDayTime} skip).</li>
 * </ul>
 */
public final class ProtocolTime {
	public static final long TICKS_PER_DAY = 24000L;

	private ProtocolTime() {}

	public static int dayIndex(long dayTime) {
		if (dayTime < 0L) {
			dayTime = 0L;
		}
		return (int) (dayTime / TICKS_PER_DAY) + 1;
	}
}
