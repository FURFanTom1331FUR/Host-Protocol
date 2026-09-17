package ru.hostprotocol.horror;

/**
 * Client/server scare beats. Scheduled by day; never disconnects or deletes worlds.
 */
public enum HorrorKind {
	FAKE_JOIN,
	FAKE_TOAST,
	STATIC_FLASH,
	STALKER,
	SCREAMER,
	PDA_BETRAY,
	HEARTBEAT;

	public static HorrorKind fromPacket(String raw) {
		if (raw == null || raw.isEmpty()) {
			return STATIC_FLASH;
		}
		try {
			return HorrorKind.valueOf(raw);
		} catch (IllegalArgumentException ignored) {
			return STATIC_FLASH;
		}
	}
}
