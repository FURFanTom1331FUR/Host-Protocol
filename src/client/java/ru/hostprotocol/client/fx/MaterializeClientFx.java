package ru.hostprotocol.client.fx;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import ru.hostprotocol.client.sound.LoopingUiSound;
import ru.hostprotocol.sound.ModSounds;

/**
 * Client-only materialization: third-person glitch, particles, overlay fade.
 */
public final class MaterializeClientFx {
	public enum Stage {
		IDLE,
		INTRO_HIDDEN,
		MATERIALIZING,
		AFTERMATH
	}

	private static final int AFTERMATH_TICKS = 40; // ~2s particles + overlay
	private static final int CAMERA_HOLD_TICKS = 16;

	private static Stage stage = Stage.IDLE;
	private static int ticks;
	private static CameraType restoreCamera = CameraType.FIRST_PERSON;
	private static boolean cameraOverridden;
	private static LoopingUiSound hum;
	private static LoopingUiSound staticLoop;
	private static final Vector3d JITTER = new Vector3d();

	private MaterializeClientFx() {}

	public static Stage stage() {
		return stage;
	}

	public static boolean isGlitching() {
		return stage == Stage.MATERIALIZING || stage == Stage.AFTERMATH;
	}

	public static boolean shouldSkipPlayerFrame() {
		if (stage == Stage.INTRO_HIDDEN) {
			return true;
		}
		if (stage == Stage.MATERIALIZING) {
			return (ticks * 17 + 3) % 5 == 0 || (ticks & 3) == 0 && (ticks % 7 == 0);
		}
		if (stage == Stage.AFTERMATH) {
			float remaining = ticks / (float) AFTERMATH_TICKS;
			return remaining > 0.35F && (ticks % 3 == 0);
		}
		return false;
	}

	public static Vector3d playerJitter() {
		if (!isGlitching()) {
			JITTER.set(0, 0, 0);
			return JITTER;
		}
		float intensity = stage == Stage.MATERIALIZING ? 1.0F : Mth.clamp(ticks / (float) AFTERMATH_TICKS, 0.0F, 1.0F);
		double amp = 0.12 * intensity;
		JITTER.set(
				Math.sin(ticks * 1.73) * amp,
				Math.cos(ticks * 2.11) * amp * 0.35,
				Math.sin(ticks * 0.91 + 1.2) * amp
		);
		return JITTER;
	}

	public static void onIntroOpened(Minecraft minecraft) {
		stopLoops(minecraft);
		restoreCamera(minecraft);
		stage = Stage.INTRO_HIDDEN;
		ticks = 0;
	}

	public static void onMaterializeStart(Minecraft minecraft) {
		stage = Stage.MATERIALIZING;
		ticks = 0;
		forceThirdPerson(minecraft);
		if (minecraft != null) {
			hum = new LoopingUiSound(ModSounds.VOICE_MATERIALIZE_HUM, 0.85F);
			staticLoop = new LoopingUiSound(ModSounds.VOICE_GLITCH_STATIC, 0.55F);
			minecraft.getSoundManager().play(hum);
			minecraft.getSoundManager().play(staticLoop);
		}
	}

	public static void onIntroFinished(Minecraft minecraft) {
		stopLoops(minecraft);
		stage = Stage.AFTERMATH;
		ticks = AFTERMATH_TICKS;
		if (!cameraOverridden) {
			forceThirdPerson(minecraft);
		}
	}

	public static void cancel(Minecraft minecraft) {
		stopLoops(minecraft);
		restoreCamera(minecraft);
		stage = Stage.IDLE;
		ticks = 0;
	}

	public static void clientTick(Minecraft minecraft) {
		if (stage == Stage.IDLE || minecraft == null) {
			return;
		}
		if (stage == Stage.MATERIALIZING) {
			ticks++;
			return;
		}
		if (stage == Stage.AFTERMATH) {
			spawnParticles(minecraft);
			if (ticks == AFTERMATH_TICKS - CAMERA_HOLD_TICKS) {
				restoreCamera(minecraft);
			}
			ticks--;
			if (ticks <= 0) {
				restoreCamera(minecraft);
				stage = Stage.IDLE;
			}
		}
	}

	public static void renderHudOverlay(GuiGraphics graphics, Minecraft minecraft) {
		if (stage != Stage.AFTERMATH || minecraft == null) {
			return;
		}
		int w = minecraft.getWindow().getGuiScaledWidth();
		int h = minecraft.getWindow().getGuiScaledHeight();
		float t = ticks / (float) AFTERMATH_TICKS;
		GlitchRenderer.render(graphics, w, h, AFTERMATH_TICKS - ticks, t * 0.85F);
		int fade = (int) (180 * t);
		graphics.fill(0, 0, w, h, (fade << 24));
	}

	private static void spawnParticles(Minecraft minecraft) {
		Player player = minecraft.player;
		Level level = minecraft.level;
		if (player == null || level == null) {
			return;
		}
		RandomSource random = player.getRandom();
		for (int i = 0; i < 6; i++) {
			double ox = (random.nextDouble() - 0.5) * 1.4;
			double oy = random.nextDouble() * 1.9;
			double oz = (random.nextDouble() - 0.5) * 1.4;
			double x = player.getX() + ox;
			double y = player.getY() + oy;
			double z = player.getZ() + oz;
			level.addParticle(ParticleTypes.PORTAL, x, y, z,
					(random.nextDouble() - 0.5) * 0.6,
					(random.nextDouble() - 0.3) * 0.4,
					(random.nextDouble() - 0.5) * 0.6);
			if (i % 2 == 0) {
				level.addParticle(ParticleTypes.END_ROD, x, y, z,
						(random.nextDouble() - 0.5) * 0.05,
						0.04 + random.nextDouble() * 0.08,
						(random.nextDouble() - 0.5) * 0.05);
			}
			if (i % 3 == 0) {
				level.addParticle(ParticleTypes.SMOKE, x, player.getY() + 0.1, z,
						0.0, 0.02, 0.0);
			}
		}
	}

	private static void forceThirdPerson(Minecraft minecraft) {
		if (minecraft == null || cameraOverridden) {
			return;
		}
		restoreCamera = minecraft.options.getCameraType();
		minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		cameraOverridden = true;
	}

	private static void restoreCamera(Minecraft minecraft) {
		if (!cameraOverridden || minecraft == null) {
			return;
		}
		minecraft.options.setCameraType(restoreCamera);
		cameraOverridden = false;
	}

	private static void stopLoops(Minecraft minecraft) {
		if (hum != null) {
			hum.requestStop();
			hum = null;
		}
		if (staticLoop != null) {
			staticLoop.requestStop();
			staticLoop = null;
		}
		if (minecraft != null) {
			minecraft.getSoundManager().stop(ModSounds.VOICE_MATERIALIZE_HUM.getLocation(), net.minecraft.sounds.SoundSource.MASTER);
			minecraft.getSoundManager().stop(ModSounds.VOICE_GLITCH_STATIC.getLocation(), net.minecraft.sounds.SoundSource.MASTER);
		}
	}
}
