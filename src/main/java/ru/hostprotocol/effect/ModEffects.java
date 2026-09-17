package ru.hostprotocol.effect;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import ru.hostprotocol.HostProtocolMod;

public final class ModEffects {
	public static final MobEffect INFECTION = new InfectionEffect();

	private ModEffects() {}

	public static void register() {
		Registry.register(BuiltInRegistries.MOB_EFFECT, HostProtocolMod.id("infection"), INFECTION);
	}

	private static final class InfectionEffect extends MobEffect {
		private InfectionEffect() {
			super(MobEffectCategory.HARMFUL, 0x7B2CBF);
		}
	}
}
