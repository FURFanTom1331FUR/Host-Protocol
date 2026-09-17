package ru.hostprotocol.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import ru.hostprotocol.HostProtocolMod;

public final class ModEntityTypes {
	public static final EntityType<SepticEntity> SEPTIC = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			HostProtocolMod.id("septic"),
			EntityType.Builder.of(SepticEntity::new, MobCategory.MONSTER)
					.sized(0.7F, 2.0F)
					.clientTrackingRange(12)
					.fireImmune()
					.build("septic")
	);

	private ModEntityTypes() {}

	public static void register() {
		FabricDefaultAttributeRegistry.register(SEPTIC, SepticEntity.createAttributes());
	}
}
