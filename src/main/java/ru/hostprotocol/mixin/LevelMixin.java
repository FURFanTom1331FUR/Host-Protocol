package ru.hostprotocol.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hostprotocol.freeze.IntroFreeze;

@Mixin(Level.class)
public abstract class LevelMixin {
	@Shadow
	public abstract boolean isClientSide();

	@Inject(method = "tickBlockEntities", at = @At("HEAD"), cancellable = true)
	private void hostprotocol$freezeBlockEntities(CallbackInfo ci) {
		if (!this.isClientSide() && IntroFreeze.isActive()) {
			ci.cancel();
		}
	}
}
