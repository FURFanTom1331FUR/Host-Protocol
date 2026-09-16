package ru.hostprotocol.data;

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
 * Per-world Host Protocol state: subject ID, intro, PDA grants, day announcements, PDA logs.
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
	private final Map<UUID, Integer> lastAnnouncedDays = new HashMap<>();

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
		ListTag announced = new ListTag();
		for (Map.Entry<UUID, Integer> entry : lastAnnouncedDays.entrySet()) {
			CompoundTag row = new CompoundTag();
			row.putString("Id", entry.getKey().toString());
			row.putInt("Day", entry.getValue());
			announced.add(row);
		}
		tag.put("LastAnnouncedDays", announced);
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

	/** Example format from design: С231А */
	public static String generateSubjectId() {
		Random random = new Random();
		char prefix = CYR_PREFIX[random.nextInt(CYR_PREFIX.length)];
		int digits = 100 + random.nextInt(900);
		char suffix = CYR_SUFFIX[random.nextInt(CYR_SUFFIX.length)];
		return "" + prefix + digits + suffix;
	}
}
