package ru.hostprotocol.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import ru.hostprotocol.effect.ModEffects;
import ru.hostprotocol.infection.InfectedMobs;

/**
 * Clears Host Protocol infection from the drinker.
 */
public class InfectionSerumItem extends Item {
	public InfectionSerumItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
			player.awardStat(Stats.ITEM_USED.get(this));
		}
		if (entity instanceof Player player) {
			player.removeEffect(ModEffects.INFECTION);
			InfectedMobs.clear(player);
			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
				ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
				if (stack.isEmpty()) {
					return bottle;
				}
				if (!player.getInventory().add(bottle)) {
					player.drop(bottle, false);
				}
			}
		} else {
			entity.removeEffect(ModEffects.INFECTION);
			InfectedMobs.clear(entity);
			if (!level.isClientSide) {
				stack.shrink(1);
			}
		}
		return stack;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 32;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.DRINK;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		return ItemUtils.startUsingInstantly(level, player, hand);
	}
}
