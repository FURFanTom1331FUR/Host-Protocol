package ru.hostprotocol.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.client.ProtocolClientState;
import ru.hostprotocol.client.sound.LoopingUiSound;
import ru.hostprotocol.entity.SepticEntity;
import ru.hostprotocol.progress.SepticPresenceLogic;
import ru.hostprotocol.sound.ModSounds;

/**
 * Day-5 Septic: whispers, light glitches, look-vignette with eyes, «Что ты такое» VO.
 */
public final class SepticPresenceClientFx {
	private static final ResourceLocation VIGNETTE = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/gui/septic_vignette.png");
	private static final ResourceLocation EYES = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/gui/septic_eyes.png");

	private static int whatCooldown;
	private static boolean looking;
	private static float lookBlend;
	private static LoopingUiSound whispers;
	private static SepticEntity nearest;

	private SepticPresenceClientFx() {}

	public static boolean isLooking() {
		return looking;
	}

	public static void cancel(Minecraft minecraft) {
		looking = false;
		lookBlend = 0.0F;
		nearest = null;
		stopWhispers(minecraft);
	}

	public static void clientTick(Minecraft minecraft) {
		if (whatCooldown > 0) {
			whatCooldown--;
		}
		if (minecraft == null || minecraft.player == null || minecraft.level == null) {
			looking = false;
			stopWhispers(minecraft);
			return;
		}
		Player player = minecraft.player;
		nearest = findNearest(minecraft, player);
		boolean day5 = ProtocolClientState.syncedDay() >= SepticPresenceLogic.FIRST_DAY
				|| ProtocolClientState.isSepticPresent();
		looking = nearest != null && isLookingAt(player, nearest);
		lookBlend = Mth.clamp(lookBlend + (looking ? 0.12F : -0.08F), 0.0F, 1.0F);

		boolean whisper = day5 && (nearest != null && player.distanceTo(nearest) < 48.0F || ProtocolClientState.isSepticPresent());
		if (whisper) {
			ensureWhispers(minecraft);
		} else {
			stopWhispers(minecraft);
		}

		if (looking && whatCooldown <= 0) {
			whatCooldown = SepticPresenceLogic.WHAT_COOLDOWN_TICKS;
			try {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_SEPTIC_WHAT, 1.0F, 1.0F));
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_HIT, 0.45F, 0.9F));
			} catch (Exception ignored) {
				// ignore
			}
		}
	}

	public static void render(GuiGraphics graphics, Minecraft minecraft) {
		if (minecraft == null) {
			return;
		}
		int w = minecraft.getWindow().getGuiScaledWidth();
		int h = minecraft.getWindow().getGuiScaledHeight();
		boolean day5 = ProtocolClientState.syncedDay() >= SepticPresenceLogic.FIRST_DAY
				|| ProtocolClientState.isSepticPresent();
		float near = 0.0F;
		if (nearest != null && minecraft.player != null) {
			float d = minecraft.player.distanceTo(nearest);
			near = Mth.clamp(1.0F - d / 40.0F, 0.0F, 1.0F);
		}
		float glitch = 0.0F;
		if (day5) {
			glitch = 0.08F + 0.22F * near + 0.45F * lookBlend;
		}
		if (glitch > 0.02F) {
			GlitchRenderer.render(graphics, w, h, minecraft.gui.getGuiTicks(), glitch);
		}
		if (lookBlend <= 0.02F) {
			return;
		}
		int a = (int) (230 * lookBlend);
		graphics.setColor(1.0F, 1.0F, 1.0F, lookBlend);
		graphics.blit(VIGNETTE, 0, 0, w, h, 0.0F, 0.0F, 256, 256, 256, 256);
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		int ew = 96;
		int eh = 48;
		int ex = (w - ew) / 2 + GlitchRenderer.textJitterX(minecraft.gui.getGuiTicks()) / 2;
		int ey = h / 2 - 48 + GlitchRenderer.textJitterY(minecraft.gui.getGuiTicks()) / 2;
		graphics.setColor(1.0F, 1.0F, 1.0F, lookBlend);
		graphics.blit(EYES, ex, ey, ew, eh, 0.0F, 0.0F, 64, 32, 64, 32);
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		if (whatCooldown > SepticPresenceLogic.WHAT_COOLDOWN_TICKS - 80) {
			graphics.drawCenteredString(minecraft.font,
					net.minecraft.network.chat.Component.translatable("hostprotocol.septic.what"),
					w / 2, h / 2 + 28, GlitchRenderer.withAlpha(0xE8E0E0, a));
			graphics.drawCenteredString(minecraft.font,
					net.minecraft.network.chat.Component.translatable("hostprotocol.septic.what.sub"),
					w / 2, h / 2 + 40, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, a));
		}
	}

	private static SepticEntity findNearest(Minecraft minecraft, Player player) {
		SepticEntity best = null;
		double bestD = Double.MAX_VALUE;
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (entity instanceof SepticEntity septic && septic.isAlive()) {
				double d = player.distanceToSqr(septic);
				if (d < bestD) {
					bestD = d;
					best = septic;
				}
			}
		}
		return best;
	}

	private static boolean isLookingAt(Player player, SepticEntity septic) {
		Vec3 eye = player.getEyePosition();
		Vec3 target = septic.getEyePosition();
		double dist = eye.distanceTo(target);
		Vec3 look = player.getViewVector(1.0F);
		Vec3 to = target.subtract(eye);
		if (to.lengthSqr() < 1.0E-6) {
			return false;
		}
		double dot = look.normalize().dot(to.normalize());
		if (!SepticPresenceLogic.isLookingAt(dot, dist)) {
			return false;
		}
		HitResult hit = player.level().clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceTo(eye) >= dist - 0.6;
	}

	private static void ensureWhispers(Minecraft minecraft) {
		if (whispers != null) {
			return;
		}
		try {
			whispers = new LoopingUiSound(ModSounds.VOICE_WHISPER_AMBIENCE, 0.45F);
			minecraft.getSoundManager().play(whispers);
		} catch (Exception ignored) {
			whispers = null;
		}
	}

	private static void stopWhispers(Minecraft minecraft) {
		if (whispers != null) {
			whispers.requestStop();
			whispers = null;
		}
		if (minecraft != null) {
			minecraft.getSoundManager().stop(ModSounds.VOICE_WHISPER_AMBIENCE.getLocation(), net.minecraft.sounds.SoundSource.MASTER);
		}
	}
}
