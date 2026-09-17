package ru.hostprotocol.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import ru.hostprotocol.item.ModItems;
import ru.hostprotocol.sound.ModSounds;

/**
 * Soft scan hum while the scanner is held; stops on release / look-away cancel.
 */
public final class ScanHumClient {
	private static LoopingUiSound hum;

	private ScanHumClient() {}

	public static void clientTick(Minecraft client) {
		boolean scanning = client.player != null
				&& client.player.isUsingItem()
				&& client.player.getUseItem().is(ModItems.SCANNER);
		SoundManager manager = client.getSoundManager();
		if (scanning) {
			if (hum == null || !manager.isActive(hum)) {
				hum = new LoopingUiSound(ModSounds.SCAN_HUM, 0.28F, 0.82F);
				manager.play(hum);
			}
		} else {
			stop();
		}
	}

	public static void stop() {
		if (hum != null) {
			hum.requestStop();
			hum = null;
		}
	}
}
