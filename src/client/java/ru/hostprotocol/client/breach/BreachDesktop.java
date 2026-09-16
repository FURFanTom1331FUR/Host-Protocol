package ru.hostprotocol.client.breach;

import ru.hostprotocol.HostProtocolMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Best-effort UTF-8 note on the user's Desktop after protocol breach.
 */
public final class BreachDesktop {
	public static final String FILE_NAME = "что_ты_такое.txt";

	private static final String NOTE = """
			Что ты такое.

			Протокол всё ещё открыт.
			Ты закрыл окно. Канал — нет.

			HOST PROTOCOL не выгружен.
			Испытуемый остаётся в сессии.
			Следующий запуск может быть нечистым.

			не выходи.
			""";

	private BreachDesktop() {}

	public static void writeBestEffort() {
		Path desktop = resolveDesktop();
		if (desktop == null) {
			HostProtocolMod.LOGGER.warn("Desktop folder not found; skip {}", FILE_NAME);
			return;
		}
		try {
			Files.createDirectories(desktop);
			Path file = desktop.resolve(FILE_NAME);
			Files.writeString(file, NOTE, StandardCharsets.UTF_8);
			HostProtocolMod.LOGGER.info("Wrote desktop note {}", file.toAbsolutePath());
		} catch (IOException e) {
			HostProtocolMod.LOGGER.warn("Failed to write desktop note: {}", e.toString());
		}
	}

	static Path resolveDesktop() {
		String home = System.getProperty("user.home");
		if (home == null || home.isBlank()) {
			return null;
		}
		Path[] candidates = {
				Path.of(home, "Desktop"),
				Path.of(home, "OneDrive", "Desktop"),
				Path.of(home, "OneDrive", "Рабочий стол"),
				Path.of(home, "Рабочий стол"),
				Path.of(home, "desktop")
		};
		for (Path candidate : candidates) {
			if (Files.isDirectory(candidate)) {
				return candidate;
			}
		}
		Path fallback = Path.of(home, "Desktop");
		return fallback;
	}
}
