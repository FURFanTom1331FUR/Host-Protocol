package ru.hostprotocol.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import ru.hostprotocol.HostProtocolMod;

public final class ModItems {
	public static final Item PDA = new PdaItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	public static final Item SCANNER = new ScannerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	private ModItems() {}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("pda"), PDA);
		Registry.register(BuiltInRegistries.ITEM, HostProtocolMod.id("scanner"), SCANNER);
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(PDA);
			entries.accept(SCANNER);
		});
	}
}
