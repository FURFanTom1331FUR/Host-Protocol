package ru.hostprotocol.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import ru.hostprotocol.client.IntroClientState;
import ru.hostprotocol.client.fx.GlitchRenderer;
import ru.hostprotocol.client.fx.MaterializeClientFx;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.sound.ModSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Intro:
 * splash → expanded credits (subject ID on line 1) → connection error → glitchy materialize → world.
 */
public class IntroScreen extends Screen {
	private enum Phase {
		SPLASH,
		CREDITS,
		ERROR,
		MATERIALIZE,
		DONE
	}

	private static final String[] CREDIT_KEYS = {
			"hostprotocol.credits.line1",
			"hostprotocol.credits.line2",
			"hostprotocol.credits.line3",
			"hostprotocol.credits.line4",
			"hostprotocol.credits.line5",
			"hostprotocol.credits.line6",
			"hostprotocol.credits.line7",
			"hostprotocol.credits.line8",
			"hostprotocol.credits.line9",
			"hostprotocol.credits.line10"
	};

	private static final int CREDITS_LINE_COUNT = CREDIT_KEYS.length;
	private static final int CREDITS_LINE_INTERVAL = 50;
	private static final int ERROR_HOLD_TICKS = 72;
	private static final int MATERIALIZE_TICKS = 70; // ~3.5s
	private static final int MATERIALIZE_MIN_SKIP_TICKS = 20;
	private static final int[] GLITCH_HIT_TICKS = {6, 16, 28, 40, 52, 64};

	private final String subjectId;
	private Phase phase = Phase.SPLASH;
	private int ticksInPhase;
	private int creditsReveal;
	private boolean voicePlayedForPhase;
	private int nextGlitchHitIndex;

	public IntroScreen(String subjectId) {
		super(Component.translatable("hostprotocol.title"));
		this.subjectId = subjectId;
	}

	@Override
	protected void init() {
		if (minecraft != null && phase == Phase.SPLASH && ticksInPhase == 0) {
			MaterializeClientFx.onIntroOpened(minecraft);
		}
	}

