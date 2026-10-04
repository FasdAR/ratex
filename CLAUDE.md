# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Проект

Ratex — Android-приложение (Kotlin) со списком курсов валют относительно выбранной базовой валюты. 
Пакет `ru.fasdev.ratex`, minSdk 23, compileSdk 37, targetSdk 34. README в репозитории нет.

## Команды

Сборка через Gradle wrapper (Gradle 9.8.0, AGP 9.4.1, Kotlin 2.4.20; нужен JDK и Android SDK — путь в `local.properties`, он не коммитится).

```bash
./gradlew assembleDebug                      # сборка debug APK
./gradlew test                               # все unit-тесты
./gradlew :app:testDebugUnitTest --tests "ru.fasdev.ratex.currency.ui.bottomSheetSelectCurrency.SelectCurrencyPresenterTest"   # один класс
./gradlew :app:testDebugUnitTest --tests "*CurrencyRateRepoTest.someMethod"                                                    # один метод
./gradlew connectedDebugAndroidTest          # инструментальные UI-тесты (нужен эмулятор/устройство)
```

Стиль кода проверяет ktlint (Gradle-плагин `org.jlleitschuh.gradle.ktlint`, подключён ко всем подпроектам в корневом `build.gradle.kts`, правила — в `.editorconfig`):

```bash
./gradlew ktlintCheck     # проверка стиля всех модулей
./gradlew ktlintFormat    # автоисправление
```

Gradle не работает с Java 25 (JBR из Android Studio) — запускайте с JDK 17, например `JAVA_HOME=~/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home`.

## Архитектура

Один Gradle-модуль `:app`. Внутри — фичи в пакете `ru.fasdev.ratex`, каждая делится на слои `ui` / `domain` / `data` (и `di`, если нужно). Зависимости направлены внутрь: `ui → domain ← data`, `domain` не знает про Android-классы и `data`. Если функционал общий и не привязан к одному бизнес-юниту — он живёт в `core`.

- **`core`** — общее без привязки к бизнес-логике: `util/` (`ConvertUtil`: `dp`/`px`), `data/sharedPrefences` (`SPrefences`, `SharedPrefencesRepoImpl`), `domain/boundaries` (`SharedPrefencesRepo`), `di/module` (`SettingsModule`, `HttpClientModule`), `rule/` в тестах (`MainDispatcherRule`).
- **`currency`** — бизнес-логика валют, список курсов и базовая валюта:
  - `domain/` — сущности (`CurrencyDomain`, `RateCurrencyDomain`), интерфейсы `boundaries/` (интеракторы и репозитории), реализации интеракторов (`*InteractorImpl`). Репозитории здесь только интерфейсы.
  - `data/` — реализации репозиториев (`repo/*RepoImpl`), `dataStore/` (`CurrencyRateDataStore`, `source/ExchangeRateDataStore`), `api/ExchangeRateApi` (интерфейс) и `api/ExchangeRateApiImpl` на Ktor Client. Цепочка получения курсов: `CurrencyRateRepoImpl` берёт базовую валюту из `CurrencyBaseRepo` и передаёт её в `CurrencyRateDataStore`. Ответ API описан `@Serializable`-классом `api/model/ExchangeRatesResponse`, `ExchangeRateApi` возвращает его напрямую (`ContentNegotiation` с kotlinx-serialization в `HttpClientModule`, `Json` с `ignoreUnknownKeys = true`; `expectSuccess = true` — ответы не 2xx дают `ResponseException`).
  - `ui/` — MVP на Moxy (`MvpPresenter` + `*View` интерфейс + Fragment/BottomSheet: `fragmentListCurrencyRate`, `bottomSheetSelectCurrency`), списки на Epoxy (`adapter/`), экраны Cicerone — фабрики `FragmentScreen` (например `ListCurrencyRateScreen()`), картинки через Glide.
  - `di/` — `CurrencyModule` (Api → репозитории → интеракторы) и компоненты `FragmentListCurrencyRateComponent`, `SelectCurrencyBottomSheetComponent`.
