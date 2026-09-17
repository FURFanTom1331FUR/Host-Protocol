package ru.hostprotocol.menu;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import ru.hostprotocol.HostProtocolMod;

public final class ModMenus {
	public static final MenuType<LabTableMenu> LAB_TABLE = new MenuType<>(LabTableMenu::new, FeatureFlags.VANILLA_SET);

	private ModMenus() {}

	public static void register() {
		Registry.register(BuiltInRegistries.MENU, HostProtocolMod.id("lab_table"), LAB_TABLE);
	}
}
