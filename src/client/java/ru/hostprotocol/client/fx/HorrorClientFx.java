package ru.hostprotocol.client.fx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import ru.hostprotocol.HostProtocolMod;
import ru.hostprotocol.horror.HorrorEventLogic;
import ru.hostprotocol.horror.HorrorKind;
import ru.hostprotocol.sound.ModSounds;

/**
 * Screamer / fake-join / toast / invert / heartbeat overlays. No speech VO.
 */
public final class HorrorClientFx {
	private static final ResourceLocation FACE = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/gui/septic_face.png");
	private static final ResourceLocation FACE_DRIP = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/gui/septic_face_drip.png");
	private static final ResourceLocation SCREAMER = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/gui/screamer_flash.png");
	private static final ResourceLocation DRIP = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/gui/septic_drip.png");
	private static final ResourceLocation VIGNETTE = new ResourceLocation(HostProtocolMod.MOD_ID, "textures/gui/septic_vignette.png");

	private static int silenceTicks;
	private static int pendingImpact;
	private static int screamerTicks;
	private static int screamerPeak;
	private static boolean secondScreamer;
	private static int staticTicks;
	private static int toastTicks;
	private static int fakeLeaveIn = -1;
	private static int heartbeatTicks;
	private static int pdaBetrayTicks;
	private static boolean pdaBetrayQueued;
	private static boolean pdaFlashedThisSession;
	private static int lookScreamerCooldown;
	private static int continuousLook;
	private static boolean secondStareUsed;
	private static boolean firstScreamerThisGaze;

	private HorrorClientFx() {}

	public static boolean isSilenced() {
		return silenceTicks > 0;
	}

	public static boolean shouldOverlay() {
		return screamerTicks > 0 || staticTicks > 0 || toastTicks > 0 || heartbeatTicks > 0 || silenceTicks > 0;
	}

	public static boolean pdaBetrayActive() {
		return pdaBetrayTicks > 0 || pdaBetrayQueued;
	}

	public static int pdaBetrayTicks() {
		return pdaBetrayTicks;
	}

	public static void beginPdaBetrayFlash() {
		pdaBetrayQueued = true;
		pdaBetrayTicks = 80;
	}

	public static void consumePdaOpen(int day) {
		if (pdaBetrayQueued || HorrorEventLogic.shouldPdaBetray(day, pdaFlashedThisSession)) {
			pdaFlashedThisSession = true;
			pdaBetrayQueued = false;
			pdaBetrayTicks = Math.max(pdaBetrayTicks, 80);
		}
	}

	public static float fovPunch(float partialTicks) {
		if (screamerTicks <= 0) {
			return 0.0F;
		}
		float t = (screamerTicks - partialTicks) / Math.max(1, screamerPeak);
		return 22.0F * Mth.clamp(t, 0.0F, 1.0F);
	}

	public static float shake(float partialTicks) {
		if (screamerTicks <= 0 && heartbeatTicks <= 0) {
			return 0.0F;
		}
		float scream = screamerTicks <= 0 ? 0.0F : (screamerTicks - partialTicks) / Math.max(1, screamerPeak);
		float beat = heartbeatTicks <= 0 ? 0.0F : heartbeatTicks / (float) HorrorEventLogic.HEARTBEAT_DURATION_TICKS;
		return scream * 2.4F + beat * 0.45F;
	}

	public static void cancel(Minecraft minecraft) {
		silenceTicks = 0;
		pendingImpact = 0;
		screamerTicks = 0;
		staticTicks = 0;
		toastTicks = 0;
		fakeLeaveIn = -1;
		heartbeatTicks = 0;
		pdaBetrayTicks = 0;
		lookScreamerCooldown = 0;
		continuousLook = 0;
		secondStareUsed = false;
		firstScreamerThisGaze = false;
		pdaBetrayQueued = false;
		pdaFlashedThisSession = false;
	}

	public static void handlePacket(Minecraft minecraft, HorrorKind kind, int extra) {
		switch (kind) {
			case FAKE_JOIN -> playFakeJoin(minecraft);
			case FAKE_TOAST -> toastTicks = HorrorEventLogic.TOAST_DURATION_TICKS;
			case STATIC_FLASH -> playStatic(minecraft);
			case STALKER -> playStalkerCue(minecraft);
			case SCREAMER -> playScreamer(minecraft, extra != 0);
			case PDA_BETRAY -> beginPdaBetrayFlash();
			case HEARTBEAT -> playHeartbeat(minecraft);
		}
	}

