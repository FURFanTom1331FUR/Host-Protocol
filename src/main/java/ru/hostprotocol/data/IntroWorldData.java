package ru.hostprotocol.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import ru.hostprotocol.HostProtocolMod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Per-world Host Protocol state: subject ID, intro, PDA grants, day announcements, PDA logs,
 * infection focus, Night-2 Septic handshake, and the post-coords+handshake protocol breach.
 */
public class IntroWorldData extends SavedData {
	public static final String DATA_NAME = HostProtocolMod.MOD_ID + "_intro";

	private static final char[] CYR_PREFIX = {'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ж', 'З', 'И', 'К', 'Л', 'М', 'Н', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Э', 'Ю', 'Я'};
	private static final char[] CYR_SUFFIX = {'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ж', 'З', 'И', 'К', 'Л', 'М', 'Н', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х'};

	private String subjectId;
	private boolean introCompleted;
	private boolean pdaPlacedAtSpawn;
	private final Set<UUID> pdaRecipients = new HashSet<>();
	private final Set<UUID> pdaDelivered = new HashSet<>();
	private final Set<UUID> day2Logs = new HashSet<>();
	private final Set<UUID> day2CoordsLogs = new HashSet<>();
	private final Set<UUID> day3Logs = new HashSet<>();
	private final Set<UUID> systemErrorLogs = new HashSet<>();
	private final Set<UUID> blueprintLogs = new HashSet<>();
	private final Set<UUID> labBlueprintLogs = new HashSet<>();
	private final Map<UUID, Set<Long>> scannedOres = new HashMap<>();
	private final Set<UUID> breachAcknowledged = new HashSet<>();
	private final Set<UUID> day3DisconnectFired = new HashSet<>();
	private final Set<UUID> day3LeftBeforeKick = new HashSet<>();
	private final Map<UUID, Integer> lastAnnouncedDays = new HashMap<>();
	private final Map<UUID, Long> day3ArmedAtGameTime = new HashMap<>();

	private boolean infectionActive;
	private boolean infectionFocusSet;
	private int focusX;
	private int focusY;
	private int focusZ;
	private long infectionStartDayTime;
	private long infectionStartGameTime;
	private long coordsUnlockAtGameTime;
	private long septicAutoAtGameTime;
	private boolean coordsDiscovered;
	private boolean septicLinkAttempted;
	private boolean protocolBreached;
	private boolean forcedChunksArmed;

	public IntroWorldData() {
		this.subjectId = generateSubjectId();
		this.introCompleted = false;
	}

	public IntroWorldData(String subjectId, boolean introCompleted) {
		this.subjectId = subjectId;
		this.introCompleted = introCompleted;
	}

	public static IntroWorldData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(
				IntroWorldData::load,
				IntroWorldData::new,
				DATA_NAME
		);
	}

	public static IntroWorldData load(CompoundTag tag) {
		String id = tag.contains("SubjectId") ? tag.getString("SubjectId") : generateSubjectId();
		boolean done = tag.getBoolean("IntroCompleted");
		IntroWorldData data = new IntroWorldData(id, done);
		data.pdaPlacedAtSpawn = tag.getBoolean("PdaPlacedAtSpawn");
		readUuidSet(tag, "PdaRecipients", data.pdaRecipients);
		readUuidSet(tag, "PdaDelivered", data.pdaDelivered);
		readUuidSet(tag, "Day2Logs", data.day2Logs);
		readUuidSet(tag, "Day2CoordsLogs", data.day2CoordsLogs);
		readUuidSet(tag, "Day3Logs", data.day3Logs);
		readUuidSet(tag, "SystemErrorLogs", data.systemErrorLogs);
		readUuidSet(tag, "BlueprintLogs", data.blueprintLogs);
		readUuidSet(tag, "LabBlueprintLogs", data.labBlueprintLogs);
		if (tag.contains("ScannedOres", Tag.TAG_LIST)) {
			ListTag list = tag.getList("ScannedOres", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag entry = list.getCompound(i);
				try {
					UUID uuid = UUID.fromString(entry.getString("Id"));
					Set<Long> positions = new HashSet<>();
					for (long packed : entry.getLongArray("Positions")) {
						positions.add(packed);
					}
					data.scannedOres.put(uuid, positions);
				} catch (IllegalArgumentException ignored) {
					// skip corrupt UUID
				}
			}
		}
		readUuidSet(tag, "BreachAcknowledged", data.breachAcknowledged);
		readUuidSet(tag, "Day3DisconnectFired", data.day3DisconnectFired);
		readUuidSet(tag, "Day3LeftBeforeKick", data.day3LeftBeforeKick);
		if (tag.contains("LastAnnouncedDays", Tag.TAG_LIST)) {
			ListTag list = tag.getList("LastAnnouncedDays", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag entry = list.getCompound(i);
				try {
					UUID uuid = UUID.fromString(entry.getString("Id"));
					data.lastAnnouncedDays.put(uuid, entry.getInt("Day"));
				} catch (IllegalArgumentException ignored) {
					// skip corrupt UUID
				}
			}
		}
		data.infectionActive = tag.getBoolean("InfectionActive");
		data.infectionFocusSet = tag.getBoolean("InfectionFocusSet");
		data.focusX = tag.getInt("FocusX");
		data.focusY = tag.getInt("FocusY");
		data.focusZ = tag.getInt("FocusZ");
		data.infectionStartDayTime = tag.getLong("InfectionStartDayTime");
		data.infectionStartGameTime = tag.getLong("InfectionStartGameTime");
		data.coordsUnlockAtGameTime = tag.getLong("CoordsUnlockAtGameTime");
		data.septicAutoAtGameTime = tag.getLong("SepticAutoAtGameTime");
		data.coordsDiscovered = tag.getBoolean("CoordsDiscovered");
		data.septicLinkAttempted = tag.getBoolean("SepticLinkAttempted");
		data.protocolBreached = tag.getBoolean("ProtocolBreached");
		data.forcedChunksArmed = tag.getBoolean("ForcedChunksArmed");
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		tag.putString("SubjectId", subjectId);
		tag.putBoolean("IntroCompleted", introCompleted);
		tag.putBoolean("PdaPlacedAtSpawn", pdaPlacedAtSpawn);
		writeUuidSet(tag, "PdaRecipients", pdaRecipients);
		writeUuidSet(tag, "PdaDelivered", pdaDelivered);
		writeUuidSet(tag, "Day2Logs", day2Logs);
		writeUuidSet(tag, "Day2CoordsLogs", day2CoordsLogs);
		writeUuidSet(tag, "Day3Logs", day3Logs);
		writeUuidSet(tag, "SystemErrorLogs", systemErrorLogs);
		writeUuidSet(tag, "BlueprintLogs", blueprintLogs);
		writeUuidSet(tag, "LabBlueprintLogs", labBlueprintLogs);
		ListTag scanned = new ListTag();
		for (Map.Entry<UUID, Set<Long>> entry : scannedOres.entrySet()) {
			CompoundTag row = new CompoundTag();
			row.putString("Id", entry.getKey().toString());
			long[] packed = new long[entry.getValue().size()];
			int i = 0;
			for (Long value : entry.getValue()) {
				packed[i++] = value;
			}
			row.putLongArray("Positions", packed);
			scanned.add(row);
		}
		tag.put("ScannedOres", scanned);
		writeUuidSet(tag, "BreachAcknowledged", breachAcknowledged);
		writeUuidSet(tag, "Day3DisconnectFired", day3DisconnectFired);
		writeUuidSet(tag, "Day3LeftBeforeKick", day3LeftBeforeKick);
		ListTag announced = new ListTag();
		for (Map.Entry<UUID, Integer> entry : lastAnnouncedDays.entrySet()) {
			CompoundTag row = new CompoundTag();
			row.putString("Id", entry.getKey().toString());
			row.putInt("Day", entry.getValue());
			announced.add(row);
		}
		tag.put("LastAnnouncedDays", announced);
		tag.putBoolean("InfectionActive", infectionActive);
		tag.putBoolean("InfectionFocusSet", infectionFocusSet);
		tag.putInt("FocusX", focusX);
		tag.putInt("FocusY", focusY);
		tag.putInt("FocusZ", focusZ);
		tag.putLong("InfectionStartDayTime", infectionStartDayTime);
		tag.putLong("InfectionStartGameTime", infectionStartGameTime);
		tag.putLong("CoordsUnlockAtGameTime", coordsUnlockAtGameTime);
		tag.putLong("SepticAutoAtGameTime", septicAutoAtGameTime);
		tag.putBoolean("CoordsDiscovered", coordsDiscovered);
		tag.putBoolean("SepticLinkAttempted", septicLinkAttempted);
		tag.putBoolean("ProtocolBreached", protocolBreached);
		tag.putBoolean("ForcedChunksArmed", forcedChunksArmed);
		return tag;
	}

	private static void writeUuidSet(CompoundTag tag, String key, Set<UUID> ids) {
		ListTag list = new ListTag();
		for (UUID id : ids) {
			list.add(StringTag.valueOf(id.toString()));
		}
		tag.put(key, list);
	}

	private static void readUuidSet(CompoundTag tag, String key, Set<UUID> dest) {
		if (!tag.contains(key, Tag.TAG_LIST)) {
			return;
		}
		ListTag list = tag.getList(key, Tag.TAG_STRING);
		for (int i = 0; i < list.size(); i++) {
			try {
				dest.add(UUID.fromString(list.getString(i)));
			} catch (IllegalArgumentException ignored) {
				// skip corrupt UUID
			}
		}
	}

	public String getSubjectId() {
		return subjectId;
	}

	/** A nearby-but-wrong subject echo used by the failed Septic handshake / Day-3 log. */
	public String garbledSubjectId() {
		String id = subjectId == null || subjectId.isEmpty() ? "С000А" : subjectId;
		char[] chars = id.toCharArray();
		boolean mutated = false;
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] >= '0' && chars[i] <= '9') {
				chars[i] = (char) ('0' + ((chars[i] - '0' + 7) % 10));
				mutated = true;
			}
		}
		if (!mutated && chars.length > 0) {
			chars[chars.length - 1] = 'Х';
		}
		return new String(chars);
	}

	public boolean isIntroCompleted() {
		return introCompleted;
	}

	public void markIntroCompleted() {
		if (!introCompleted) {
			introCompleted = true;
			setDirty();
		}
	}

	/** Testing helper: play the intro again in this world. Does not reset PDA grants or day logs. */
	public void resetIntroCompleted() {
		if (introCompleted) {
			introCompleted = false;
			setDirty();
		}
	}

	public boolean hasReceivedPda(UUID playerId) {
		return pdaRecipients.contains(playerId);
	}

	public void markPdaReceived(UUID playerId) {
		if (pdaRecipients.add(playerId)) {
			setDirty();
		}
	}

	public boolean hasDeliveredPda(UUID playerId) {
		return pdaDelivered.contains(playerId);
	}

	public void markPdaDelivered(UUID playerId) {
		boolean changed = pdaDelivered.add(playerId);
		changed |= pdaRecipients.add(playerId);
		if (changed) {
			setDirty();
		}
	}

	public boolean hasDay2Log(UUID playerId) {
		return day2Logs.contains(playerId);
	}

	/** @return true if this was the first unlock for the player */
	public boolean markDay2Log(UUID playerId) {
		if (day2Logs.add(playerId)) {
			setDirty();
			return true;
		}
		return false;
	}

	public boolean hasDay2CoordsLog(UUID playerId) {
		return day2CoordsLogs.contains(playerId);
	}

	public boolean markDay2CoordsLog(UUID playerId) {
		if (day2CoordsLogs.add(playerId)) {
			setDirty();
			return true;
		}
		return false;
	}

	public boolean hasDay3Log(UUID playerId) {
		return day3Logs.contains(playerId);
	}

	public boolean markDay3Log(UUID playerId) {
		if (day3Logs.add(playerId)) {
			setDirty();
			return true;
		}
		return false;
	}

	public boolean hasSystemErrorLog(UUID playerId) {
		return systemErrorLogs.contains(playerId);
	}

	public boolean markSystemErrorLog(UUID playerId) {
		if (systemErrorLogs.add(playerId)) {
			setDirty();
			return true;
		}
		return false;
	}

	public boolean hasBlueprints(UUID playerId) {
		return blueprintLogs.contains(playerId);
	}

	public boolean markBlueprints(UUID playerId) {
		if (blueprintLogs.add(playerId)) {
			setDirty();
			return true;
		}
		return false;
	}

	public boolean hasLabBlueprints(UUID playerId) {
		return labBlueprintLogs.contains(playerId);
	}

	/** First iron-ore scan unlock. Returns true only the first time. */
	public boolean markLabBlueprints(UUID playerId) {
		if (labBlueprintLogs.add(playerId)) {
			setDirty();
			return true;
		}
		return false;
	}

	public boolean isOreScanned(UUID playerId, BlockPos pos) {
		Set<Long> set = scannedOres.get(playerId);
		return set != null && set.contains(pos.asLong());
	}

	/** @return true if this BlockPos was newly marked for the player */
	public boolean markOreScanned(UUID playerId, BlockPos pos) {
		Set<Long> set = scannedOres.computeIfAbsent(playerId, id -> new HashSet<>());
		if (set.add(pos.asLong())) {
			setDirty();
			return true;
		}
		return false;
	}

	public int scannedOreCount(UUID playerId) {
		Set<Long> set = scannedOres.get(playerId);
		return set == null ? 0 : set.size();
	}

	public boolean hasBreachAcknowledged(UUID playerId) {
		return breachAcknowledged.contains(playerId);
	}

	public boolean markBreachAcknowledged(UUID playerId) {
		if (breachAcknowledged.add(playerId)) {
			setDirty();
			return true;
		}
		return false;
	}

	public boolean hasDay3DisconnectFired(UUID playerId) {
		return day3DisconnectFired.contains(playerId);
	}

	public boolean markDay3DisconnectFired(UUID playerId) {
		boolean changed = day3DisconnectFired.add(playerId);
		changed |= day3ArmedAtGameTime.remove(playerId) != null;
		if (changed) {
			setDirty();
		}
		return day3DisconnectFired.contains(playerId) && changed;
	}

	public boolean hasDay3LeftBeforeKick(UUID playerId) {
		return day3LeftBeforeKick.contains(playerId);
	}

	public boolean markDay3LeftBeforeKick(UUID playerId) {
		if (day3DisconnectFired.contains(playerId)) {
			return false;
		}
		boolean changed = day3LeftBeforeKick.add(playerId);
		changed |= day3ArmedAtGameTime.remove(playerId) != null;
		if (changed) {
			setDirty();
		}
		return changed;
	}

	public long getDay3ArmedAtGameTime(UUID playerId) {
		return day3ArmedAtGameTime.getOrDefault(playerId, 0L);
	}

	public void armDay3Stay(UUID playerId, long gameTime) {
		if (day3ArmedAtGameTime.containsKey(playerId)) {
			return;
		}
		day3ArmedAtGameTime.put(playerId, gameTime);
		setDirty();
	}

	public void clearDay3Stay(UUID playerId) {
		if (day3ArmedAtGameTime.remove(playerId) != null) {
			setDirty();
		}
	}

	public int getLastAnnouncedDay(UUID playerId) {
		return lastAnnouncedDays.getOrDefault(playerId, 0);
	}

	public void setLastAnnouncedDay(UUID playerId, int day) {
		Integer previous = lastAnnouncedDays.put(playerId, day);
		if (previous == null || previous != day) {
			setDirty();
		}
	}

	public boolean isPdaPlacedAtSpawn() {
		return pdaPlacedAtSpawn;
	}

	public boolean isInfectionActive() {
		return infectionActive;
	}

	public boolean hasInfectionFocus() {
		return infectionFocusSet;
	}

	public BlockPos getFocusPos() {
		return new BlockPos(focusX, focusY, focusZ);
	}

	public int getFocusX() {
		return focusX;
	}

	public int getFocusY() {
		return focusY;
	}

	public int getFocusZ() {
		return focusZ;
	}

	public long getInfectionStartDayTime() {
		return infectionStartDayTime;
	}

	public long getInfectionStartGameTime() {
		return infectionStartGameTime;
	}

	public long getCoordsUnlockAtGameTime() {
		return coordsUnlockAtGameTime;
	}

	public long getSepticAutoAtGameTime() {
		return septicAutoAtGameTime;
	}

	public void setSepticAutoAtGameTime(long gameTime) {
		if (this.septicAutoAtGameTime != gameTime) {
			this.septicAutoAtGameTime = gameTime;
			setDirty();
		}
	}

	public void activateInfection(BlockPos focus, long startDayTime, long startGameTime, long coordsUnlockAt) {
		this.infectionActive = true;
		this.infectionFocusSet = true;
		this.focusX = focus.getX();
		this.focusY = focus.getY();
		this.focusZ = focus.getZ();
		this.infectionStartDayTime = startDayTime;
		this.infectionStartGameTime = startGameTime;
		this.coordsUnlockAtGameTime = coordsUnlockAt;
		this.forcedChunksArmed = false;
		setDirty();
	}

	/**
	 * Worlds saved before gameTime clocks existed still get a fair real-time wait from first tick
	 * after the update, plus whatever dayTime fallback they already accumulated.
	 */
	public void ensureInfectionClocks(long gameTime, long seed) {
		ensureInfectionClocks(gameTime, seed, 1);
	}

	/**
	 * Worlds saved before gameTime clocks existed still get a fair real-time wait from first tick
	 * after the update. If the delay already elapsed (or Day 3 is already active), unlock is due now.
	 */
	public void ensureInfectionClocks(long gameTime, long seed, int dayIndex) {
		if (!infectionActive) {
			return;
		}
		boolean dirty = false;
		if (infectionStartGameTime <= 0L) {
			infectionStartGameTime = gameTime;
			dirty = true;
		}
		if (coordsUnlockAtGameTime <= 0L && !coordsDiscovered) {
			long delay = ru.hostprotocol.world.ProtocolTime.coordsLockGameDelay(seed);
			long due = infectionStartGameTime + delay;
			if (dayIndex >= 3 || gameTime >= due) {
				coordsUnlockAtGameTime = gameTime;
			} else {
				coordsUnlockAtGameTime = due;
			}
			dirty = true;
		} else if (!coordsDiscovered && coordsUnlockAtGameTime > 0L && gameTime >= coordsUnlockAtGameTime) {
			// already due — maybeUnlockCoords handles it this tick
		}
		if (dirty) {
			setDirty();
		}
	}

	public boolean isCoordsDiscovered() {
		return coordsDiscovered;
	}

	public boolean markCoordsDiscovered() {
		if (!coordsDiscovered) {
			coordsDiscovered = true;
			setDirty();
			return true;
		}
		return false;
	}

	public boolean hasSepticLinkAttempted() {
		return septicLinkAttempted;
	}

	public boolean markSepticLinkAttempted() {
		if (!septicLinkAttempted) {
			septicLinkAttempted = true;
			setDirty();
			return true;
		}
		return false;
	}

	public boolean isProtocolBreached() {
		return protocolBreached;
	}

	public boolean markProtocolBreached() {
		if (!protocolBreached) {
			protocolBreached = true;
			setDirty();
			return true;
		}
		return false;
	}

	public boolean isForcedChunksArmed() {
		return forcedChunksArmed;
	}

	public void markForcedChunksArmed() {
		if (!forcedChunksArmed) {
			forcedChunksArmed = true;
			setDirty();
		}
	}

	/** Example format from design: С231А */
	public static String generateSubjectId() {
		Random random = new Random();
		char prefix = CYR_PREFIX[random.nextInt(CYR_PREFIX.length)];
		int digits = 100 + random.nextInt(900);
		char suffix = CYR_SUFFIX[random.nextInt(CYR_SUFFIX.length)];
		return "" + prefix + digits + suffix;
	}
}
