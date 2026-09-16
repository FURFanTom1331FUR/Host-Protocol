package ru.hostprotocol.infection;

import ru.hostprotocol.world.ProtocolTime;

/**
 * Pure decision for when Day-2 focus coordinates become visible.
 *
 * <p>Primary clock is {@code gameTime} (real ticking, immune to {@code /time set} jumping the sun
 * backwards). {@code dayTime} is a fallback so {@code /time add} still unlocks. Day index ≥ 3
 * catches players who slept/skipped the rest of Day 2 without waiting.
 */
public final class CoordsUnlock {
	private CoordsUnlock() {}

	public static boolean shouldUnlock(
			boolean alreadyUnlocked,
			long gameTime,
			long unlockAtGameTime,
			long infectionStartGameTime,
			long dayTime,
			long infectionStartDayTime,
			int dayIndex
	) {
		if (alreadyUnlocked) {
			return false;
		}
		if (unlockAtGameTime > 0L && gameTime >= unlockAtGameTime) {
			return true;
		}
		if (infectionStartGameTime > 0L
				&& gameTime - infectionStartGameTime >= ProtocolTime.COORDS_LOCK_GAME_TICKS_MAX) {
			return true;
		}
		if (infectionStartDayTime > 0L) {
			long dayElapsed = dayTime - infectionStartDayTime;
			if (dayElapsed >= ProtocolTime.COORDS_LOCK_DAYTIME_FALLBACK) {
				return true;
			}
		}
		return dayIndex >= 3;
	}
}
