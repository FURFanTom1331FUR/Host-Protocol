package ru.hostprotocol.client.breach;

import net.minecraft.client.Minecraft;
import ru.hostprotocol.HostProtocolMod;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * After breach: on JVM shutdown write the desktop note and try to relaunch Minecraft once.
 *
 * <p>Shutdown hooks do <em>not</em> run on SIGKILL / Task Manager "End process" / {@code kill -9}.
 * Those unclean exits leave {@code session.lock}; the next start plays a bunker siren instead.
 */
public final class BreachWatchdog {
	private static final AtomicBoolean ARMED = new AtomicBoolean(false);
	private static volatile List<String> capturedCommand;
	private static volatile File workingDir;

	private BreachWatchdog() {}

	public static void arm(Minecraft minecraft) {
		if (minecraft == null) {
			return;
		}
		BreachClientFlags.markBreached(minecraft);
		if (capturedCommand == null) {
			capturedCommand = captureLaunchCommand();
			workingDir = new File(System.getProperty("user.dir", "."));
		}
		if (!ARMED.compareAndSet(false, true)) {
			return;
		}
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (!BreachClientFlags.isDesktopWritten()) {
					BreachDesktop.writeBestEffort();
					BreachClientFlags.markDesktopWritten(minecraft);
				} else {
					BreachDesktop.writeBestEffort();
				}
			} catch (Throwable t) {
				HostProtocolMod.LOGGER.warn("Desktop note failed: {}", t.toString());
			}
			try {
				if (BreachClientFlags.consumeRelaunch(minecraft)) {
					tryRelaunch();
				}
			} catch (Throwable t) {
				HostProtocolMod.LOGGER.warn("Relaunch failed: {}", t.toString());
			}
			try {
				BreachClientFlags.onCleanShutdown(minecraft);
			} catch (Throwable ignored) {
				// ignore
			}
		}, "hostprotocol-breach-watchdog"));
		HostProtocolMod.LOGGER.info("Host Protocol breach watchdog armed (max 1 auto-relaunch)");
	}

	private static void tryRelaunch() {
		List<String> command = capturedCommand;
		if (command == null || command.isEmpty()) {
			HostProtocolMod.LOGGER.warn("No captured JVM command; skip relaunch");
			return;
		}
		try {
			ProcessBuilder builder = new ProcessBuilder(command);
			if (workingDir != null) {
				builder.directory(workingDir);
			}
			builder.redirectErrorStream(true);
			builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
			builder.start();
			HostProtocolMod.LOGGER.info("Best-effort Minecraft relaunch started: {}", command.get(0));
		} catch (Exception e) {
			HostProtocolMod.LOGGER.warn("Best-effort relaunch threw: {}", e.toString());
		}
	}

	static List<String> captureLaunchCommand() {
		List<String> line = new ArrayList<>();
		String javaHome = System.getProperty("java.home");
		String javaBin = javaHome == null ? "java" : javaHome + File.separator + "bin" + File.separator + "java";
		line.add(javaBin);
		try {
			line.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
		} catch (Exception ignored) {
			// ignore
		}
		ProcessHandle.current().info().arguments().ifPresent(args -> {
			// Prefer the live process command line when the OS exposes it.
			if (args.length > 0 && line.size() == 1) {
				line.addAll(Arrays.asList(args));
			}
		});
		if (line.size() == 1) {
			String cp = System.getProperty("java.class.path", "");
			String sun = System.getProperty("sun.java.command", "");
			if (!cp.isBlank()) {
				line.add("-cp");
				line.add(cp);
			}
			if (!sun.isBlank()) {
				line.addAll(Arrays.asList(sun.split(" ")));
			}
		}
		return line;
	}
}
