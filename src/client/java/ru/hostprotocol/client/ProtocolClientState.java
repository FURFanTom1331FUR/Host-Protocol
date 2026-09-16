package ru.hostprotocol.client;

/**
 * Client mirror of per-player protocol flags (PDA logs, intro). Day HUD uses world day-time locally.
 */
public final class ProtocolClientState {
	private static String subjectId = "—";
	private static boolean introCompleted;
	private static boolean day2Log;
	private static boolean pdaGiven;
	private static int syncedDay = 1;

	private ProtocolClientState() {}

	public static void apply(String subject, boolean introDone, boolean day2, boolean pda, int day) {
		subjectId = subject == null || subject.isEmpty() ? "—" : subject;
		introCompleted = introDone;
		day2Log = day2;
		pdaGiven = pda;
		syncedDay = Math.max(1, day);
	}

	public static void reset() {
		subjectId = "—";
		introCompleted = false;
		day2Log = false;
		pdaGiven = false;
		syncedDay = 1;
	}

	public static String subjectId() {
		return subjectId;
	}

	public static boolean isIntroCompleted() {
		return introCompleted;
	}

	public static boolean hasDay2Log() {
		return day2Log;
	}

	public static boolean hasPdaGiven() {
		return pdaGiven;
	}

	public static int syncedDay() {
		return syncedDay;
	}
}
