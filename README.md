# Host Protocol (Fabric 1.20.1)

Мод **Host Protocol** — интро MVP: заставка, титры с Испытуемым (ID на мир), текст «Ошибка подключения к системе…».

- **modid:** `hostprotocol`
- **Minecraft:** 1.20.1
- **Loader:** Fabric
- **Java:** 17+

## Требования

1. **JDK 21** для Gradle/Loom (сборка). Игра/мод компилируются под **Java 17** (`options.release = 17`).
2. Интернет при первом билде (Gradle скачает Minecraft, mappings, Fabric).
3. Fabric API 1.20.1 в папке `mods`.

Проверка Java:

```bat
java -version
```

Нужен **21.x** (или новее) в PATH для `gradlew`. Если есть только JDK 17 — поставьте Temurin 21; мод всё равно рассчитан на MC 1.20.1 / Java 17 runtime.

## Сборка

В корне проекта (PowerShell или cmd):

```bat
.\gradlew.bat build
```

Готовый jar:

```
build\libs\hostprotocol-0.1.0-mvp.jar
```

(имя может чуть отличаться — смотрите `build\libs\`).

Скопируйте jar в `.minecraft\mods` вместе с **Fabric API** для 1.20.1.

## Запуск из IDE / Gradle

Клиент разработки:

```bat
.\gradlew.bat runClient
```

## Что делает интро MVP

1. При входе в мир (один раз на мир) — чёрный экран с **HOST PROTOCOL**.
2. «Нажмите, чтобы продолжить».
3. Титры на русском с **Испытуемый {ID}** (ID вида `С231А`, сохраняется в данных мира).
4. «Ошибка подключения к системе…», затем вход в игру.
5. Повторно в том же мире интро не показывается.

Голосовые `SoundEvent` зарегистрированы; положите OGG в:

```
src/main/resources/assets/hostprotocol/sounds/voice/
  intro_title.ogg
  intro_credits.ogg
  connection_error.ogg
```

## Структура

```
src/main/java/ru/hostprotocol/     — сервер/общая логика, данные мира, звуки, сеть
src/client/java/ru/hostprotocol/   — клиент, IntroScreen
src/main/resources/assets/hostprotocol/lang/ — ru_ru + en_us
```

## Git

Репозиторий на GitHub пока не создавался. Когда будете готовы:

```bat
git init
git add .
git commit -m "Initial Host Protocol intro MVP"
git remote add origin <url>
git push -u origin main
```

## Дизайн

См. `docs/HOST_PROTOCOL_DESIGN.md` и `INTRO_SCRIPT_RU.txt`.
