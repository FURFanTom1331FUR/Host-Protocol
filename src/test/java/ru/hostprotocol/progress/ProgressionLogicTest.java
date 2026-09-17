package ru.hostprotocol.progress;

import org.junit.jupiter.api.Test;
import ru.hostprotocol.world.ProtocolTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionLogicTest {
	@Test
	void ironOrePathsUnlockOnce() {
		assertTrue(IronScanLogic.isIronOrePath("iron_ore"));
		assertTrue(IronScanLogic.isIronOrePath("deepslate_iron_ore"));
		assertFalse(IronScanLogic.isIronOrePath("gold_ore"));
		assertFalse(IronScanLogic.isIronOrePath(null));
		assertTrue(IronScanLogic.shouldUnlockLabBlueprints(true, false));
		assertFalse(IronScanLogic.shouldUnlockLabBlueprints(true, true));
		assertFalse(IronScanLogic.shouldUnlockLabBlueprints(false, false));
	}

	@Test
	void transferNeedsOldPdaAndShellOnly() {
		assertTrue(PdaTransferLogic.matches(true, true, 0));
		assertFalse(PdaTransferLogic.matches(true, false, 0));
		assertFalse(PdaTransferLogic.matches(false, true, 0));
		assertFalse(PdaTransferLogic.matches(true, true, 1));
	}

	@Test
	void dayFiveStartsAt96000() {
		assertEquals(4, ProtocolTime.dayIndex(95_999));
		assertEquals(5, ProtocolTime.dayIndex(96_000));
		assertTrue(SepticPresenceLogic.shouldSpawn(5, false));
		assertFalse(SepticPresenceLogic.shouldSpawn(5, true));
		assertFalse(SepticPresenceLogic.shouldSpawn(4, false));
	}

	@Test
	void lookingAtSepticUsesConeAndRange() {
		assertTrue(SepticPresenceLogic.isLookingAt(0.95, 12.0));
		assertFalse(SepticPresenceLogic.isLookingAt(0.5, 12.0));
		assertFalse(SepticPresenceLogic.isLookingAt(0.99, 80.0));
		assertFalse(SepticPresenceLogic.isLookingAt(1.0, 0.1));
	}

	@Test
	void infectionRadiusAndStages() {
		assertEquals(10, InfectedMobLogic.affectRadius(4));
		assertTrue(InfectedMobLogic.inRadius(3, 0, 4, 5));
		assertFalse(InfectedMobLogic.inRadius(6, 0, 0, 5));
		assertEquals(0, InfectedMobLogic.stageForTicks(0));
		assertEquals(1, InfectedMobLogic.stageForTicks(20));
		assertEquals(2, InfectedMobLogic.stageForTicks(InfectedMobLogic.STAGE2_TICKS));
		assertTrue(InfectedMobLogic.shouldDropFlesh(true, false));
		assertFalse(InfectedMobLogic.shouldDropFlesh(true, true));
		assertFalse(InfectedMobLogic.shouldDropFlesh(false, false));
	}
}
