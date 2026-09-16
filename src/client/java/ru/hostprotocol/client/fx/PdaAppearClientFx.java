package ru.hostprotocol.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.hostprotocol.client.sound.LoopingUiSound;
import ru.hostprotocol.item.ModItems;
import ru.hostprotocol.item.PdaItem;
import ru.hostprotocol.sound.ModSounds;

/**
 * Short hologram beat after intro: glitch/scanline PDA in front of the player, then it settles
 * into inventory. Does not freeze movement.
 */
public final class PdaAppearClientFx {
	private static final int DURATION_TICKS = 48;
	private static final int SETTLE_TICKS = 12;

	private static boolean queued;
	private static String queuedSubject = "—";
	private static boolean active;
	private static int ticksLeft;
	private static String subjectId = "—";
	private static LoopingUiSound staticLoop;
	private static boolean pickupPlayed;

	private PdaAppearClientFx() {}

	public static boolean isActive() {
		return active || queued;
	}

	public static void queue(String subject) {
		queued = true;
		queuedSubject = subject == null || subject.isEmpty() ? "—" : subject;
	}

	public static void cancel(Minecraft minecraft) {
		queued = false;
		stopLoop();
		active = false;
		ticksLeft = 0;
		pickupPlayed = false;
		if (minecraft != null) {
			minecraft.getSoundManager().stop(ModSounds.VOICE_GLITCH_STATIC.getLocation(), net.minecraft.sounds.SoundSource.MASTER);
		}
	}

	public static void clientTick(Minecraft minecraft) {
		if (queued && canStart()) {
			queued = false;
			begin(minecraft, queuedSubject);
		}
		if (!active || minecraft == null) {
			return;
		}
		spawnFrontParticles(minecraft);
		int elapsed = DURATION_TICKS - ticksLeft;
		if (elapsed == 10 || elapsed == 22 || elapsed == 34) {
			playUi(minecraft, 0.55F);
		}
		if (ticksLeft == SETTLE_TICKS && !pickupPlayed) {
			pickupPlayed = true;
			try {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.2F, 0.7F));
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_HIT, 1.0F, 0.45F));
			} catch (Exception ignored) {
				// missing sound must not break the cinematic
			}
		}
		ticksLeft--;
		if (ticksLeft <= 0) {
			stopLoop();
			active = false;
		}
	}

	public static void render(GuiGraphics graphics, Minecraft minecraft) {
		if (!active || minecraft == null) {
			return;
		}
		int w = minecraft.getWindow().getGuiScaledWidth();
		int h = minecraft.getWindow().getGuiScaledHeight();
		int elapsed = DURATION_TICKS - ticksLeft;
		float life = ticksLeft / (float) DURATION_TICKS;
		float intensity = ticksLeft > SETTLE_TICKS
				? 0.55F + 0.35F * Mth.sin(elapsed * 0.4F)
				: life * 1.4F;
		intensity = Mth.clamp(intensity, 0.0F, 1.0F);

		GlitchRenderer.render(graphics, w, h, elapsed, intensity * 0.75F);

		float settle = ticksLeft > SETTLE_TICKS
				? 0.0F
				: 1.0F - ticksLeft / (float) SETTLE_TICKS;
		float cx = w / 2.0F;
		float cy = h / 2.0F - 8.0F;
		float tx = w / 2.0F - 8.0F;
		float ty = h - 28.0F;
		float x = Mth.lerp(settle * settle, cx, tx);
		float y = Mth.lerp(settle, cy, ty);
		float scale = Mth.lerp(settle, 4.2F, 1.0F);
		int jx = (int) (GlitchRenderer.textJitterX(elapsed) * (1.0F - settle));
		int jy = (int) (GlitchRenderer.textJitterY(elapsed) * (1.0F - settle));

		ItemStack pda = PdaItem.createForSubject(subjectId);
		graphics.pose().pushPose();
		graphics.pose().translate(x + jx - 8.0F * scale, y + jy - 8.0F * scale, 200.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.renderItem(pda, 0, 0);
		graphics.pose().popPose();

		if (settle < 0.85F) {
			int a = (int) ((1.0F - settle) * 220);
			Component title = Component.translatable("hostprotocol.pda.appear");
			Component device = Component.translatable("hostprotocol.pda.appear.device", subjectId);
			graphics.drawCenteredString(minecraft.font, title, w / 2 + jx / 2, (int) cy + 36, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, a));
			graphics.drawCenteredString(minecraft.font, device, w / 2, (int) cy + 48, GlitchRenderer.withAlpha(0xC8C8C8, a));
		}

		int veil = (int) (40 * intensity);
		graphics.fill(0, 0, w, h, GlitchRenderer.withAlpha(GlitchRenderer.PURPLE_RGB, veil));
	}

	private static boolean canStart() {
		MaterializeClientFx.Stage stage = MaterializeClientFx.stage();
		return stage != MaterializeClientFx.Stage.INTRO_HIDDEN
				&& stage != MaterializeClientFx.Stage.MATERIALIZING;
	}

	private static void begin(Minecraft minecraft, String subject) {
		active = true;
		ticksLeft = DURATION_TICKS;
		subjectId = subject;
		pickupPlayed = false;
		stopLoop();
		if (minecraft != null) {
			staticLoop = new LoopingUiSound(ModSounds.VOICE_GLITCH_STATIC, 0.4F);
			minecraft.getSoundManager().play(staticLoop);
			try {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_MATERIALIZE, 1.15F, 0.55F));
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_HIT, 1.0F, 0.7F));
			} catch (Exception ignored) {
				// missing ogg should not crash
			}
		}
	}

	private static void playUi(Minecraft minecraft, float volume) {
		try {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VOICE_GLITCH_HIT, 1.0F, volume));
		} catch (Exception ignored) {
			// ignore
		}
	}

	private static void spawnFrontParticles(Minecraft minecraft) {
		Player player = minecraft.player;
		Level level = minecraft.level;
		if (player == null || level == null) {
			return;
		}
		Vec3 look = player.getViewVector(1.0F);
		Vec3 origin = player.getEyePosition().add(look.scale(1.35));
		RandomSource random = player.getRandom();
		for (int i = 0; i < 4; i++) {
			double ox = (random.nextDouble() - 0.5) * 0.55;
			double oy = (random.nextDouble() - 0.5) * 0.7;
			double oz = (random.nextDouble() - 0.5) * 0.55;
			level.addParticle(ParticleTypes.END_ROD,
					origin.x + ox, origin.y + oy, origin.z + oz,
					look.x * 0.02, 0.03, look.z * 0.02);
			if (i % 2 == 0) {
				level.addParticle(ParticleTypes.PORTAL,
						origin.x + ox, origin.y + oy, origin.z + oz,
						(random.nextDouble() - 0.5) * 0.4,
						(random.nextDouble() - 0.3) * 0.3,
						(random.nextDouble() - 0.5) * 0.4);
			}
		}
	}

	private static void stopLoop() {
		if (staticLoop != null) {
			staticLoop.requestStop();
			staticLoop = null;
		}
	}
}
