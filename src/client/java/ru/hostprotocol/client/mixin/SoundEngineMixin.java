package ru.hostprotocol.client.mixin;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hostprotocol.client.IntroClientState;

import java.util.List;
import java.util.Map;

/**
 * Keep intro UI (MASTER) voices alive while the pause screen freezes the rest of the game.
 */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {
	@Shadow
	@Final
	private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

	@Shadow
	@Final
	private Multimap<SoundSource, SoundInstance> instanceBySource;

	@Shadow
	@Final
	private List<TickableSoundInstance> tickingSounds;

	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void hostprotocol$muteWorldDuringIntro(SoundInstance instance, CallbackInfo ci) {
		if (shouldMute(instance)) {
			ci.cancel();
		}
	}

	@Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
	private void hostprotocol$muteDelayedWorldDuringIntro(SoundInstance instance, int delay, CallbackInfo ci) {
		if (shouldMute(instance)) {
			ci.cancel();
		}
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void hostprotocol$keepIntroVoicesWhilePaused(boolean paused, CallbackInfo ci) {
		if (!paused || !IntroClientState.isFreezeActive()) {
			return;
		}
		for (TickableSoundInstance sound : this.tickingSounds) {
			if (sound.getSource() == SoundSource.MASTER) {
				sound.tick();
			}
		}
		for (SoundInstance instance : this.instanceBySource.get(SoundSource.MASTER)) {
			ChannelAccess.ChannelHandle handle = this.instanceToChannel.get(instance);
			if (handle != null) {
				handle.execute(Channel::unpause);
			}
		}
	}

	private static boolean shouldMute(SoundInstance instance) {
		if (instance == null) {
			return false;
		}
		if (ru.hostprotocol.client.fx.HorrorClientFx.isSilenced() && instance.getSource() != SoundSource.MASTER) {
			return true;
		}
		if (!IntroClientState.isFreezeActive()) {
			return false;
		}
		return instance.getSource() != SoundSource.MASTER;
	}
}
