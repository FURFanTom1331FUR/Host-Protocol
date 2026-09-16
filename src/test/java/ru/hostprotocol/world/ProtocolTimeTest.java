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
	void coordsLockGameDelayIs45To90Seconds() {
		assertEquals(900, ProtocolTime.COORDS_LOCK_GAME_TICKS_MIN);
		assertEquals(1800, ProtocolTime.COORDS_LOCK_GAME_TICKS_MAX);
		assertEquals(2400, ProtocolTime.COORDS_LOCK_DAYTIME_FALLBACK);
		long a = ProtocolTime.coordsLockGameDelay(1L);
		long b = ProtocolTime.coordsLockGameDelay(99999L);
		assertTrue(a >= 900 && a <= 1800);
		assertTrue(b >= 900 && b <= 1800);
		assertEquals(a, ProtocolTime.coordsLockGameDelay(1L));
	}

	@Test
	void septicAutoDelayIs10To20Seconds() {
		assertEquals(200, ProtocolTime.SEPTIC_AUTO_GAME_TICKS_MIN);
		assertEquals(400, ProtocolTime.SEPTIC_AUTO_GAME_TICKS_MAX);
		long d = ProtocolTime.septicAutoGameDelay(42L);
		assertTrue(d >= 200 && d <= 400);
		assertEquals(d, ProtocolTime.septicAutoGameDelay(42L));
	}
}
