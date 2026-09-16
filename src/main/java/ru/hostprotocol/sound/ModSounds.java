package ru.hostprotocol.sound;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import ru.hostprotocol.HostProtocolMod;

public final class ModSounds {
	public static final SoundEvent VOICE_INTRO_TITLE = create("voice.intro_title");
	public static final SoundEvent VOICE_INTRO_CREDITS = create("voice.intro_credits");
	public static final SoundEvent VOICE_CONNECTION_ERROR = create("voice.connection_error");

	private ModSounds() {}

	private static SoundEvent create(String path) {
		ResourceLocation id = HostProtocolMod.id(path);
		return SoundEvent.createVariableRangeEvent(id);
	}

	public static void register() {
		Registry.register(BuiltInRegistries.SOUND_EVENT, HostProtocolMod.id("voice.intro_title"), VOICE_INTRO_TITLE);
		Registry.register(BuiltInRegistries.SOUND_EVENT, HostProtocolMod.id("voice.intro_credits"), VOICE_INTRO_CREDITS);
		Registry.register(BuiltInRegistries.SOUND_EVENT, HostProtocolMod.id("voice.connection_error"), VOICE_CONNECTION_ERROR);
	}
}
