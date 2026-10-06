# AGENTS.md

This file provides guidance to AI coding agents (Claude Code, etc.) when working with code in this repository.

## Проект
- Тип: Android-приложение, пакет: `ru.fasdev.ratex`
- Язык: Kotlin
- Сборка: Gradle
- Актуальные зависимости, плагины, SDK и все их версии в [version catalog](gradle/libs.versions.toml)

```bash
./gradlew assembleDevDebug                                               # сборка debug APK (dev); prod: assembleProdDebug
./gradlew test                                                           # все unit-тесты
./gradlew :app:testDevDebugUnitTest --tests "package.ClassTest"          # один класс
./gradlew :app:testDevDebugUnitTest --tests "*ClassTest.someMethod"      # один метод
./gradlew ktlintCheck                                                    # проверка стиля (правила — в .editorconfig)
./gradlew ktlintFormat                                                   # автоисправление стиля
```

## Архитектура

Один Gradle-модуль `:app`. Внутри — фичи в пакете `ru.fasdev.ratex`, каждая делится на слои `ui` / `domain` / `data` (и `di`, если нужно). Зависимости направлены внутрь: `ui → domain ← data`; `domain` не знает про Android-классы и `data`.

- **`core`** — общее, не привязанное к бизнес-логике конкретной фичи. Новую общую вещь кладите сюда, а не в `main` и не в фичу.
- **`main`** — точка входа, собирает всё вместе: приложение, Activity, тема, навигация, корневой DI-компонент.
- **Фичи** (остальные пакеты рядом с `core` и `main`) — код конкретной предметной области. Новая предметная область — новая папка-фича.

Схема фичи:

- `domain/` — сущности, интерфейсы `boundaries/` (интеракторы и репозитории), реализации интеракторов. Репозитории в `domain` только интерфейсы.
- `data/` — реализации репозиториев, источники данных, API-клиент.
- `ui/` — MVVM+UDF: `ViewModel` отдаёт один `StateFlow` состояния, Compose-экран подписывается на него; ошибка для пользователя — поле состояния, экран показывает её и сообщает ViewModel, что она показана.
- `di/` — Dagger-модуль и компонент фичи, фабрика `ViewModelProvider.Factory`.

Тесты лежат в тех же пакетах, что и тестируемый код (`app/src/test/.../<фича>/<слой>/`).

Асинхронность — везде Kotlin Coroutines: одноразовые операции в интерфейсах `boundaries` и в API — `suspend fun`. Блокирующую работу репозитории и источники данных выполняют через `withContext(ioDispatcher)`; `CoroutineDispatcher` принимается в конструкторе (по умолчанию `Dispatchers.IO`). ViewModel запускают корутины в `viewModelScope`, ошибки ловят через `try/catch` (`CancellationException` — rethrow).

### DI (Dagger 2, KSP)

Иерархия компонентов через `dependencies`: корневой `AppComponent` (`main/di`) → компонент каждой фичи (`<фича>/di`). Это не subcomponents: дочерний компонент видит только то, что родитель явно **объявил provision-методом**. Если фиче нужна новая зависимость из `AppComponent` — добавляйте provision-метод. Новая ViewModel — `@Inject constructor` + ветка в фабрике ViewModel соответствующей фичи.

## Git flow

- `master` — стабильная ветка релизов. Напрямую в неё не коммитим; в ней только слияния из `develop` (и `hotfix/*`) и теги `X.Y.Z`.
- `develop` — ветка разработки, сюда вливаются готовые фичи.
- `feature/NN-<название>` — ветка одной фичи. Создаётся **от `develop`**, `NN-<название>` совпадает с файлом задачи в `docs-ai/planning` (например `feature/07-git_flow`).
- `hotfix/<название>` — срочное исправление `master`; создаём только при необходимости: от `master`, вливаем и в `master`, и в `develop`. Ветки `release/*` не используем.

Слияние — локально, всегда с merge-коммитом (`--no-ff`), без PR:

```bash
git switch develop && git merge --no-ff feature/NN-name -m "Merge feature/NN-name: <кратко>"   # фича готова
git switch master && git merge --no-ff develop -m "Release X.Y.Z" && git tag X.Y.Z               # релиз
```

Агентам: ветку фичи создавать от `develop`; коммитить, вливать, ставить теги и пушить только по явной просьбе пользователя.

## Релизы и сборки

- Окружения (flavor `env`): `dev` (`ru.fasdev.ratex.dev`, «Ratex Dev») и `prod` (`ru.fasdev.ratex`, «Ratex»). Типы сборки: `debug` (суффикс `.debug`, логи, отладка) и `release` (R8, без отладки). Варианты: `devDebug`, `devRelease`, `prodDebug`, `prodRelease`.
- Версия берётся из git: `versionName` = `git describe --tags --match "[0-9]*.[0-9]*.[0-9]*"`, `versionCode` = число коммитов (`git rev-list --count HEAD`). Без тегов `0.0.0-dev`. Счётчик коммитов не монотонен между ветками: release с `master` с меньшим числом коммитов, чем у ранее установленного alpha с `develop`, Android не поставит поверх (`INSTALL_FAILED_VERSION_DOWNGRADE`), нужно сначала влить `develop` в `master`.
- Тестовый билд (pre-release): тег `X.Y.Z-alpha` на коммит из `develop`. Релиз: тег `X.Y.Z` на коммит из `master`. Префикса `v` и номера нет. GitHub Actions (`publish.yml`) проверит ветку, прогонит ktlint и тесты, соберёт подписанный `prodRelease` и опубликует его в GitHub Releases.
- Повторный alpha той же версии: удалить тег (локально и на origin) и GitHub release, поставить заново.
- Подпись: release- и debug-ключи берутся из env `RATEX_<NAME>` или `keystore.properties` (в `.gitignore`), имена `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` и `DEBUG_*`. Без release-ключа падают `assemble*Release`, `package*Release`, `bundle*Release`, а также `assemble` и `build` (они включают release). Debug-ключ без настройки заменяется `~/.android/debug.keystore`. Агентам теги не ставить и не пушить.

## docs-ai

В `docs-ai/` лежит информация о работе AI в проекте: что и почему было сделано, принятые решения, отложенные задачи. Правила ведения — в `docs-ai/README.md`.

- После завершения задачи добавить запись результата сессии в `docs-ai/artifact`.
