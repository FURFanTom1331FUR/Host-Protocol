package ru.hostprotocol.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import ru.hostprotocol.scan.ScanHit;
import ru.hostprotocol.scan.ScanRay;
import ru.hostprotocol.scan.ScanService;
import ru.hostprotocol.sound.ModSounds;

import java.util.List;

/**
 * Field scanner: hold right-click toward an ore or living target.
 */
public class ScannerItem extends Item {
	public ScannerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		ScanHit hit = ScanRay.pick(player);
		if (hit.isEmpty()) {
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.displayClientMessage(Component.translatable("hostprotocol.scan.fail.none")
						.withStyle(ChatFormatting.DARK_PURPLE), true);
				serverPlayer.playNotifySound(ModSounds.SCAN_FAIL, SoundSource.PLAYERS, 0.55F, 0.7F);
			}
			return InteractionResultHolder.fail(stack);
		}
		if (player instanceof ServerPlayer serverPlayer && !ScanService.canBegin(serverPlayer, hit)) {
			serverPlayer.displayClientMessage(Component.translatable("hostprotocol.scan.ore.already")
					.withStyle(ChatFormatting.DARK_PURPLE), true);
			serverPlayer.playNotifySound(ModSounds.SCAN_FAIL, SoundSource.PLAYERS, 0.7F, 0.75F);
			return InteractionResultHolder.fail(stack);
		}
		if (player instanceof ServerPlayer serverPlayer) {
			ScanService.tryBegin(serverPlayer, hit);
		}
		player.startUsingItem(hand);
		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
		if (entity instanceof ServerPlayer player) {
			ScanService.onUseTick(player);
		}
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (entity instanceof ServerPlayer player) {
			ScanService.onReleased(player);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("hostprotocol.scanner.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
