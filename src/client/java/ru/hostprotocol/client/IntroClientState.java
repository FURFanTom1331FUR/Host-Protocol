package ru.hostprotocol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;

/**
 * Client freeze / mute flag from join until intro completion (and during replay).
 */
public final class IntroClientState {
	private static final SoundSource[] WORLD_SOURCES = {
			SoundSource.MUSIC,
			SoundSource.RECORDS,
			SoundSource.WEATHER,
			SoundSource.BLOCKS,
			SoundSource.HOSTILE,
			SoundSource.NEUTRAL,
			SoundSource.PLAYERS,
			SoundSource.AMBIENT,
			SoundSource.VOICE
	};

	private static boolean freezeActive;

	private IntroClientState() {}

	public static boolean isFreezeActive() {
		return freezeActive;
	}

	public static void setFreezeActive(boolean active) {
		boolean wasActive = freezeActive;
		freezeActive = active;
		if (active && !wasActive) {
			suppressWorldSounds();
		}
	}

	public static void suppressWorldSounds() {
		Minecraft minecraft = Minecraft.getInstance();
		SoundManager sounds = minecraft.getSoundManager();
		for (SoundSource source : WORLD_SOURCES) {
			sounds.stop(null, source);
		}
	}
}
