package ru.hostprotocol.item;

import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Client screen openers registered from the client entrypoint (split source sets).
 */
public final class ClientItemScreens {
	public static Consumer<ItemStack> OPEN_PDA = stack -> {
	};

	private ClientItemScreens() {}
}