	public static void onLookTick(Minecraft minecraft, int day, boolean looking) {
		if (lookScreamerCooldown > 0) {
			lookScreamerCooldown--;
		}
		if (looking) {
			continuousLook++;
		} else {
			continuousLook = 0;
			secondStareUsed = false;
			firstScreamerThisGaze = false;
		}
		if (HorrorEventLogic.shouldLookScreamer(day, looking, looking && continuousLook > 1, lookScreamerCooldown)
				&& continuousLook == 1) {
			firstScreamerThisGaze = true;
			beginSilenceThenScreamer(minecraft, false);
		} else if (HorrorEventLogic.shouldSecondStare(day, looking, continuousLook, secondStareUsed, firstScreamerThisGaze)) {
			secondStareUsed = true;
			playScreamer(minecraft, true);
		}
	}

	public static void clientTick(Minecraft minecraft) {
		if (silenceTicks > 0) {
			silenceTicks--;
			if (silenceTicks == 0 && pendingImpact > 0) {
				pendingImpact = 0;
				bang(minecraft);
			}
		}
		if (screamerTicks > 0) {
			screamerTicks--;
		}
		if (staticTicks > 0) {
			staticTicks--;
		}
		if (toastTicks > 0) {
			toastTicks--;
		}
		if (heartbeatTicks > 0) {
			heartbeatTicks--;
		}
		if (pdaBetrayTicks > 0) {
			pdaBetrayTicks--;
		}
		if (fakeLeaveIn >= 0) {
			fakeLeaveIn--;
			if (fakeLeaveIn < 0 && minecraft != null && minecraft.player != null) {
				minecraft.player.displayClientMessage(
						Component.translatable("multiplayer.player.left", Component.literal(HorrorEventLogic.FAKE_PLAYER))
								.withStyle(ChatFormatting.YELLOW),
						false);
			}
		}
	}

	public static void render(GuiGraphics graphics, Minecraft minecraft) {
		if (minecraft == null) {
			return;
		}
		int w = minecraft.getWindow().getGuiScaledWidth();
		int h = minecraft.getWindow().getGuiScaledHeight();
		int ticks = minecraft.gui.getGuiTicks();
		if (silenceTicks > 0 && (silenceTicks <= 3 || (ticks & 1) == 0)) {
			graphics.fill(0, 0, w, h, 0xFF000000);
		}
		if (staticTicks > 0) {
			renderStatic(graphics, minecraft, w, h, ticks);
		}
		if (heartbeatTicks > 0) {
			float a = heartbeatTicks / (float) HorrorEventLogic.HEARTBEAT_DURATION_TICKS;
			int pulse = (int) (90 * a);
			graphics.fill(0, 0, w, h, GlitchRenderer.withAlpha(0x2A0010, pulse));
			graphics.setColor(1.0F, 1.0F, 1.0F, a * 0.85F);
			graphics.blit(VIGNETTE, 0, 0, w, h, 0.0F, 0.0F, 256, 256, 256, 256);
			graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		}
		if (screamerTicks > 0) {
			renderScreamer(graphics, minecraft, w, h, ticks);
		}
		if (toastTicks > 0) {
			renderToast(graphics, minecraft, w);
		}
	}

	private static void beginSilenceThenScreamer(Minecraft minecraft, boolean second) {
		silenceTicks = HorrorEventLogic.SILENCE_TICKS;
		pendingImpact = 1;
		secondScreamer = second;
		lookScreamerCooldown = HorrorEventLogic.SCREAMER_COOLDOWN_TICKS;
		if (minecraft != null) {
			minecraft.getSoundManager().stop(ModSounds.VOICE_WHISPER_AMBIENCE.getLocation(), SoundSource.MASTER);
		}
	}

	private static void bang(Minecraft minecraft) {
		playScreamer(minecraft, secondScreamer);
	}