- **`main`** — точка входа, собирает всё вместе: `RatexApp`, `ui/` (`MainActivity`, `SplashActivity`), `navigation/` (`MainNavigator`, `BackButtonProvider`, `FragmentProvider`; навигация через Cicerone 7), `di/` (`AppComponent`, `ActivityComponent`, модули `AppModule`, `ActivityModule`, `CiceroneModule`, scope-аннотации).

Новую общую вещь кладите в `core`, а не в `main` и не в конкретную фичу; код конкретной предметной области — в её фичу (или новую папку-фичу рядом с `currency`). Тесты лежат в тех же пакетах, что и тестируемый код (`app/src/test/.../<фича>/<слой>/`).

Асинхронность — везде Kotlin Coroutines: одноразовые операции в интерфейсах `boundaries` — `suspend fun`, методы `ExchangeRateApi` тоже `suspend`. Блокирующую работу (SharedPreferences, чтение/разбор ответа) репозитории и `ExchangeRateDataStore` выполняют через `withContext(ioDispatcher)` — `CoroutineDispatcher` принимается в конструкторе (по умолчанию `Dispatchers.IO`). Презентеры запускают корутины в `presenterScope` (`moxy-ktx`, отменяется в `onDestroy`), ошибки ловят через `try/catch` (с `CancellationException` — rethrow).

### DI (Dagger 2, генерация через KSP) — иерархия компонентов через `dependencies`

`AppComponent` (`@AppScope`: Context, SharedPreferences, HttpClient; пакет `main/di`) → `ActivityComponent` (`@ActivityScope`, Cicerone) → `FragmentListCurrencyRateComponent` (`currency/di`, `@FragmentScope`, модуль `CurrencyModule` собирает Api → репозитории → интеракторы) → `SelectCurrencyBottomSheetComponent` (`currency/di`, `@BottomSheetScope`; зависит от фрагментного компонента и переиспользует его интеракторы).

Это не subcomponents: дочерний компонент видит только то, что родитель явно **объявил provision-методом** (`fun httpClient(): HttpClient` и т. п.). Если дочернему компоненту нужна новая зависимость из родителя — добавляйте provision-метод в каждый промежуточный компонент. `AppComponent` хранится в `RatexApp.DI.appComponent`, `ActivityComponent` — в `MainActivity.activitySubComponent`; фрагменты строят свои компоненты лениво через `Dagger*Component.builder().activityComponent(...)`.

### Особенности сборки

- Скрипты сборки на Kotlin DSL (`*.gradle.kts`). Версии зависимостей, плагинов и SDK (`compileSdk`, `minSdk`, `targetSdk`) — в version catalog `gradle/libs.versions.toml`; репозитории — в `settings.gradle.kts`.
- Kotlin встроен в AGP 9 (плагин `kotlin-android` не применяется). Dagger-компилятор подключён через KSP (`ksp`), а Moxy и Epoxy остаются на kapt через `com.android.legacy-kapt` (Moxy-компилятор не поддерживает KSP, Epoxy не переводили). В kapt-конфигурации нужен явный `kotlin-metadata-jvm`: процессор Epoxy иначе падает на метаданных Kotlin 2.4. Модуль компилируется с Java 11 (этого требуют inline-функции новых AndroidX).
- В корневом `build.gradle.kts` оставлен `alias(libs.plugins.kotlin.jvm) apply false`, хотя Kotlin-JVM-модулей нет: он закрепляет Kotlin Gradle plugin 2.4.20 на classpath, без него ktlint-плагин тянет 2.2.10 и kapt падает на метаданных stdlib.
- Epoxy-модели задают layout через `override fun getDefaultLayout()`, а не `@EpoxyModelClass(layout = ...)`: в AGP 9 идентификаторы `R` не константы, флаг `android.nonFinalResIds=false` устарел и удалён.
- Базовый URL API (`https://api.exchangeratesapi.io`) захардкожен в `HttpClientModule` (есть TODO).

## Тесты

