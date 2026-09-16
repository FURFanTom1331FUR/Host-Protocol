# Host Protocol (Fabric 1.20.1)

Мод **Host Protocol** — интро: заставка, расширенные титры с Испытуемым (ID на мир), ошибка подключения и глитч-материализация персонажа.

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

Linux / macOS:

```bash
./gradlew build
```

Готовый jar:

```
build/libs/hostprotocol-0.1.0-mvp.jar
```

(имя может чуть отличаться — смотрите `build/libs/`).

Скопируйте jar в `.minecraft/mods` вместе с **Fabric API** для 1.20.1.

## Запуск из IDE / Gradle

Клиент разработки:

```bat
.\gradlew.bat runClient
```

```bash
./gradlew runClient
```

## Как тестировать интро (новый мир)

Интро показывается **один раз на мир**. Для полного прогона:

1. Соберите мод (`./gradlew build`) и положите jar + Fabric API 1.20.1 в `mods`.
2. Запустите Minecraft 1.20.1 (Fabric).
3. Создайте **новый мир** (не заходите в уже существующий, если интро там уже прошло).
4. Ожидаемая последовательность:
   1. Чёрный экран **HOST PROTOCOL** / «Подключение…» / «Нажмите, чтобы продолжить».
   2. Титры (строка 1 — **Испытуемый {ID}**, дальше расширенный лор).
   3. «Ошибка подключения к системе. Внедрение испытуемого…»
   4. **Материализация** (~2–4 с): scanlines, джиттер, фиолетово-чёрные вспышки (`#7b2cbf`), текст «Материализация испытуемого {ID}», голос + hum/static/glitch.
   5. Камера на мгновение в третьем лице, персонаж «собирается» с глитчем; после закрытия экрана — частицы (portal / end_rod / smoke) ~2 с и затухание оверлея.
5. Повторный вход в **тот же** мир интро не показывает.

Повтор без нового мира (для отладки):

```
/hostprotocol replayintro
```

Команда сбрасывает флаг интро у мира и запускает сцену снова.

## Голос / SFX

Зарегистрированы `SoundEvent` и лежат OGG:

```
src/main/resources/assets/hostprotocol/sounds/voice/
  intro_title.ogg
  intro_credits.ogg
  connection_error.ogg
  materialize.ogg
  glitch_hit.ogg
  glitch_static.ogg
  materialize_hum.ogg
```

## Структура

```
src/main/java/ru/hostprotocol/     — сервер/общая логика, данные мира, звуки, сеть, /hostprotocol
src/client/java/ru/hostprotocol/   — клиент, IntroScreen, глитч-FX материализации
src/main/resources/assets/hostprotocol/lang/ — ru_ru + en_us
```

## Дизайн

См. `docs/HOST_PROTOCOL_DESIGN.md` и `INTRO_SCRIPT_RU.txt`.
