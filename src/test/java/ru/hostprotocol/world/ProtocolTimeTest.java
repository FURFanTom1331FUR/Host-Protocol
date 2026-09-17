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
	void day2BeginsAtDawnNotOnlyNight() {
		assertTrue(ProtocolTime.isDay2OrLater(24000));
		assertTrue(ProtocolTime.isDay2OrLater(37000));
		assertTrue(ProtocolTime.isDay2OrLater(48000));
		assertFalse(ProtocolTime.isDay2OrLater(23999));
		assertFalse(ProtocolTime.isDay2Night(24000));
		assertTrue(ProtocolTime.isDay2Night(37000));
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
	void coordsLockGameDelayIs20To30Seconds() {
		assertEquals(400, ProtocolTime.COORDS_LOCK_GAME_TICKS_MIN);
		assertEquals(600, ProtocolTime.COORDS_LOCK_GAME_TICKS_MAX);
		long a = ProtocolTime.coordsLockGameDelay(1L);
		long b = ProtocolTime.coordsLockGameDelay(99999L);
		assertTrue(a >= 400 && a <= 600);
		assertTrue(b >= 400 && b <= 600);
		assertEquals(a, ProtocolTime.coordsLockGameDelay(1L));
	}

	@Test
	void septicAutoDelayIs5To10Seconds() {
		assertEquals(100, ProtocolTime.SEPTIC_AUTO_GAME_TICKS_MIN);
		assertEquals(200, ProtocolTime.SEPTIC_AUTO_GAME_TICKS_MAX);
		long d = ProtocolTime.septicAutoGameDelay(42L);
		assertTrue(d >= 100 && d <= 200);
		assertEquals(d, ProtocolTime.septicAutoGameDelay(42L));
	}

	@Test
	void day3KickIsTenSecondsThenFx() {
		assertEquals(200, ProtocolTime.DAY3_DISCONNECT_DELAY_TICKS);
		assertEquals(100, ProtocolTime.DAY3_KICK_FX_TICKS);
	}
}
