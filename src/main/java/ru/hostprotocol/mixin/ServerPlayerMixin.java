package ru.hostprotocol.mixin;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.hostprotocol.infection.SepticLinkController;

/**
 * Intercepts {@code startSleepInBed} before vanilla night/distance checks so the Day-2
 * handshake cannot be skipped by a failed vanilla sleep (and so the player never enters
 * {@code SleepingChatScreen}, which hid the old HUD cinematic).
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
	private void hostprotocol$septicSleep(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (SepticLinkController.tryInterceptSleep(self)) {
			cir.setReturnValue(Either.left(Player.BedSleepingProblem.OTHER_PROBLEM));
		}
	}
}
