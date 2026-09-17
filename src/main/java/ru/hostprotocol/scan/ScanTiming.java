package ru.hostprotocol.scan;

/**
 * Hold-to-scan durations. Ores should feel slower than a living ping.
 */
public final class ScanTiming {
	public static final int ORE_TICKS = 50;
	public static final int MOB_TICKS = 30;
	public static final double RANGE = 8.0;
	public static final double MAX_MOVE = 3.5;

	private ScanTiming() {}

	public static int durationTicks(boolean ore) {
		return ore ? ORE_TICKS : MOB_TICKS;
	}
}
