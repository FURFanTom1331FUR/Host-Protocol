package ru.hostprotocol.progress;

/**
 * Laboratory table transfer: old field PDA + unactivated MK-II shell, nothing else.
 */
public final class PdaTransferLogic {
	private PdaTransferLogic() {}

	public static boolean matches(boolean hasOldPda, boolean hasUnactivatedMk2, int extraItems) {
		return hasOldPda && hasUnactivatedMk2 && extraItems == 0;
	}
}
