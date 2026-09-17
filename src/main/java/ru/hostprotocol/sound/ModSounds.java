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
	public static final SoundEvent VOICE_SYSTEM_ERROR = create("voice.system_error");
	public static final SoundEvent VOICE_SYSTEM_ERROR_SHORT = create("voice.system_error_short");
	public static final SoundEvent VOICE_BUNKER_SIREN = create("voice.bunker_siren");
	public static final SoundEvent VOICE_COORDS_ANNOUNCE = create("voice.coords_announce");
	public static final SoundEvent VOICE_SEPTIC_LINK = create("voice.septic_link");
	public static final SoundEvent VOICE_SEPTIC_DROP = create("voice.septic_drop");
	public static final SoundEvent VOICE_SIGNAL_LOST = create("voice.signal_lost");
	public static final SoundEvent VOICE_SIGNAL_LOST_TONE = create("voice.signal_lost_tone");
	public static final SoundEvent SCAN_HUM = create("scan.hum");
	public static final SoundEvent SCAN_COMPLETE = create("scan.complete");
	public static final SoundEvent SCAN_FAIL = create("scan.fail");
	public static final SoundEvent VOICE_BLUEPRINT_UPDATE = create("voice.blueprint_update");
	public static final SoundEvent VOICE_TRANSFER_COMPLETE = create("voice.transfer_complete");
	public static final SoundEvent VOICE_MODULE_ONLINE = create("voice.module_online");
	public static final SoundEvent VOICE_SEPTIC_WHAT = create("voice.septic_what");
	public static final SoundEvent VOICE_WHISPER_AMBIENCE = create("voice.whisper_ambience");
	public static final SoundEvent HORROR_IMPACT = create("horror.impact");
	public static final SoundEvent HORROR_STATIC = create("horror.static_burst");
	public static final SoundEvent HORROR_DRIP = create("horror.drip");
	public static final SoundEvent HORROR_HEARTBEAT = create("horror.heartbeat");
	public static final SoundEvent HORROR_WHISPER_BED = create("horror.whisper_bed");

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
		register(VOICE_SYSTEM_ERROR);
		register(VOICE_SYSTEM_ERROR_SHORT);
		register(VOICE_BUNKER_SIREN);
		register(VOICE_COORDS_ANNOUNCE);
		register(VOICE_SEPTIC_LINK);
		register(VOICE_SEPTIC_DROP);
		register(VOICE_SIGNAL_LOST);
		register(VOICE_SIGNAL_LOST_TONE);
		register(SCAN_HUM);
		register(SCAN_COMPLETE);
		register(SCAN_FAIL);
		register(VOICE_BLUEPRINT_UPDATE);
		register(VOICE_TRANSFER_COMPLETE);
		register(VOICE_MODULE_ONLINE);
		register(VOICE_SEPTIC_WHAT);
		register(VOICE_WHISPER_AMBIENCE);
		register(HORROR_IMPACT);
		register(HORROR_STATIC);
		register(HORROR_DRIP);
		register(HORROR_HEARTBEAT);
		register(HORROR_WHISPER_BED);
	}

	private static void register(SoundEvent event) {
		Registry.register(BuiltInRegistries.SOUND_EVENT, event.getLocation(), event);
	}
}
