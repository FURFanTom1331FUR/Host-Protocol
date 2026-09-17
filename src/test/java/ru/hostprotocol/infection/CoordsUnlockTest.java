package ru.hostprotocol.infection;

import org.junit.jupiter.api.Test;
import ru.hostprotocol.world.ProtocolTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordsUnlockTest {
	@Test
	void alreadyUnlockedNeverFiresAgain() {
		assertFalse(CoordsUnlock.shouldUnlock(true, 10_000, 100, 0, 40_000, 37_000, 2));
	}

	@Test
	void gameTimeUnlocksInside20To30Seconds() {
		long start = 1000L;
		long at = start + ProtocolTime.coordsLockGameDelay(7L);
		assertFalse(CoordsUnlock.shouldUnlock(false, at - 1, at, start, 37_000, 37_000, 2));
		assertTrue(CoordsUnlock.shouldUnlock(false, at, at, start, 37_000, 37_000, 2));
	}

	@Test
	void maxGameTimeIsHardCap() {
		long start = 50L;
		assertFalse(CoordsUnlock.shouldUnlock(false, start + 599, Long.MAX_VALUE, start, 37_000, 37_000, 2));
		assertTrue(CoordsUnlock.shouldUnlock(false, start + 600, Long.MAX_VALUE, start, 37_000, 37_000, 2));
	}

	@Test
	void missingUnlockAtUsesMinDelay() {
		long start = 10L;
		assertFalse(CoordsUnlock.shouldUnlock(false, start + 399, 0, start, 24_000, 24_000, 2));
		assertTrue(CoordsUnlock.shouldUnlock(false, start + 400, 0, start, 24_000, 24_000, 2));
	}

	@Test
	void dayTimeFallbackUnlocksEvenIfGameTimeIsStuck() {
		assertFalse(CoordsUnlock.shouldUnlock(false, 10, 10_000, 10, 24_000 + 599, 24_000, 2));
		assertTrue(CoordsUnlock.shouldUnlock(false, 10, 10_000, 10, 24_000 + 600, 24_000, 2));
	}

	@Test
	void negativeDayTimeElapsedDoesNotUnlockWithoutGameTime() {
		assertFalse(CoordsUnlock.shouldUnlock(false, 100, 10_000, 50, 1000, 37_000, 1));
	}

	@Test
	void dayThreeIsLastResortFallback() {
		assertTrue(CoordsUnlock.shouldUnlock(false, 100, 10_000, 50, 48_000, 37_000, 3));
	}
}
