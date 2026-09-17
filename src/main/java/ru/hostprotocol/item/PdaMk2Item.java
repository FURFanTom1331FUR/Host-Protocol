package ru.hostprotocol.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import ru.hostprotocol.progress.ProgressionService;

import java.util.List;

/**
 * Host Protocol PDA MK-II. Inactive shell until laboratory transfer with the old field PDA.
 */
public class PdaMk2Item extends PdaItem {
	public static final String TAG_ACTIVATED = "Activated";
	public static final String TAG_BOOTED = "AssistantBooted";

	public PdaMk2Item(Properties properties) {
		super(properties);
	}

	public static ItemStack createForSubject(String subjectId) {
		ItemStack stack = new ItemStack(ModItems.PDA_MK2);
		stack.getOrCreateTag().putString(TAG_SUBJECT, subjectId);
		return stack;
	}

	public static boolean isActivated(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.getBoolean(TAG_ACTIVATED);
	}

	public static boolean isBooted(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.getBoolean(TAG_BOOTED);
	}

	public static void markBooted(ItemStack stack) {
		stack.getOrCreateTag().putBoolean(TAG_BOOTED, true);
	}

	public static ItemStack createActivatedFrom(ItemStack oldPda) {
		ItemStack out = new ItemStack(ModItems.PDA_MK2);
		CompoundTag src = oldPda.getTag();
		if (src != null) {
			out.setTag(src.copy());
		}
		CompoundTag tag = out.getOrCreateTag();
		tag.putBoolean(TAG_ACTIVATED, true);
		tag.putBoolean(TAG_BOOTED, false);
		if (oldPda.getItem() instanceof PdaItem) {
			tag.putString(TAG_SUBJECT, PdaItem.getSubjectId(oldPda));
		}
		return out;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!isActivated(stack)) {
			if (!level.isClientSide) {
				player.displayClientMessage(Component.translatable("hostprotocol.pda.mk2.offline")
						.withStyle(ChatFormatting.DARK_PURPLE), true);
			}
			player.awardStat(Stats.ITEM_USED.get(this));
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
		}
		if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && !isBooted(stack)) {
			markBooted(stack);
			ProgressionService.onMk2Boot(serverPlayer, stack);
		}
		if (level.isClientSide) {
			ClientItemScreens.OPEN_PDA.accept(stack);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		if (isActivated(stack)) {
			tooltip.add(Component.translatable("hostprotocol.pda.mk2.tooltip.online", getSubjectId(stack))
					.withStyle(ChatFormatting.AQUA));
		} else {
			tooltip.add(Component.translatable("hostprotocol.pda.mk2.tooltip.offline")
					.withStyle(ChatFormatting.DARK_GRAY));
		}
	}
}
