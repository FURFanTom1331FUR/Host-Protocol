package ru.hostprotocol.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hostprotocol.freeze.IntroFreeze;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	@Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
	private void hostprotocol$freezeEntities(Entity entity, CallbackInfo ci) {
		if (IntroFreeze.isActive()) {
			ci.cancel();
		}
	}

	@Inject(method = "tickCustomSpawners", at = @At("HEAD"), cancellable = true)
	private void hostprotocol$freezeSpawners(boolean spawnEnemies, boolean spawnFriendlies, CallbackInfo ci) {
		if (IntroFreeze.isActive()) {
			ci.cancel();
		}
	}

	@ModifyArg(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerChunkCache;tick(Ljava/util/function/BooleanSupplier;Z)V"
			),
			index = 1
	)
	private boolean hostprotocol$skipChunkSimulation(boolean tickChunks) {
		return tickChunks && !IntroFreeze.isActive();
	}
}
