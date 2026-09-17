package ru.hostprotocol.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Host Protocol PDA MK-II. Same logs as the field unit, plus a pending-transfer note.
 */
public class PdaMk2Item extends PdaItem {
	public PdaMk2Item(Properties properties) {
		super(properties);
	}

	public static ItemStack createForSubject(String subjectId) {
		ItemStack stack = new ItemStack(ModItems.PDA_MK2);
		stack.getOrCreateTag().putString(TAG_SUBJECT, subjectId);
		return stack;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("hostprotocol.pda.mk2.tooltip", getSubjectId(stack)).withStyle(ChatFormatting.AQUA));
	}
}
