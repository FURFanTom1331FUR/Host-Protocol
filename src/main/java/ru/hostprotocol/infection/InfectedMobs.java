package ru.hostprotocol.infection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import ru.hostprotocol.block.ModBlockTags;
import ru.hostprotocol.data.IntroWorldData;
import ru.hostprotocol.effect.ModEffects;
import ru.hostprotocol.entity.SepticEntity;
import ru.hostprotocol.freeze.IntroFreeze;
import ru.hostprotocol.item.ModItems;
import ru.hostprotocol.progress.InfectedMobLogic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tags living things near the infection focus. Drops infected flesh on death.
 */
public final class InfectedMobs {
	public static final String TAG = "hostprotocol.infected";
	public static final String TAG_STAGE2 = "hostprotocol.infected.stage2";

	private static final Map<UUID, Integer> INFECTED_TICKS = new ConcurrentHashMap<>();

	private InfectedMobs() {}

	public static boolean isInfected(LivingEntity entity) {
		if (entity == null) {
			return false;
		}
		if (entity.hasEffect(ModEffects.INFECTION)) {
			return true;
		}
		return entity.getTags().contains(TAG);
	}

	public static int stageOf(LivingEntity entity) {
		if (entity == null || !isInfected(entity)) {
			return 0;
		}
		if (entity.getTags().contains(TAG_STAGE2)) {
			return 2;
		}
		MobEffectInstance effect = entity.getEffect(ModEffects.INFECTION);
		if (effect != null && effect.getAmplifier() >= 1) {
			return 2;
		}
		Integer ticks = INFECTED_TICKS.get(entity.getUUID());
		return InfectedMobLogic.stageForTicks(ticks == null ? 1 : ticks);
	}

	public static void clear(LivingEntity entity) {
		entity.removeEffect(ModEffects.INFECTION);
		entity.removeTag(TAG);
		entity.removeTag(TAG_STAGE2);
		INFECTED_TICKS.remove(entity.getUUID());
	}

	public static void tick(MinecraftServer server) {
		if (IntroFreeze.isActive()) {
			return;
		}
		ServerLevel level = server.overworld();
		IntroWorldData data = IntroWorldData.get(level);
		if (!data.isInfectionActive() || !data.hasInfectionFocus()) {
			return;
		}
		if (level.getGameTime() % 20L != 0L) {
			return;
		}
		long elapsed = Math.max(0L, level.getDayTime() - data.getInfectionStartDayTime());
		int radius = InfectedMobLogic.affectRadius(InfectionTicker.currentRadius(elapsed));
		BlockPos focus = data.getFocusPos();
		AABB box = new AABB(focus).inflate(radius);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, InfectedMobs::canInfect)) {
			int dx = entity.getBlockX() - focus.getX();
			int dy = entity.getBlockY() - focus.getY();
			int dz = entity.getBlockZ() - focus.getZ();
			if (InfectedMobLogic.inRadius(dx, dy, dz, radius)) {
				infect(entity, 20);
			}
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.serverLevel() != level) {
				continue;
			}
			if (level.getBlockState(player.blockPosition()).is(ModBlockTags.INFECTED)
					|| level.getBlockState(player.blockPosition().below()).is(ModBlockTags.INFECTED)) {
				infect(player, 20);
			}
		}
	}

	public static void onDeath(LivingEntity entity) {
		if (!InfectedMobLogic.shouldDropFlesh(isInfected(entity), entity instanceof Player)) {
			return;
		}
		int count = 1 + entity.getRandom().nextInt(2);
		entity.spawnAtLocation(new ItemStack(ModItems.INFECTED_FLESH, count));
		if (entity.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.PORTAL,
					entity.getX(), entity.getY() + 0.6, entity.getZ(),
					18, 0.35, 0.45, 0.35, 0.2);
			serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
					entity.getX(), entity.getY() + 0.5, entity.getZ(),
					8, 0.25, 0.3, 0.25, 0.05);
		}
		INFECTED_TICKS.remove(entity.getUUID());
	}

	public static int playerStage(ServerPlayer player, IntroWorldData data) {
		if (player == null) {
			return 0;
		}
		int tagged = stageOf(player);
		if (tagged > 0) {
			return tagged;
		}
		if (data != null && data.isInfectionActive() && data.hasInfectionFocus()) {
			int dx = player.getBlockX() - data.getFocusX();
			int dy = player.getBlockY() - data.getFocusY();
			int dz = player.getBlockZ() - data.getFocusZ();
			int radius = InfectedMobLogic.affectRadius(16);
			if (InfectedMobLogic.inRadius(dx, dy, dz, radius)) {
				return 1;
			}
		}
		return 0;
	}

	private static boolean canInfect(LivingEntity entity) {
		if (!entity.isAlive() || entity instanceof SepticEntity) {
			return false;
		}
		return entity instanceof Player || entity instanceof net.minecraft.world.entity.Mob;
	}

	private static void infect(LivingEntity entity, int addTicks) {
		entity.addTag(TAG);
		int ticks = INFECTED_TICKS.merge(entity.getUUID(), addTicks, Integer::sum);
		int stage = InfectedMobLogic.stageForTicks(ticks);
		if (entity.getTags().contains(TAG_STAGE2)) {
			stage = Math.max(stage, 2);
		}
		if (stage >= 2) {
			entity.addTag(TAG_STAGE2);
		}
		int amplifier = Math.max(0, stage - 1);
		entity.addEffect(new MobEffectInstance(ModEffects.INFECTION, 20 * 60 * 10, amplifier, true, false, true));
		if (entity instanceof net.minecraft.world.entity.monster.Monster monster && !(entity instanceof SepticEntity)) {
			net.minecraft.world.entity.player.Player nearest = monster.level().getNearestPlayer(monster, 24.0);
			if (nearest != null && monster.getTarget() == null) {
				monster.setTarget(nearest);
			}
			if (stage >= 2) {
				monster.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 80, 1, true, false, false));
				monster.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 80, 0, true, false, false));
			} else {
				monster.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 40, 0, true, false, false));
			}
		}
		if (entity.level() instanceof ServerLevel serverLevel && serverLevel.getGameTime() % 40L == 0L) {
			serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
					entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
					3, 0.2, 0.3, 0.2, 0.01);
		}
	}
}
