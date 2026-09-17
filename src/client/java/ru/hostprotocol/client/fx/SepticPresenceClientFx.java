package ru.hostprotocol.client.fx;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.client.ProtocolClientState;
import ru.hostprotocol.client.sound.LoopingUiSound;
import ru.hostprotocol.entity.SepticEntity;
import ru.hostprotocol.progress.SepticPresenceLogic;
import ru.hostprotocol.sound.ModSounds;

/**
 * Day-5 Septic: nearby whispers; look-at is silent and shows the eyeless face HUD.
 */
public final class SepticPresenceClientFx {
	private static final ResourceLocation FACE = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/gui/septic_face.png");
	private static final ResourceLocation FACE_DRIP = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/gui/septic_face_drip.png");

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
		if (looking) {
			lookBlend = 1.0F;
		} else {
			lookBlend = Mth.clamp(lookBlend - 0.16F, 0.0F, 1.0F);
		}

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
			glitch = 0.08F + 0.18F * near + 0.12F * lookBlend;
		}
		if (glitch > 0.02F) {
			GlitchRenderer.render(graphics, w, h, ticks, Math.min(1.0F, glitch));
		}
		if (lookBlend <= 0.02F && impact <= 0.02F) {
			return;
		}
		float cover = Mth.clamp(lookBlend * 0.92F + impact * 0.25F, 0.0F, 1.0F);
		graphics.fill(0, 0, w, h, GlitchRenderer.withAlpha(0x000000, (int) (255 * cover)));

		int jx = GlitchRenderer.textJitterX(ticks) * 2;
		int jy = GlitchRenderer.textJitterY(ticks) * 2;
		float zoom = 1.06F + 0.22F * lookBlend + 0.18F * impact;
		int size = (int) (Math.max(w, h) * zoom);
		int x = (w - size) / 2 + jx;
		int y = (h - size) / 2 + jy;
		ResourceLocation face = ((ticks / 5) & 1) == 0 ? FACE : FACE_DRIP;
		graphics.setColor(1.0F, 1.0F, 1.0F, cover);
		graphics.blit(face, x, y, size, size, 0.0F, 0.0F, 256, 256, 256, 256);
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

		if (hudTicks > 0) {
			int a = (int) (255 * Mth.clamp(hudTicks / 18.0F, 0.0F, 1.0F));
			int textY = h - 36;
			graphics.drawCenteredString(minecraft.font,
					Component.translatable("hostprotocol.septic.chat.look").withStyle(ChatFormatting.DARK_PURPLE),
					w / 2 + jx / 2, textY, GlitchRenderer.withAlpha(0xE8E0E0, a));
			graphics.drawCenteredString(minecraft.font,
					Component.translatable("hostprotocol.septic.chat.look.sub"),
					w / 2, textY + 12, GlitchRenderer.withAlpha(0xC8C8C8, a));
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
		AABB search = player.getBoundingBox().inflate(SepticPresenceLogic.LOOK_RANGE);
		for (SepticEntity septic : minecraft.level.getEntitiesOfClass(SepticEntity.class, search, Entity::isAlive)) {
			double d = player.distanceToSqr(septic);
			if (d < bestD) {
				bestD = d;
				best = septic;
			}
		}
		if (best != null) {
			return best;
		}
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
		Vec3 look = player.getViewVector(1.0F);
		double range = SepticPresenceLogic.LOOK_RANGE;
		Vec3 end = eye.add(look.scale(range));
		if (septic.getBoundingBox().inflate(SepticPresenceLogic.LOOK_BOX_INFLATE).clip(eye, end).isPresent()) {
			return eye.distanceTo(septic.position()) <= range;
		}
		Vec3 target = septic.getBoundingBox().getCenter();
		Vec3 to = target.subtract(eye);
		if (to.lengthSqr() < 1.0E-6) {
			return false;
		}
		double dist = to.length();
		double dot = look.normalize().dot(to.scale(1.0 / dist));
		return SepticPresenceLogic.isLookingAt(dot, dist);
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
