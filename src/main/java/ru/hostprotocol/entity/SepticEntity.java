package ru.hostprotocol.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import ru.hostprotocol.sound.ModSounds;

/**
 * Day-5 Septic presence. Watches. Does not run a full boss fight in this slice.
 */
public class SepticEntity extends Monster {
	private static final EntityDataAccessor<Boolean> HORROR_STALKER = SynchedEntityData.defineId(SepticEntity.class, EntityDataSerializers.BOOLEAN);
	private int stalkerTicks;

	public SepticEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
		this.xpReward = 0;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(HORROR_STALKER, false);
	}

	public void markHorrorStalker(int lifeTicks) {
		this.stalkerTicks = Math.max(1, lifeTicks);
		this.entityData.set(HORROR_STALKER, true);
		this.setNoGravity(true);
		this.setSilent(true);
		this.setInvulnerable(true);
		this.setNoAi(true);
	}

	public boolean isHorrorStalker() {
		return this.entityData.get(HORROR_STALKER) || this.stalkerTicks > 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.stalkerTicks > 0) {
			this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			this.stalkerTicks--;
			if (this.stalkerTicks <= 0 || this.level().players().isEmpty()) {
				this.discard();
			}
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return !isHorrorStalker() && super.shouldBeSaved();
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return isHorrorStalker();
	}

	@Override
	public boolean isInvulnerableTo(DamageSource damageSource) {
		return isHorrorStalker() || super.isInvulnerableTo(damageSource);
	}

	@Override
	public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (!isHorrorStalker()) {
			tag.putInt("HorrorStalker", 0);
		}
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.stalkerTicks = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 80.0)
				.add(Attributes.MOVEMENT_SPEED, 0.18)
				.add(Attributes.FOLLOW_RANGE, 48.0)
				.add(Attributes.ATTACK_DAMAGE, 0.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.65);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 48.0F, 1.0F));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.45));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isHorrorStalker() ? null : ModSounds.VOICE_WHISPER_AMBIENCE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSource) {
		return ModSounds.VOICE_GLITCH_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.VOICE_GLITCH_STATIC;
	}

	@Override
	public float getVoicePitch() {
		return 0.55F;
	}

	@Override
	protected float getSoundVolume() {
		return isHorrorStalker() ? 0.0F : 0.85F;
	}

	@Override
	public boolean isPushable() {
		return !isHorrorStalker() && super.isPushable();
	}

	@Override
	protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) {
		return 2.12F;
	}
}
