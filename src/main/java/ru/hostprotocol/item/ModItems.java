package ru.hostprotocol.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
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

	private ModItems() {}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("pda"), PDA);
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("pda_mk2"), PDA_MK2);
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("scanner"), SCANNER);
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("lab_table"), LAB_TABLE);
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(PDA);
			entries.accept(PDA_MK2);
			entries.accept(SCANNER);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(LAB_TABLE));
	}
}
