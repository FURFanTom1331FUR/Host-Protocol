package ru.hostprotocol.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import ru.hostprotocol.HostProtocolMod;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Per-world intro state: stable subject ID + whether splash/credits already shown.
 */
public class IntroWorldData extends SavedData {
	public static final String DATA_NAME = HostProtocolMod.MOD_ID + "_intro";

	private static final char[] CYR_PREFIX = {'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ж', 'З', 'И', 'К', 'Л', 'М', 'Н', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Э', 'Ю', 'Я'};
	private static final char[] CYR_SUFFIX = {'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ж', 'З', 'И', 'К', 'Л', 'М', 'Н', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х'};

	private String subjectId;
	private boolean introCompleted;
	private boolean pdaPlacedAtSpawn;
	private final Set<UUID> pdaRecipients = new HashSet<>();

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
		if (tag.contains("PdaRecipients", Tag.TAG_LIST)) {
			ListTag list = tag.getList("PdaRecipients", Tag.TAG_STRING);
			for (int i = 0; i < list.size(); i++) {
				try {
					data.pdaRecipients.add(UUID.fromString(list.getString(i)));
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
		ListTag recipients = new ListTag();
		for (UUID id : pdaRecipients) {
			recipients.add(StringTag.valueOf(id.toString()));
		}
		tag.put("PdaRecipients", recipients);
		return tag;
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

	/** Testing helper: play the intro again in this world. Does not reset PDA grants. */
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

	public boolean isPdaPlacedAtSpawn() {
		return pdaPlacedAtSpawn;
	}

	public void markPdaPlacedAtSpawn() {
		if (!pdaPlacedAtSpawn) {
			pdaPlacedAtSpawn = true;
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
