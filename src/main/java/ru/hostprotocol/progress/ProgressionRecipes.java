package ru.hostprotocol.progress;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import ru.hostprotocol.HostProtocolMod;

/**
 * Recipe-book grants when blueprints unlock. Crafting still works if the player knows the pattern.
 */
public final class ProgressionRecipes {
	public static final ResourceLocation SCANNER = HostProtocolMod.id("scanner");
	public static final ResourceLocation LAB_TABLE = HostProtocolMod.id("lab_table");
	public static final ResourceLocation INFECTION_SERUM = HostProtocolMod.id("infection_serum");
	public static final ResourceLocation PROTECTIVE_HELMET = HostProtocolMod.id("protective_helmet");
	public static final ResourceLocation PROTECTIVE_CHESTPLATE = HostProtocolMod.id("protective_chestplate");
	public static final ResourceLocation PROTECTIVE_LEGGINGS = HostProtocolMod.id("protective_leggings");
	public static final ResourceLocation PROTECTIVE_BOOTS = HostProtocolMod.id("protective_boots");

	private ProgressionRecipes() {}

	public static void unlockScanner(ServerPlayer player) {
		award(player, SCANNER);
	}

	public static void unlockLab(ServerPlayer player) {
		award(player, LAB_TABLE);
	}

	public static void unlockBase(ServerPlayer player) {
		award(player, INFECTION_SERUM, PROTECTIVE_HELMET, PROTECTIVE_CHESTPLATE, PROTECTIVE_LEGGINGS, PROTECTIVE_BOOTS);
	}

	private static void award(ServerPlayer player, ResourceLocation... ids) {
		player.awardRecipesByKey(ids);
	}
}
