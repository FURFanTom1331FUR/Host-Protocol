package ru.hostprotocol.progress;

/**
 * First iron-ore scan unlocks laboratory + PDA MK-II blueprints.
 */
public final class IronScanLogic {
	private IronScanLogic() {}

	public static boolean isIronOrePath(String path) {
		if (path == null || path.isEmpty()) {
			return false;
		}
		return "iron_ore".equals(path) || "deepslate_iron_ore".equals(path);
	}

	/** @return true if this scan should fire the lab/MK-II unlock VO */
	public static boolean shouldUnlockLabBlueprints(boolean isIronOre, boolean alreadyUnlocked) {
		return isIronOre && !alreadyUnlocked;
	}
}
