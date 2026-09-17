package ru.hostprotocol.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Protective suit stitched from infected matter. Repair with infected flesh.
 */
public enum ModArmorMaterials implements ArmorMaterial {
	PROTECTIVE("protective_suit", 18, 8, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.05F);

	private final String name;
	private final int durabilityMultiplier;
	private final int enchantmentValue;
	private final SoundEvent equipSound;
	private final float toughness;
	private final float knockbackResistance;

	ModArmorMaterials(String name, int durabilityMultiplier, int enchantmentValue, SoundEvent equipSound,
			float toughness, float knockbackResistance) {
		this.name = name;
		this.durabilityMultiplier = durabilityMultiplier;
		this.enchantmentValue = enchantmentValue;
		this.equipSound = equipSound;
		this.toughness = toughness;
		this.knockbackResistance = knockbackResistance;
	}

	@Override
	public int getDurabilityForType(ArmorItem.Type type) {
		int base = switch (type) {
			case HELMET -> 11;
			case CHESTPLATE -> 16;
			case LEGGINGS -> 15;
			case BOOTS -> 13;
		};
		return base * durabilityMultiplier;
	}

	@Override
	public int getDefenseForType(ArmorItem.Type type) {
		return switch (type) {
			case HELMET -> 2;
			case CHESTPLATE -> 6;
			case LEGGINGS -> 5;
			case BOOTS -> 2;
		};
	}

	@Override
	public int getEnchantmentValue() {
		return enchantmentValue;
	}

	@Override
	public SoundEvent getEquipSound() {
		return equipSound;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return Ingredient.of(ModItems.INFECTED_FLESH);
	}

	@Override
	public String getName() {
		return "hostprotocol:" + name;
	}

	@Override
	public float getToughness() {
		return toughness;
	}

	@Override
	public float getKnockbackResistance() {
		return knockbackResistance;
	}
}
