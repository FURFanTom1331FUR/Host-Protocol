package ru.hostprotocol.client;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Client mirror of per-player protocol flags (PDA logs, intro, infection lock, breach, lab).
 */
public final class ProtocolClientState {
	private static String subjectId = "—";
	private static String garbledSubjectId = "—";
	private static boolean introCompleted;
	private static boolean day2Log;
	private static boolean day2Coords;
	private static boolean day3Log;
	private static boolean pdaGiven;
	private static int syncedDay = 1;
	private static boolean infectionActive;
	private static boolean septicLinkAttempted;
	private static boolean coordsDiscovered;
	private static boolean protocolBreached;
	private static boolean systemErrorLog;
	private static boolean blueprints;
	private static boolean labBlueprints;
	private static boolean transferred;
	private static boolean baseBlueprints;
	private static boolean mk2Booted;
	private static int infectionStage;
	private static boolean septicPresent;
	private static final List<String> recentScans = new ArrayList<>();
	private static int focusX;
	private static int focusY;
	private static int focusZ;

	private ProtocolClientState() {}

	public static void apply(
			String subject,
			boolean introDone,
			boolean day2,
			boolean coords,
			boolean day3,
			boolean pda,
			int day,
			boolean infection,
			boolean septicLink,
			boolean hasCoords,
			BlockPos focus,
			String garbled,
			boolean breached,
			boolean systemError,
			boolean scannerBlueprints,
			boolean labBp,
			boolean didTransfer,
			boolean baseBp,
			boolean booted,
			int stage,
			boolean septic,
			List<String> scans
	) {
		subjectId = subject == null || subject.isEmpty() ? "—" : subject;
		garbledSubjectId = garbled == null || garbled.isEmpty() ? "—" : garbled;
		introCompleted = introDone;
		day2Log = day2;
		day2Coords = coords;
		day3Log = day3;
		pdaGiven = pda;
		syncedDay = Math.max(1, day);
		infectionActive = infection;
		septicLinkAttempted = septicLink;
		coordsDiscovered = hasCoords;
		protocolBreached = breached;
		systemErrorLog = systemError;
		blueprints = scannerBlueprints;
		labBlueprints = labBp;
		transferred = didTransfer;
		baseBlueprints = baseBp;
		mk2Booted = booted;
		infectionStage = Math.max(0, stage);
		septicPresent = septic;
		recentScans.clear();
		if (scans != null) {
			recentScans.addAll(scans);
		}
		if (focus != null) {
			focusX = focus.getX();
			focusY = focus.getY();
			focusZ = focus.getZ();
		}
	}

	public static void reset() {
		subjectId = "—";
		garbledSubjectId = "—";
		introCompleted = false;
		day2Log = false;
		day2Coords = false;
		day3Log = false;
		pdaGiven = false;
		syncedDay = 1;
		infectionActive = false;
		septicLinkAttempted = false;
		coordsDiscovered = false;
		protocolBreached = false;
		systemErrorLog = false;
		blueprints = false;
		labBlueprints = false;
		transferred = false;
		baseBlueprints = false;
		mk2Booted = false;
		infectionStage = 0;
		septicPresent = false;
		recentScans.clear();
		focusX = 0;
		focusY = 0;
		focusZ = 0;
	}

	public static String subjectId() {
		return subjectId;
	}

	public static String garbledSubjectId() {
		return garbledSubjectId;
	}

	public static boolean isIntroCompleted() {
		return introCompleted;
	}

	public static boolean hasDay2Log() {
		return day2Log;
	}

	public static boolean hasDay2Coords() {
		return day2Coords || coordsDiscovered;
	}

	public static boolean hasDay3Log() {
		return day3Log;
	}

	public static boolean hasPdaGiven() {
		return pdaGiven;
	}

	public static int syncedDay() {
		return syncedDay;
	}

	public static boolean isInfectionActive() {
		return infectionActive;
	}

	public static boolean hasSepticLinkAttempted() {
		return septicLinkAttempted;
	}

	public static boolean isCoordsDiscovered() {
		return coordsDiscovered || day2Coords;
	}

	public static boolean isProtocolBreached() {
		return protocolBreached;
	}

	public static boolean hasSystemErrorLog() {
		return systemErrorLog;
	}

	public static boolean hasBlueprints() {
		return blueprints;
	}

	public static boolean hasLabBlueprints() {
		return labBlueprints;
	}

	public static boolean hasTransferred() {
		return transferred;
	}

	public static boolean hasBaseBlueprints() {
		return baseBlueprints;
	}

	public static boolean hasMk2Booted() {
		return mk2Booted;
	}

	public static int infectionStage() {
		return infectionStage;
	}

	public static boolean isSepticPresent() {
		return septicPresent;
	}

	public static List<String> recentScans() {
		return List.copyOf(recentScans);
	}

	public static int focusX() {
		return focusX;
	}

	public static int focusY() {
		return focusY;
	}

	public static int focusZ() {
		return focusZ;
	}

	public static String coordsLabel() {
		if (!isCoordsDiscovered()) {
			return null;
		}
		return focusX + " / " + focusY + " / " + focusZ;
	}
}
