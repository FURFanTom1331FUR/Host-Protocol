package ru.hostprotocol.infection;

import ru.hostprotocol.world.ProtocolTime;

/**
 * Pure decision for when Day-2 focus coordinates become visible.
 *
 * <p>Clock is {@code gameTime} (~20–30s). Immune to {@code /time set} jumping the sun.
 * If the delay already elapsed (load / skip), unlock immediately. Day index ≥ 3 is a
 * last-resort so a jump to Day 3 cannot skip the lock before the 10s kick.
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
		if (infectionStartGameTime > 0L
				&& unlockAtGameTime <= 0L
				&& gameTime - infectionStartGameTime >= ProtocolTime.COORDS_LOCK_GAME_TICKS_MIN) {
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