	@Override
	public void tick() {
		ticksInPhase++;

		if (phase == Phase.SPLASH && !voicePlayedForPhase) {
			playVoice(ModSounds.VOICE_INTRO_TITLE, 1.0F);
			voicePlayedForPhase = true;
		}

		if (phase == Phase.CREDITS) {
			if (!voicePlayedForPhase) {
				playVoice(ModSounds.VOICE_INTRO_CREDITS, 1.0F);
				voicePlayedForPhase = true;
			}
			creditsReveal = Math.min(CREDITS_LINE_COUNT, ticksInPhase / CREDITS_LINE_INTERVAL + 1);
		}

		if (phase == Phase.ERROR) {
			if (!voicePlayedForPhase) {
				playVoice(ModSounds.VOICE_CONNECTION_ERROR, 1.0F);
				voicePlayedForPhase = true;
			}
			if (ticksInPhase >= ERROR_HOLD_TICKS) {
				enterPhase(Phase.MATERIALIZE);
			}
		}

		if (phase == Phase.MATERIALIZE) {
			if (!voicePlayedForPhase) {
				playVoice(ModSounds.VOICE_MATERIALIZE, 1.0F);
				voicePlayedForPhase = true;
			}
			while (nextGlitchHitIndex < GLITCH_HIT_TICKS.length && ticksInPhase >= GLITCH_HIT_TICKS[nextGlitchHitIndex]) {
				playVoice(ModSounds.VOICE_GLITCH_HIT, 0.9F);
				nextGlitchHitIndex++;
			}
			if (ticksInPhase >= MATERIALIZE_TICKS) {
				finishIntro();
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		advance();
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		advance();
		return true;
	}

	private void advance() {
		switch (phase) {
			case SPLASH -> enterPhase(Phase.CREDITS);
			case CREDITS -> {
				if (creditsReveal < CREDITS_LINE_COUNT) {
					creditsReveal = CREDITS_LINE_COUNT;
					ticksInPhase = CREDITS_LINE_INTERVAL * CREDITS_LINE_COUNT;
				} else {
					enterPhase(Phase.ERROR);
				}
			}
			case ERROR -> enterPhase(Phase.MATERIALIZE);
			case MATERIALIZE -> {
				if (ticksInPhase >= MATERIALIZE_MIN_SKIP_TICKS) {
					finishIntro();
				}
			}
			default -> {
			}
		}
	}

	private void enterPhase(Phase next) {
		stopPhaseSounds();
		phase = next;
		ticksInPhase = 0;
		voicePlayedForPhase = false;
		if (next == Phase.CREDITS) {
			creditsReveal = 1;
		}
		if (next == Phase.MATERIALIZE) {
			nextGlitchHitIndex = 0;
			if (minecraft != null) {
				MaterializeClientFx.onMaterializeStart(minecraft);
			}
		}
	}

	private void finishIntro() {
		if (phase == Phase.DONE) {
			return;
		}
		phase = Phase.DONE;
		stopPhaseSounds();
		if (minecraft != null) {
			MaterializeClientFx.onIntroFinished(minecraft);
		}
		ClientPlayNetworking.send(ModNetworking.INTRO_COMPLETE_C2S, ModNetworking.createIntroCompletePacket());
		IntroClientState.setFreezeActive(false);
		onClose();
	}

	private void stopPhaseSounds() {
		if (minecraft == null) {
			return;
		}
		minecraft.getSoundManager().stop(ModSounds.VOICE_INTRO_TITLE.getLocation(), SoundSource.MASTER);
		minecraft.getSoundManager().stop(ModSounds.VOICE_INTRO_CREDITS.getLocation(), SoundSource.MASTER);
		minecraft.getSoundManager().stop(ModSounds.VOICE_CONNECTION_ERROR.getLocation(), SoundSource.MASTER);
		minecraft.getSoundManager().stop(ModSounds.VOICE_MATERIALIZE.getLocation(), SoundSource.MASTER);
		minecraft.getSoundManager().stop(ModSounds.VOICE_GLITCH_HIT.getLocation(), SoundSource.MASTER);
	}

	private void playVoice(SoundEvent event, float volume) {
		if (minecraft == null) {
			return;
		}
		try {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0F, volume));
		} catch (Exception ignored) {
			// Missing ogg should not crash the intro
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int centerX = this.width / 2;
		int centerY = this.height / 2;

		switch (phase) {
			case SPLASH -> {
				graphics.fill(0, 0, this.width, this.height, 0xFF000000);
				renderSplash(graphics, centerX, centerY);
			}
			case CREDITS -> {
				graphics.fill(0, 0, this.width, this.height, 0xFF000000);
				renderCredits(graphics, centerX, centerY);
			}
			case ERROR -> {
				graphics.fill(0, 0, this.width, this.height, 0xFF000000);
				renderError(graphics, centerX, centerY);
			}
			case MATERIALIZE -> renderMaterialize(graphics, centerX, centerY);
			default -> {
			}
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private void renderSplash(GuiGraphics graphics, int centerX, int centerY) {
		float pulse = 0.75F + 0.25F * Mth.sin(ticksInPhase * 0.08F);
		int alpha = (int) (pulse * 255) << 24 | 0x00FFFFFF;

		graphics.drawCenteredString(this.font, Component.translatable("hostprotocol.title"), centerX, centerY - 28, 0xFFE0E0E0);
		graphics.drawCenteredString(this.font, Component.translatable("hostprotocol.connecting"), centerX, centerY - 10, 0xFF7B2CBF);

		if (ticksInPhase > 20) {
			Component hint = Component.translatable("hostprotocol.press_to_continue");
			graphics.drawCenteredString(this.font, hint, centerX, centerY + 22, 0xFF808080 | (alpha & 0xFF000000));
		}
	}

	private void renderCredits(GuiGraphics graphics, int centerX, int centerY) {
		int maxWidth = Math.max(40, this.width - 48);
		List<FormattedCharSequence> lines = new ArrayList<>();
		for (int i = 0; i < creditsReveal && i < CREDIT_KEYS.length; i++) {
			Component line = i == 0
					? Component.translatable(CREDIT_KEYS[i], subjectId)
					: Component.translatable(CREDIT_KEYS[i]);
			lines.addAll(this.font.split(line, maxWidth));
		}

		int lineHeight = 12;
		int startY = centerY - (lines.size() * lineHeight) / 2;
		for (int i = 0; i < lines.size(); i++) {
			graphics.drawCenteredString(this.font, lines.get(i), centerX, startY + i * lineHeight, 0xFFC8C8C8);
		}

		if (creditsReveal >= CREDITS_LINE_COUNT) {
			Component hint = Component.translatable("hostprotocol.press_to_continue");
			graphics.drawCenteredString(this.font, hint, centerX, startY + lines.size() * lineHeight + 18, 0xFF666666);
		}
	}

	private void renderError(GuiGraphics graphics, int centerX, int centerY) {
		int jx = ticksInPhase > 8 ? GlitchRenderer.textJitterX(ticksInPhase) / 2 : 0;
		int jy = ticksInPhase > 8 ? GlitchRenderer.textJitterY(ticksInPhase) / 2 : 0;
		Component error = Component.translatable("hostprotocol.connection_error");
		graphics.drawCenteredString(this.font, error, centerX + jx, centerY + jy, 0xFFFF5555);
	}

	private void renderMaterialize(GuiGraphics graphics, int centerX, int centerY) {
		GlitchRenderer.render(graphics, this.width, this.height, ticksInPhase, 1.0F);
		int jx = GlitchRenderer.textJitterX(ticksInPhase);
		int jy = GlitchRenderer.textJitterY(ticksInPhase);

		graphics.drawCenteredString(this.font, Component.translatable("hostprotocol.materialize.subject", subjectId),
				centerX + jx, centerY - 10 + jy, GlitchRenderer.PURPLE);
		graphics.drawCenteredString(this.font, Component.translatable("hostprotocol.materialize.subject", subjectId),
				centerX, centerY - 10, 0xFFEDEDED);
		graphics.drawCenteredString(this.font, Component.translatable("hostprotocol.materialize"),
				centerX + jx / 2, centerY + 8 + jy / 2, 0xFFB388FF);
	}

	@Override
	public void removed() {
		super.removed();
		stopPhaseSounds();
		if (phase != Phase.DONE && minecraft != null) {
			MaterializeClientFx.cancel(minecraft);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
