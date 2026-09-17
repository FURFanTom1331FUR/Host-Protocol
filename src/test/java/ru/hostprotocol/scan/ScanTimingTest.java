package ru.hostprotocol.scan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanTimingTest {
	@Test
	void oreScanIsFeelableAndLongerThanMob() {
		assertEquals(50, ScanTiming.ORE_TICKS);
		assertEquals(30, ScanTiming.MOB_TICKS);
		assertTrue(ScanTiming.durationTicks(true) > ScanTiming.durationTicks(false));
		assertTrue(ScanTiming.ORE_TICKS >= 30);
		assertTrue(ScanTiming.ORE_TICKS <= 60);
	}
}
