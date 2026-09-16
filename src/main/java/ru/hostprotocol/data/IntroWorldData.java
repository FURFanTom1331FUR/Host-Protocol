package ru.hostprotocol.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import ru.hostprotocol.HostProtocolMod;

import java.util.Random;

/**
 * Per-world intro state: stable subject ID + whether splash/credits already shown.
 */
public class IntroWorldData extends SavedData {
	public static final String DATA_NAME = HostProtocolMod.MOD_ID + "_intro";

	private static final char[] CYR_PREFIX = {'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ж', 'З', 'И', 'К', 'Л', 'М', 'Н', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Э', 'Ю', 'Я'};
	private static final char[] CYR_SUFFIX = {'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ж', 'З', 'И', 'К', 'Л', 'М', 'Н', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х'};

	private String subjectId;
	private boolean introCompleted;

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
		return new IntroWorldData(id, done);
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		tag.putString("SubjectId", subjectId);
		tag.putBoolean("IntroCompleted", introCompleted);
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

	/** Example format from design: С231А */
	public static String generateSubjectId() {
		Random random = new Random();
		char prefix = CYR_PREFIX[random.nextInt(CYR_PREFIX.length)];
		int digits = 100 + random.nextInt(900);
		char suffix = CYR_SUFFIX[random.nextInt(CYR_SUFFIX.length)];
		return "" + prefix + digits + suffix;
	}
}
