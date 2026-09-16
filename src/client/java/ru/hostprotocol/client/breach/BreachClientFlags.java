package ru.hostprotocol.client.breach;

import net.minecraft.client.Minecraft;
import ru.hostprotocol.HostProtocolMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persists protocol-breach state in the game directory so the title screen stays corrupted
 * after JVM restart. Also tracks the unclean-session marker used when force-kill skips hooks.
 */
public final class BreachClientFlags {
	public static final String DIR_NAME = "hostprotocol";
	public static final String FLAGS_NAME = "client.flags";
	public static final String SESSION_LOCK = "session.lock";

	private static boolean breached;
	private static boolean relaunchUsed;
	private static boolean desktopWritten;
	private static boolean unclean;
	private static boolean titleSirenQueued;
	private static boolean loaded;

	private BreachClientFlags() {}

	public static synchronized void load(Minecraft minecraft) {
		if (minecraft == null) {
			return;
		}
		Path dir = dir(minecraft);
		Path flags = dir.resolve(FLAGS_NAME);
		Path lock = dir.resolve(SESSION_LOCK);
		try {
			if (Files.isRegularFile(flags)) {
				Properties props = new Properties();
				try (var in = Files.newInputStream(flags)) {
					props.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
				}
				breached = Boolean.parseBoolean(props.getProperty("breached", "false"));
				relaunchUsed = Boolean.parseBoolean(props.getProperty("relaunchUsed", "false"));
				desktopWritten = Boolean.parseBoolean(props.getProperty("desktopWritten", "false"));
			}
			if (breached && Files.isRegularFile(lock)) {
				unclean = true;
				titleSirenQueued = true;
				HostProtocolMod.LOGGER.info("Host Protocol: stale session.lock — previous exit was unclean");
			}
		} catch (IOException e) {
			HostProtocolMod.LOGGER.warn("Failed to read breach flags: {}", e.toString());
		}
		loaded = true;
		if (breached) {
			writeSessionLock(minecraft);
			save(minecraft);
		}
	}

	public static synchronized void markBreached(Minecraft minecraft) {
		loadIfNeeded(minecraft);
		boolean was = breached;
		breached = true;
		if (minecraft != null) {
			writeSessionLock(minecraft);
			save(minecraft);
		}
		if (!was) {
			HostProtocolMod.LOGGER.info("Host Protocol: client breach flag armed");
		}
	}

	public static synchronized boolean isBreached() {
		return breached;
	}

	public static synchronized boolean isUnclean() {
		return unclean;
	}

	public static synchronized boolean consumeTitleSiren() {
		if (!titleSirenQueued) {
			return false;
		}
		titleSirenQueued = false;
		return true;
	}

	public static synchronized boolean consumeRelaunch(Minecraft minecraft) {
		loadIfNeeded(minecraft);
		if (relaunchUsed) {
			return false;
		}
		relaunchUsed = true;
		save(minecraft);
		return true;
	}

	public static synchronized boolean markDesktopWritten(Minecraft minecraft) {
		loadIfNeeded(minecraft);
		if (desktopWritten) {
			return false;
		}
		desktopWritten = true;
		save(minecraft);
		return true;
	}

	public static synchronized boolean isDesktopWritten() {
		return desktopWritten;
	}

	public static synchronized void onCleanShutdown(Minecraft minecraft) {
		if (minecraft == null || !breached) {
			return;
		}
		try {
			Files.deleteIfExists(dir(minecraft).resolve(SESSION_LOCK));
		} catch (IOException ignored) {
			// best-effort
		}
	}

	private static void writeSessionLock(Minecraft minecraft) {
		try {
			Path dir = dir(minecraft);
			Files.createDirectories(dir);
			Files.writeString(dir.resolve(SESSION_LOCK),
					Long.toString(ProcessHandle.current().pid()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			HostProtocolMod.LOGGER.warn("Failed to write session.lock: {}", e.toString());
		}
	}

	private static void save(Minecraft minecraft) {
		if (minecraft == null) {
			return;
		}
		try {
			Path dir = dir(minecraft);
			Files.createDirectories(dir);
			Properties props = new Properties();
			props.setProperty("breached", Boolean.toString(breached));
			props.setProperty("relaunchUsed", Boolean.toString(relaunchUsed));
			props.setProperty("desktopWritten", Boolean.toString(desktopWritten));
			try (var out = Files.newOutputStream(dir.resolve(FLAGS_NAME))) {
				props.store(new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8), "Host Protocol client breach flags");
			}
		} catch (IOException e) {
			HostProtocolMod.LOGGER.warn("Failed to write breach flags: {}", e.toString());
		}
	}

	private static void loadIfNeeded(Minecraft minecraft) {
		if (!loaded) {
			load(minecraft);
		}
	}

	public static Path dir(Minecraft minecraft) {
		return minecraft.gameDirectory.toPath().resolve(DIR_NAME);
	}
}
