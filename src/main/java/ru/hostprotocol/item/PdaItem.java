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
 * Host Protocol field unit (КПК / PDA). Opens Day-1 briefing and later protocol logs.
 */
public class PdaItem extends Item {
	public static final String TAG_SUBJECT = "SubjectId";
	public static final String TAG_DAY2 = "Day2Log";
	public static final String TAG_DAY2_COORDS = "Day2Coords";
	public static final String TAG_FOCUS_X = "FocusX";
	public static final String TAG_FOCUS_Y = "FocusY";
	public static final String TAG_FOCUS_Z = "FocusZ";
	public static final String TAG_DAY3 = "Day3Log";

	public PdaItem(Properties properties) {
		super(properties);
	}

	public static ItemStack createForSubject(String subjectId) {
		ItemStack stack = new ItemStack(ModItems.PDA);
		stack.getOrCreateTag().putString(TAG_SUBJECT, subjectId);
		return stack;
	}

	public static boolean hasDay2Log(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.getBoolean(TAG_DAY2);
	}

	public static void markDay2Log(ItemStack stack) {
		stack.getOrCreateTag().putBoolean(TAG_DAY2, true);
	}

	public static boolean hasDay2Coords(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.getBoolean(TAG_DAY2_COORDS);
	}

	public static void markDay2Coords(ItemStack stack, int x, int y, int z) {
		CompoundTag tag = stack.getOrCreateTag();
		tag.putBoolean(TAG_DAY2, true);
		tag.putBoolean(TAG_DAY2_COORDS, true);
		tag.putInt(TAG_FOCUS_X, x);
		tag.putInt(TAG_FOCUS_Y, y);
		tag.putInt(TAG_FOCUS_Z, z);
	}

	public static int getFocusX(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? 0 : tag.getInt(TAG_FOCUS_X);
	}

	public static int getFocusY(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? 0 : tag.getInt(TAG_FOCUS_Y);
	}

	public static int getFocusZ(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? 0 : tag.getInt(TAG_FOCUS_Z);
	}

	public static boolean hasDay3Log(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.getBoolean(TAG_DAY3);
	}

	public static void markDay3Log(ItemStack stack) {
		stack.getOrCreateTag().putBoolean(TAG_DAY3, true);
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
