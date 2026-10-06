# AGENTS.md

This file provides guidance to AI coding agents (Claude Code, etc.) when working with code in this repository.

## Проект
- Тип: Android-приложение, пакет: `ru.fasdev.ratex`
- Язык: Kotlin
- Сборка: Gradle
- Актуальные зависимости, плагины, SDK и все их версии в [version catalog](gradle/libs.versions.toml)

```bash
./gradlew assembleDebug                                                  # сборка debug APK
./gradlew test                                                           # все unit-тесты
./gradlew :app:testDebugUnitTest --tests "package.ClassTest"             # один класс
./gradlew :app:testDebugUnitTest --tests "*ClassTest.someMethod"         # один метод
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

## docs-ai

В `docs-ai/` лежит информация о работе AI в проекте: что и почему было сделано, принятые решения, отложенные задачи. Правила ведения — в `docs-ai/README.md`.

- После завершения задачи добавить запись результата сессии в `docs-ai/artifact`.
