package ru.hostprotocol.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.block.ModBlocks;

public final class ModItems {
	public static final Item PDA = new PdaItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	public static final Item PDA_MK2 = new PdaMk2Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	public static final Item SCANNER = new ScannerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	public static final Item LAB_TABLE = new BlockItem(ModBlocks.LAB_TABLE, new Item.Properties());
	public static final Item INFECTED_FLESH = new Item(new Item.Properties());
	public static final Item INFECTION_SERUM = new InfectionSerumItem(new Item.Properties().stacksTo(16));
	public static final Item PROTECTIVE_HELMET = new ArmorItem(ModArmorMaterials.PROTECTIVE, ArmorItem.Type.HELMET, new Item.Properties());
	public static final Item PROTECTIVE_CHESTPLATE = new ArmorItem(ModArmorMaterials.PROTECTIVE, ArmorItem.Type.CHESTPLATE, new Item.Properties());
	public static final Item PROTECTIVE_LEGGINGS = new ArmorItem(ModArmorMaterials.PROTECTIVE, ArmorItem.Type.LEGGINGS, new Item.Properties());
	public static final Item PROTECTIVE_BOOTS = new ArmorItem(ModArmorMaterials.PROTECTIVE, ArmorItem.Type.BOOTS, new Item.Properties());

	private ModItems() {}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("pda"), PDA);
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("pda_mk2"), PDA_MK2);
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("scanner"), SCANNER);
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("lab_table"), LAB_TABLE);
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("infected_flesh"), INFECTED_FLESH);
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("infection_serum"), INFECTION_SERUM);
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("protective_helmet"), PROTECTIVE_HELMET);
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("protective_chestplate"), PROTECTIVE_CHESTPLATE);
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("protective_leggings"), PROTECTIVE_LEGGINGS);
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("protective_boots"), PROTECTIVE_BOOTS);

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(PDA);
			entries.accept(PDA_MK2);
			entries.accept(SCANNER);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(LAB_TABLE));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> entries.accept(INFECTED_FLESH));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> entries.accept(INFECTION_SERUM));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
			entries.accept(PROTECTIVE_HELMET);
			entries.accept(PROTECTIVE_CHESTPLATE);
			entries.accept(PROTECTIVE_LEGGINGS);
			entries.accept(PROTECTIVE_BOOTS);
		});
	}
}
