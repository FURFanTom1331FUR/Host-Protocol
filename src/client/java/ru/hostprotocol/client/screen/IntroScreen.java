package ru.hostprotocol.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import ru.hostprotocol.network.ModNetworking;
import ru.hostprotocol.sound.ModSounds;

/**
 * Intro MVP:
 * 1) Splash HOST PROTOCOL + «Нажмите, чтобы продолжить»
 * 2) Credits with Испытуемый {ID}
 * 3) Flavor «Ошибка подключения к системе…»
 */
public class IntroScreen extends Screen {
	private enum Phase {
		SPLASH,
		CREDITS,
		ERROR,
		DONE
	}

	private final String subjectId;
	private Phase phase = Phase.SPLASH;
	private int ticksInPhase;
	private int creditsReveal; // how many credit lines visible
	private boolean voicePlayedForPhase;

	private static final int CREDITS_LINE_INTERVAL = 40; // ticks between lines
	private static final int ERROR_HOLD_TICKS = 60;

	public IntroScreen(String subjectId) {
		super(Component.translatable("hostprotocol.title"));
		this.subjectId = subjectId;
	}

	@Override
	protected void init() {
		ticksInPhase = 0;
		creditsReveal = 0;
		voicePlayedForPhase = false;
	}

	@Override
	public void tick() {
		ticksInPhase++;

		if (phase == Phase.SPLASH && !voicePlayedForPhase) {
			playVoice(ModSounds.VOICE_INTRO_TITLE);
			voicePlayedForPhase = true;
		}

		if (phase == Phase.CREDITS) {
			if (!voicePlayedForPhase) {
				playVoice(ModSounds.VOICE_INTRO_CREDITS);
				voicePlayedForPhase = true;
			}
			int target = Math.min(6, ticksInPhase / CREDITS_LINE_INTERVAL + 1);
			creditsReveal = target;
			// After last line shown + short pause, auto-advance on click only (or after hold)
			if (creditsReveal >= 6 && ticksInPhase > CREDITS_LINE_INTERVAL * 6 + 40) {
				// wait for click; optional auto — keep click-gated for readability
			}
		}

		if (phase == Phase.ERROR) {
			if (!voicePlayedForPhase) {
				playVoice(ModSounds.VOICE_CONNECTION_ERROR);
				voicePlayedForPhase = true;
			}
			if (ticksInPhase >= ERROR_HOLD_TICKS) {
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
		// Space / Enter / Escape advance (Escape also advances rather than closing early without complete)
		advance();
		return true;
	}

	private void advance() {
		switch (phase) {
			case SPLASH -> enterPhase(Phase.CREDITS);
			case CREDITS -> {
				if (creditsReveal < 6) {
					creditsReveal = 6;
					ticksInPhase = CREDITS_LINE_INTERVAL * 6;
				} else {
					enterPhase(Phase.ERROR);
				}
			}
			case ERROR -> finishIntro();
			default -> {
			}
		}
	}

	private void enterPhase(Phase next) {
		phase = next;
		ticksInPhase = 0;
		voicePlayedForPhase = false;
		if (next == Phase.CREDITS) {
			creditsReveal = 1;
		}
	}

	private void finishIntro() {
		if (phase == Phase.DONE) {
			return;
		}
		phase = Phase.DONE;
		ClientPlayNetworking.send(ModNetworking.INTRO_COMPLETE_C2S, ModNetworking.createIntroCompletePacket());
		onClose();
	}

	private void playVoice(SoundEvent event) {
		if (minecraft == null) {
			return;
		}
		try {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0F));
		} catch (Exception ignored) {
			// Missing ogg placeholders should not crash the intro
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0xFF000000);

		int centerX = this.width / 2;
		int centerY = this.height / 2;

		switch (phase) {
			case SPLASH -> renderSplash(graphics, centerX, centerY);
			case CREDITS -> renderCredits(graphics, centerX, centerY);
			case ERROR -> renderError(graphics, centerX, centerY);
			default -> {
			}
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private void renderSplash(GuiGraphics graphics, int centerX, int centerY) {
		float pulse = 0.75F + 0.25F * Mth.sin(ticksInPhase * 0.08F);
		int alpha = (int) (pulse * 255) << 24 | 0x00FFFFFF;

		Component title = Component.translatable("hostprotocol.title");
		graphics.drawCenteredString(this.font, title, centerX, centerY - 20, 0xFFE0E0E0);

		if (ticksInPhase > 20) {
			Component hint = Component.translatable("hostprotocol.press_to_continue");
			graphics.drawCenteredString(this.font, hint, centerX, centerY + 20, 0xFF808080 | (alpha & 0xFF000000));
		}
	}

	private void renderCredits(GuiGraphics graphics, int centerX, int centerY) {
		String[] keys = {
				"hostprotocol.credits.line1",
				"hostprotocol.credits.line2",
				"hostprotocol.credits.line3",
				"hostprotocol.credits.line4",
				"hostprotocol.credits.line5",
				"hostprotocol.credits.line6"
		};

		int startY = centerY - 50;
		for (int i = 0; i < creditsReveal && i < keys.length; i++) {
			Component line;
			if (i == 0) {
				line = Component.translatable(keys[i], subjectId);
			} else {
				line = Component.translatable(keys[i]);
			}
			graphics.drawCenteredString(this.font, line, centerX, startY + i * 14, 0xFFC8C8C8);
		}

		if (creditsReveal >= 6) {
			Component hint = Component.translatable("hostprotocol.press_to_continue");
			graphics.drawCenteredString(this.font, hint, centerX, startY + 6 * 14 + 24, 0xFF666666);
		}
	}

	private void renderError(GuiGraphics graphics, int centerX, int centerY) {
		Component error = Component.translatable("hostprotocol.connection_error");
		graphics.drawCenteredString(this.font, error, centerX, centerY, 0xFFFF5555);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
