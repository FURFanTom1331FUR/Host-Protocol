package ru.hostprotocol.client.fx;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
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
 * Day-5 Septic: nearby whispers, look-at horror HUD (silent — chat instead of VO).
 */
public final class SepticPresenceClientFx {
	private static final ResourceLocation VIGNETTE = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/gui/septic_vignette.png");
	private static final ResourceLocation EYES = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/gui/septic_eyes.png");

	private static int chatCooldown;
	private static int impactTicks;
	private static int hudTicks;
	private static boolean looking;
	private static boolean wasLooking;
	private static float lookBlend;
	private static LoopingUiSound whispers;
	private static SepticEntity nearest;

	private SepticPresenceClientFx() {}

	public static boolean isLooking() {
		return looking;
	}

	public static boolean shouldOverlay() {
		return lookBlend > 0.01F || impactTicks > 0 || hudTicks > 0;
	}

	public static float lookBlend() {
		return lookBlend;
	}

	public static float fovNarrow(float partialTicks) {
		float impact = impactAmount(partialTicks);
		return 14.0F * lookBlend + 10.0F * impact;
	}

	public static float shakeIntensity(float partialTicks) {
		float impact = impactAmount(partialTicks);
		return lookBlend * 0.55F + impact * 1.35F;
	}

	public static void cancel(Minecraft minecraft) {
		looking = false;
		wasLooking = false;
		lookBlend = 0.0F;
		impactTicks = 0;
		hudTicks = 0;
		nearest = null;
		stopWhispers(minecraft);
	}

	public static void clientTick(Minecraft minecraft) {
		if (chatCooldown > 0) {
			chatCooldown--;
		}
		if (impactTicks > 0) {
			impactTicks--;
		}
		if (hudTicks > 0) {
			hudTicks--;
		}
		if (minecraft == null || minecraft.player == null || minecraft.level == null) {
			looking = false;
			wasLooking = false;
			stopWhispers(minecraft);
			return;
		}
		Player player = minecraft.player;
		nearest = findNearest(minecraft, player);
		boolean day5 = ProtocolClientState.syncedDay() >= SepticPresenceLogic.FIRST_DAY
				|| ProtocolClientState.isSepticPresent();
		looking = nearest != null && isLookingAt(player, nearest);
		lookBlend = Mth.clamp(lookBlend + (looking ? 0.22F : -0.10F), 0.0F, 1.0F);

		boolean whisper = day5 && (nearest != null && player.distanceTo(nearest) < 48.0F || ProtocolClientState.isSepticPresent());
		if (whisper) {
			ensureWhispers(minecraft);
		} else {
			stopWhispers(minecraft);
		}

		if (SepticPresenceLogic.shouldAnnounceLook(looking, wasLooking, chatCooldown)) {
			chatCooldown = SepticPresenceLogic.LOOK_CHAT_COOLDOWN_TICKS;
			impactTicks = SepticPresenceLogic.LOOK_IMPACT_TICKS;
			hudTicks = SepticPresenceLogic.LOOK_HUD_TICKS;
			sendLookChat(minecraft);
		}
		wasLooking = looking;
	}

	public static void render(GuiGraphics graphics, Minecraft minecraft) {
		if (minecraft == null) {
			return;
		}
		int w = minecraft.getWindow().getGuiScaledWidth();
		int h = minecraft.getWindow().getGuiScaledHeight();
		int ticks = minecraft.gui.getGuiTicks();
		boolean day5 = ProtocolClientState.syncedDay() >= SepticPresenceLogic.FIRST_DAY
				|| ProtocolClientState.isSepticPresent();
		float near = 0.0F;
		if (nearest != null && minecraft.player != null) {
			float d = minecraft.player.distanceTo(nearest);
			near = Mth.clamp(1.0F - d / 40.0F, 0.0F, 1.0F);
		}
		float impact = impactAmount(0.0F);
		float glitch = 0.0F;
		if (day5) {
			glitch = 0.10F + 0.28F * near + 0.72F * lookBlend + 0.55F * impact;
		}
		if (glitch > 0.02F) {
			GlitchRenderer.render(graphics, w, h, ticks, Math.min(1.0F, glitch));
		}
		if (lookBlend <= 0.02F && impact <= 0.02F) {
			return;
		}
		float veil = Mth.clamp(lookBlend * 0.72F + impact * 0.35F, 0.0F, 0.88F);
		graphics.fill(0, 0, w, h, GlitchRenderer.withAlpha(0x050308, (int) (255 * veil)));
		if (impact > 0.4F && (ticks & 1) == 0) {
			graphics.fill(0, 0, w, h, GlitchRenderer.withAlpha(0x2A0018, (int) (90 * impact)));
		}

		float vigA = Mth.clamp(lookBlend * 1.35F + impact * 0.4F, 0.0F, 1.0F);
		graphics.setColor(1.0F, 1.0F, 1.0F, vigA);
		graphics.blit(VIGNETTE, 0, 0, w, h, 0.0F, 0.0F, 256, 256, 256, 256);
		graphics.setColor(0.55F, 0.0F, 0.12F, vigA * 0.55F);
		graphics.blit(VIGNETTE, -4, 2, w + 8, h + 4, 0.0F, 0.0F, 256, 256, 256, 256);
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

		int jx = GlitchRenderer.textJitterX(ticks);
		int jy = GlitchRenderer.textJitterY(ticks);
		int ew = 168;
		int eh = 84;
		int ex = (w - ew) / 2 + jx;
		int ey = h / 2 - 72 + jy;
		float eyeA = Mth.clamp(lookBlend * 1.15F + impact * 0.5F, 0.0F, 1.0F);
		graphics.setColor(1.0F, 0.12F, 0.18F, eyeA * 0.85F);
		graphics.blit(EYES, ex - 6, ey, ew, eh, 0.0F, 0.0F, 64, 32, 64, 32);
		graphics.setColor(0.25F, 0.85F, 1.0F, eyeA * 0.55F);
		graphics.blit(EYES, ex + 6, ey + 2, ew, eh, 0.0F, 0.0F, 64, 32, 64, 32);
		graphics.setColor(1.0F, 1.0F, 1.0F, eyeA);
		graphics.blit(EYES, ex, ey, ew, eh, 0.0F, 0.0F, 64, 32, 64, 32);
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

		if (hudTicks > 0) {
			int a = (int) (255 * Mth.clamp(hudTicks / 18.0F, 0.0F, 1.0F));
			graphics.drawCenteredString(minecraft.font,
					Component.translatable("hostprotocol.septic.chat.look").withStyle(ChatFormatting.DARK_PURPLE),
					w / 2 + jx / 2, h / 2 + 36, GlitchRenderer.withAlpha(0xE8E0E0, a));
			graphics.drawCenteredString(minecraft.font,
					Component.translatable("hostprotocol.septic.chat.look.sub"),
					w / 2, h / 2 + 48, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, a));
		}
	}

	private static void sendLookChat(Minecraft minecraft) {
		if (minecraft.player == null) {
			return;
		}
		minecraft.player.displayClientMessage(
				Component.translatable("hostprotocol.septic.chat.look")
						.withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
				false);
		minecraft.player.displayClientMessage(
				Component.translatable("hostprotocol.septic.chat.look.sub")
						.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC),
				false);
	}

	private static float impactAmount(float partialTicks) {
		if (impactTicks <= 0) {
			return 0.0F;
		}
		return Mth.clamp((impactTicks - partialTicks) / (float) SepticPresenceLogic.LOOK_IMPACT_TICKS, 0.0F, 1.0F);
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
