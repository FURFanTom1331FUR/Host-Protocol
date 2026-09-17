package ru.hostprotocol.horror;

import org.junit.jupiter.api.Test;
import ru.hostprotocol.world.ProtocolTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorrorEventLogicTest {
	@Test
	void dayGatesKeepEarlyDaysEerie() {
		assertFalse(HorrorEventLogic.allowed(1, HorrorKind.FAKE_JOIN));
		assertFalse(HorrorEventLogic.allowed(2, HorrorKind.STALKER));
		assertFalse(HorrorEventLogic.allowed(4, HorrorKind.SCREAMER));
		assertFalse(HorrorEventLogic.allowed(5, HorrorKind.PDA_BETRAY));
		assertTrue(HorrorEventLogic.allowed(3, HorrorKind.FAKE_JOIN));
		assertTrue(HorrorEventLogic.allowed(4, HorrorKind.STALKER));
		assertTrue(HorrorEventLogic.allowed(5, HorrorKind.SCREAMER));
		assertTrue(HorrorEventLogic.allowed(6, HorrorKind.PDA_BETRAY));
		assertTrue(HorrorEventLogic.allowed(2, HorrorKind.HEARTBEAT));
	}

	@Test
	void socialGlitchWaitsForDay3Kick() {
		assertFalse(HorrorEventLogic.canSocialGlitch(3, false, false, true));
		assertTrue(HorrorEventLogic.canSocialGlitch(3, true, false, true));
		assertTrue(HorrorEventLogic.canSocialGlitch(4, false, false, true));
		assertFalse(HorrorEventLogic.canSocialGlitch(4, true, true, true));
		assertFalse(HorrorEventLogic.canSocialGlitch(4, true, false, false));
	}

	@Test
	void stalkerLifetimeIsHalfToOneAndHalfSeconds() {
		assertEquals(10, HorrorEventLogic.STALKER_LIFE_MIN);
		assertEquals(30, HorrorEventLogic.STALKER_LIFE_MAX);
		int life = HorrorEventLogic.stalkerLife(42L, 1000L);
		assertTrue(life >= 10 && life <= 30);
		assertEquals(life, HorrorEventLogic.stalkerLife(42L, 1000L));
	}

	@Test
	void stalkerCooldownAndFirstDelay() {
		assertTrue(HorrorEventLogic.firstStalkerReady(400L, 20L));
		assertFalse(HorrorEventLogic.firstStalkerReady(100L, 20L));
		assertTrue(HorrorEventLogic.stalkerCooldownReady(100L, 0L, true));
		assertFalse(HorrorEventLogic.stalkerCooldownReady(100L, 0L, false));
		assertFalse(HorrorEventLogic.stalkerCooldownReady(100L, 90L, true));
		assertTrue(HorrorEventLogic.stalkerCooldownReady(100L + HorrorEventLogic.STALKER_COOLDOWN_TICKS, 100L, false));
	}

	@Test
	void lookScreamerIsRisingEdgeThenRareSecondStare() {
		assertTrue(HorrorEventLogic.shouldLookScreamer(5, true, false, 0));
		assertFalse(HorrorEventLogic.shouldLookScreamer(4, true, false, 0));
		assertFalse(HorrorEventLogic.shouldLookScreamer(5, true, true, 0));
		assertFalse(HorrorEventLogic.shouldLookScreamer(5, true, false, 1));
		assertTrue(HorrorEventLogic.shouldSecondStare(5, true, 55, 0, false));
		assertFalse(HorrorEventLogic.shouldSecondStare(5, true, 54, 0, false));
		assertFalse(HorrorEventLogic.shouldSecondStare(5, true, 80, 0, true));
	}

	@Test
	void focusNightScreamerIsOneShot() {
		assertTrue(HorrorEventLogic.shouldFocusNightScreamer(5, true, true, false));
		assertFalse(HorrorEventLogic.shouldFocusNightScreamer(5, true, true, true));
		assertFalse(HorrorEventLogic.shouldFocusNightScreamer(5, false, true, false));
		assertTrue(HorrorEventLogic.nearFocus(3, 0, 4, 14));
		assertFalse(HorrorEventLogic.nearFocus(20, 0, 0, 14));
		assertTrue(ProtocolTime.isNight(13_000L));
	}

	@Test
	void heartbeatAndPdaBetrayAreRareOrMomentary() {
		assertTrue(HorrorEventLogic.shouldHeartbeat(true, 0));
		assertFalse(HorrorEventLogic.shouldHeartbeat(true, 1));
		assertFalse(HorrorEventLogic.shouldHeartbeat(false, 0));
		assertTrue(HorrorEventLogic.shouldPdaBetray(6, false));
		assertFalse(HorrorEventLogic.shouldPdaBetray(6, true));
		assertFalse(HorrorEventLogic.shouldPdaBetray(5, false));
		assertEquals("Spryzen", HorrorEventLogic.FAKE_PLAYER);
	}

	@Test
	void packetNamesRoundTrip() {
		assertEquals(HorrorKind.SCREAMER, HorrorKind.fromPacket("SCREAMER"));
		assertEquals(HorrorKind.STATIC_FLASH, HorrorKind.fromPacket("nope"));
	}
}
