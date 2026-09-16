package ru.hostprotocol.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hostprotocol.client.IntroClientState;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
	@Inject(method = "tickEntities", at = @At("HEAD"), cancellable = true)
	private void hostprotocol$freezeClientEntities(CallbackInfo ci) {
		if (IntroClientState.isFreezeActive()) {
			ci.cancel();
		}
	}

	@Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
	private void hostprotocol$freezeClientEntity(Entity entity, CallbackInfo ci) {
		if (IntroClientState.isFreezeActive()) {
			ci.cancel();
		}
	}
}