- Unit-тесты презентеров/репозиториев/интеракторов на JUnit4 + Mockito + AssertJ; репозитории (`currency/data`, `core/data`) тестируются с Ktor `MockEngine` и Robolectric.
- Для презентеров с корутинами используйте правило `ru.fasdev.ratex.core.rule.MainDispatcherRule` (`app/src/test`) — подменяет `Dispatchers.Main` на `UnconfinedTestDispatcher` (`Dispatchers.setMain`), тесты пишутся через `runTest`. В тестах data-слоя в репозитории/`ExchangeRateDataStore` передавайте `UnconfinedTestDispatcher()` вместо `Dispatchers.IO`.
- `app/src/androidTest` — Espresso-тесты `MainActivity` (`main/ui`).

## docs-ai

В `docs-ai/` лежит информация о работе AI в проекте: что и почему было сделано, принятые решения, отложенные задачи. Правила ведения — в `docs-ai/README.md`.

- После завершения крупной задачи добавьте запись `docs-ai/YYYY-MM-DD-название.md` и строку в список в `docs-ai/README.md`. Мелкие правки не документируйте.
- Пишите только то, чего нет в коде и истории git: причины решений, отличия в поведении, что проверено и что нет, что отложено.


# ast-index Rules

All commands: `ast-index <command>`

## Keep Index Up To Date

After `git pull`, `git rebase`, `git checkout`, or `git switch`, run
`ast-index update`.

For active development, keep the watcher running:

```bash
ast-index watch
# or, from the current shell:
ast-index watch &
```

## Mandatory Search Rules

1. **ALWAYS use ast-index FIRST** for any code search task.
2. **NEVER duplicate results** — if ast-index found results, that is the complete answer.
3. **DO NOT run grep** after ast-index returns results.
4. Use Grep only when ast-index returns empty or for regex/string-literal search.

## Mandatory Read Rules

1. **ALWAYS run `ast-index outline <file>` BEFORE `Read`** for any file longer than 500 lines.
2. Use the outline to identify the specific symbol or range you need, then `Read` only that slice with `offset` / `limit`.
3. This rule is mandatory — do not bulk-read large files without an outline first.

## Rules For Subagents

When spawning any agent for code search, ALWAYS include these instructions in
the prompt. Many agent systems do not automatically pass project rules to
subagents.

```text
Use `ast-index` via Bash for code search before grep/Grep:
- search "query" — universal search
- file "Name" — find file
- usages "Name" — find all usages
- implementations "Name" — find implementations
- class "Name" — find definition
- callers "func" — find callers

Use Grep only if ast-index returns empty or when regex/string-literal search is required.

Before using the Read tool on any file longer than 500 lines, first run
`ast-index outline <file>` to get its structure, then Read only the targeted
slice via offset/limit. Never bulk-read large files.
```

## Commands

- **Search:** `search`, `file`, `symbol`, `class` — find files and symbols by name
- **Usages:** `usages`, `callers`, `call-tree`, `refs` — find where symbols are used
- **Graph:** `graph dependents|dependencies|impact|path|cycles|top|metrics` — symbol dependency graph (`graph build` first)
- **Hierarchy:** `implementations`, `hierarchy`, `extensions` — class hierarchy
- **Modules:** `module`, `deps`, `dependents`, `api` — module dependencies
- **Files:** `outline`, `imports`, `changed` — file analysis
- **iOS:** `storyboard-usages`, `asset-usages` (`--unused` for unused assets) — storyboard/asset search
- **Quality:** `todo`, `deprecated`, `hotspots` — TODOs, deprecated items, Git-history risk
- **Index:** `rebuild`, `update`, `watch`, `stats` — index management

## Common Use Cases

- `ast-index usages "PaymentViewController"` — where is this class used?
- `ast-index implementations "PaymentProcessing"` — what implements this protocol?
- `ast-index callers "processPayment"` — where is this function called?
- `ast-index call-tree "processPayment" -d 3` — call hierarchy
- `ast-index deps "PaymentFeature"` — module dependencies
- `ast-index dependents "NetworkKit"` — what depends on this module?
- `ast-index changed` — what changed in my branch?
- `ast-index hotspots --collect` — which files churn most and attract the most bugfixes?
- `ast-index search Service --rank proven` — which of these is safe to copy? (also `risky`, `hotspots`, `central`)
- `ast-index graph impact "PaymentGateway" --depth 3` — what breaks if I change this, transitively?
- `ast-index todo` — find all TODOs
