package ru.hostprotocol.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.hostprotocol.infection.SepticLinkController;

/**
 * Belt-and-suspenders for the Fabric {@code ALLOW_SLEEPING} event: cancel the bed use on the
 * server before vanilla can start the sleep animation.
 */
@Mixin(BedBlock.class)
public abstract class BedBlockMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void hostprotocol$septicBed(
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			InteractionHand hand,
			BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (level.isClientSide()) {
			return;
		}
		if (SepticLinkController.tryInterceptSleep(player)) {
			cir.setReturnValue(InteractionResult.SUCCESS);
		}
	}
}
