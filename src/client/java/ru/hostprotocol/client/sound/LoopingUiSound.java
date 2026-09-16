package ru.hostprotocol.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * UI-relative looping clip (hum / static) that can be stopped from the intro cinematic.
 */
public class LoopingUiSound extends AbstractTickableSoundInstance {
	private boolean requestedStop;

	public LoopingUiSound(SoundEvent event, float volume) {
		super(event, SoundSource.MASTER, SoundInstance.createUnseededRandom());
		this.looping = true;
		this.delay = 0;
		this.volume = volume;
		this.pitch = 1.0F;
		this.relative = true;
		this.attenuation = Attenuation.NONE;
		this.x = 0.0;
		this.y = 0.0;
		this.z = 0.0;
	}

	@Override
	public void tick() {
		if (requestedStop) {
			this.stop();
		}
	}

	public void requestStop() {
		this.requestedStop = true;
	}
}