	private static void playScreamer(Minecraft minecraft, boolean second) {
		secondScreamer = second;
		screamerPeak = HorrorEventLogic.SCREAMER_DURATION_TICKS + (second ? 6 : 0);
		screamerTicks = screamerPeak;
		lookScreamerCooldown = HorrorEventLogic.SCREAMER_COOLDOWN_TICKS;
		silenceTicks = 0;
		if (minecraft == null) {
			return;
		}
		try {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HORROR_IMPACT, 0.85F, 1.0F));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HORROR_STATIC, second ? 0.7F : 1.15F, 0.55F));
		} catch (Exception ignored) {
			// missing sound pack should never crash
		}
	}

	private static void playStatic(Minecraft minecraft) {
		staticTicks = HorrorEventLogic.STATIC_DURATION_TICKS;
		if (minecraft == null) {
			return;
		}
		try {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HORROR_STATIC, 1.0F, 0.7F));
		} catch (Exception ignored) {
			// ignore
		}
	}

	private static void playStalkerCue(Minecraft minecraft) {
		if (minecraft == null) {
			return;
		}
		try {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HORROR_WHISPER_BED, 0.7F, 0.25F));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_HIT, 0.45F, 0.18F));
		} catch (Exception ignored) {
			// ignore
		}
	}

	private static void playFakeJoin(Minecraft minecraft) {
		fakeLeaveIn = HorrorEventLogic.FAKE_LEAVE_DELAY_TICKS;
		if (minecraft == null || minecraft.player == null) {
			return;
		}
		minecraft.player.displayClientMessage(
				Component.translatable("multiplayer.player.joined", Component.literal(HorrorEventLogic.FAKE_PLAYER))
						.withStyle(ChatFormatting.YELLOW),
				false);
		try {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HORROR_STATIC, 1.6F, 0.12F));
		} catch (Exception ignored) {
			// ignore
		}
	}

	public static void playHeartbeat(Minecraft minecraft) {
		heartbeatTicks = HorrorEventLogic.HEARTBEAT_DURATION_TICKS;
		if (minecraft == null) {
			return;
		}
		try {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HORROR_HEARTBEAT, 0.9F, 0.7F));
		} catch (Exception ignored) {
			// ignore
		}
	}

	private static void renderScreamer(GuiGraphics graphics, Minecraft minecraft, int w, int h, int ticks) {
		float t = screamerTicks / (float) Math.max(1, screamerPeak);
		float slam = Mth.clamp(t * 1.35F, 0.0F, 1.0F);
		graphics.fill(0, 0, w, h, GlitchRenderer.withAlpha(0x000000, (int) (255 * slam)));
		if ((ticks & 2) == 0 && t > 0.55F) {
			graphics.fill(0, 0, w, h, 0xFF000000);
		}
		int jx = GlitchRenderer.textJitterX(ticks) * 3;
		int jy = GlitchRenderer.textJitterY(ticks) * 3;
		float zoom = 1.15F + 0.55F * slam;
		int size = (int) (Math.max(w, h) * zoom);
		int x = (w - size) / 2 + jx;
		int y = (h - size) / 2 + jy;
		ResourceLocation face = secondScreamer || t > 0.45F ? SCREAMER : FACE;
		graphics.setColor(1.0F, 0.15F, 0.18F, slam * 0.75F);
		graphics.blit(face, x - 10, y, size, size, 0.0F, 0.0F, 256, 256, 256, 256);
		graphics.setColor(0.15F, 0.55F, 1.0F, slam * 0.55F);
		graphics.blit(face, x + 10, y + 4, size, size, 0.0F, 0.0F, 256, 256, 256, 256);
		graphics.setColor(1.0F, 1.0F, 1.0F, slam);
		graphics.blit(face, x, y, size, size, 0.0F, 0.0F, 256, 256, 256, 256);
		float dripV = ((ticks * 11) % 256) / 256.0F;
		graphics.setColor(1.0F, 1.0F, 1.0F, slam * 0.85F);
		graphics.blit(DRIP, 0, 0, w, h, 0.0F, dripV * 256.0F, 256, 256, 256, 256);
		graphics.blit(FACE_DRIP, x, y + (int) (8 * slam), size, size, 0.0F, 0.0F, 256, 256, 256, 256);
		graphics.setColor(1.0F, 1.0F, 1.0F, slam);
		graphics.blit(VIGNETTE, 0, 0, w, h, 0.0F, 0.0F, 256, 256, 256, 256);
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		GlitchRenderer.render(graphics, w, h, ticks, 0.55F * slam);
	}

	private static void renderStatic(GuiGraphics graphics, Minecraft minecraft, int w, int h, int ticks) {
		graphics.flush();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(
				GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
				GlStateManager.DestFactor.ZERO,
				GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		graphics.fill(0, 0, w, h, 0xFFFFFFFF);
		graphics.flush();
		RenderSystem.defaultBlendFunc();
		GlitchRenderer.render(graphics, w, h, ticks, 0.85F);
		if ((ticks & 1) == 0) {
			graphics.fill(0, 0, w, h, GlitchRenderer.withAlpha(0x000000, 180));
		}
	}

	private static void renderToast(GuiGraphics graphics, Minecraft minecraft, int w) {
		int tw = 168;
		int th = 36;
		int x = w - tw - 8;
		int y = 8;
		float slide = Mth.clamp((HorrorEventLogic.TOAST_DURATION_TICKS - toastTicks) / 6.0F, 0.0F, 1.0F);
		if (toastTicks < 8) {
			slide = toastTicks / 8.0F;
		}
		int drawY = y - (int) ((1.0F - slide) * 40);
		graphics.fill(x - 1, drawY - 1, x + tw + 1, drawY + th + 1, 0xFF7B2CBF);
		graphics.fill(x, drawY, x + tw, drawY + th, 0xF0100C12);
		graphics.blit(FACE, x + 4, drawY + 4, 28, 28, 0.0F, 0.0F, 256, 256, 256, 256);
		graphics.drawString(minecraft.font, Component.translatable("hostprotocol.horror.toast.title"),
				x + 38, drawY + 7, 0xFFFFE082, false);
		graphics.drawString(minecraft.font, Component.translatable("hostprotocol.horror.toast.desc"),
				x + 38, drawY + 18, 0xFFEDEDED, false);
	}
}
