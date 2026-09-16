package ru.hostprotocol.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolTimeTest {
	@Test
	void dayIndexFollowsSolarCycle() {
		assertEquals(1, ProtocolTime.dayIndex(0));
		assertEquals(1, ProtocolTime.dayIndex(23999));
		assertEquals(2, ProtocolTime.dayIndex(24000));
		assertEquals(2, ProtocolTime.dayIndex(37000));
		assertEquals(3, ProtocolTime.dayIndex(48000));
	}

	@Test
	void day2NightIsFirstNightOfDayTwo() {
		assertFalse(ProtocolTime.isDay2Night(13000));
		assertFalse(ProtocolTime.isDay2Night(24000));
		assertFalse(ProtocolTime.isDay2Night(36999));
		assertTrue(ProtocolTime.isDay2Night(37000));
		assertTrue(ProtocolTime.isNight(37000));
		assertTrue(ProtocolTime.isDay2Night(43000));
		assertFalse(ProtocolTime.isDay2Night(47000));
		assertFalse(ProtocolTime.isDay2Night(48000));
	}

	@Test
	void coordsLockIsQuarterDay() {
		assertEquals(6000, ProtocolTime.COORDS_LOCK_DELAY);
	}
}
