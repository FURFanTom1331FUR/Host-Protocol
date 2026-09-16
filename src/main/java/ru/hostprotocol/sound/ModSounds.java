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
	public static final SoundEvent VOICE_MATERIALIZE = create("voice.materialize");
	public static final SoundEvent VOICE_GLITCH_HIT = create("voice.glitch_hit");
	public static final SoundEvent VOICE_GLITCH_STATIC = create("voice.glitch_static");
	public static final SoundEvent VOICE_MATERIALIZE_HUM = create("voice.materialize_hum");

	private ModSounds() {}

	private static SoundEvent create(String path) {
		ResourceLocation id = HostProtocolMod.id(path);
		return SoundEvent.createVariableRangeEvent(id);
	}

	public static void register() {
		register(VOICE_INTRO_TITLE);
		register(VOICE_INTRO_CREDITS);
		register(VOICE_CONNECTION_ERROR);
		register(VOICE_MATERIALIZE);
		register(VOICE_GLITCH_HIT);
		register(VOICE_GLITCH_STATIC);
		register(VOICE_MATERIALIZE_HUM);
	}

	private static void register(SoundEvent event) {
		Registry.register(BuiltInRegistries.SOUND_EVENT, event.getLocation(), event);
	}
}
