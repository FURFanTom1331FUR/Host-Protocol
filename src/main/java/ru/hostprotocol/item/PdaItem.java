package ru.hostprotocol.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Host Protocol field unit (КПК / PDA). Opens a readable Day-1 briefing.
 */
public class PdaItem extends Item {
	public static final String TAG_SUBJECT = "SubjectId";

	public PdaItem(Properties properties) {
		super(properties);
	}

	public static ItemStack createForSubject(String subjectId) {
		ItemStack stack = new ItemStack(ModItems.PDA);
		stack.getOrCreateTag().putString(TAG_SUBJECT, subjectId);
		return stack;
	}

	public static String getSubjectId(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag != null && tag.contains(TAG_SUBJECT)) {
			String id = tag.getString(TAG_SUBJECT);
			if (!id.isEmpty()) {
				return id;
			}
		}
		return "—";
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide) {
			ClientItemScreens.OPEN_PDA.accept(stack);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("hostprotocol.pda.tooltip", getSubjectId(stack)).withStyle(ChatFormatting.DARK_PURPLE));
	}
}
