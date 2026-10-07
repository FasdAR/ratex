# Раскатка приложения: CI, версионирование, flavors, подпись

Задача: `docs-ai/planning/05-delivery_app.md`.

## Цель
Автоматическая сборка и тесты в GitHub Actions; публикация тестового (alpha) и релизного билдов в GitHub Releases; версия приложения из git; два окружения (`dev`, `prod`) и два типа сборки (`debug`, `release`).

Раздача билдов: только автор / узкий круг, APK из GitHub Releases. Внешние сервисы (Firebase, Google Play) не используются.

## Вне объёма
- Различие URL между dev и prod (сейчас бэкенда нет, URL источников остаются как есть).
- Публикация в Google Play / AAB.
- Автоочистка старых alpha-релизов.
- Подмена источников данных моками в dev.

## 1. Теги и ветки
| Тег | Ветка, на которой должен лежать коммит | Результат |
|---|---|---|
| `X.Y.Z-alpha` | `develop` | GitHub pre-release |
| `X.Y.Z` | `master` | GitHub release |

- Префикса `v` и номера `N` нет. Следствие: на одну версию `X.Y.Z` приходится один alpha-тег. Повторный alpha той же версии требует пересоздать тег (удалить и поставить заново) либо поднять версию.
- Это меняет формулировку в `AGENTS.md` (Git flow: теги `vX.Y.Z` -> `X.Y.Z`), правка входит в объём (раздел 6).

- Теги ставит человек, вручную (агенты теги не ставят и не пушат, см. AGENTS.md).
- Остальные теги workflow не запускает: фильтр триггера `publish.yml` пропускает только `[0-9]+.[0-9]+.[0-9]+` и `[0-9]+.[0-9]+.[0-9]+-alpha`.
- Проверка ветки в CI: checkout с `fetch-depth: 0`, затем в шаге `publish.yml` (без отдельного скрипта) тип определяется по суффиксу: `*-alpha` — alpha, иначе release; `git merge-base --is-ancestor $GITHUB_SHA origin/develop` (alpha) или `origin/master` (release). При несоответствии job падает с понятным сообщением, публикации нет. Формат тега отдельно не проверяется, его гарантирует фильтр триггера.

## 2. Версионирование
- `versionName` = вывод `git describe --tags --match "[0-9]*.[0-9]*.[0-9]*"` как есть (для тега: `1.2.0-alpha`, `1.2.0`; между тегами вида `1.2.0-alpha-5-gabc1234`).
- `versionCode` = `git rev-list --count HEAD`.
- Оба значения можно переопределить через Gradle-property / env (для CI).
- Fallback: без тегов (локально, свежий клон) `versionName = "0.0.0-dev"`; если `git rev-list --count` не вернул число, `versionCode = 0`. Сборка при этом не ломается.
- `gitOutput` не подавляет ошибки запуска `git`: если `git` не установлен или не запускается, исключение пробрасывается и сборка падает (`throw e`), а не молча подставляет версию. Код выхода самого `git` игнорируется (например, `git describe` без тегов).
- Реализация в `app/build.gradle.kts` (функция `gitOutput` и значения `appVersionName` / `appVersionCode`), без отдельного модуля, `buildSrc` и script plugins.
- Ограничение: счётчик коммитов не монотонен между ветками (release с `master` может иметь меньший `versionCode`, чем ранее установленный alpha с `develop`), перед release нужно влить `develop` в `master`.

## 3. Flavors и build types
Flavor-измерение `env`: `dev`, `prod`. Build types: `debug`, `release`. Итого 4 варианта.

| | debug | release |
|---|---|---|
| **dev** | `ru.fasdev.ratex.dev.debug` | `ru.fasdev.ratex.dev` |
| **prod** | `ru.fasdev.ratex.debug` | `ru.fasdev.ratex` |

- `dev`: `applicationIdSuffix = ".dev"`, имя приложения «Ratex Dev». `prod`: имя «Ratex».
- `debug`: `applicationIdSuffix = ".debug"`, `isDebuggable = true`, логи включены (Ktor `LogLevel.BODY` уже завязан на `BuildConfig.DEBUG`).
- `release`: `isMinifyEnabled = true`, `isShrinkResources = true`, `isDebuggable = false`.
- Разные `applicationId` позволяют ставить все варианты рядом.
- Различия URL между flavors отсутствуют (вне объёма).

### R8
- Dagger, Room, kotlinx.serialization приносят consumer-rules; Ktor/OkHttp могут потребовать правил в `app/proguard-rules.pro`.
- Unit-тесты не ловят проблемы R8. Проверка: `assembleProdRelease` / `assembleDevRelease` собираются без ошибок; перед первым релизом ручная проверка установленного release-APK (`devRelease` для alpha, `prodRelease` для релиза) (основной сценарий приложения).

## 4. Подпись
- `release`-варианты подписаны release-ключом, `debug`-варианты отдельным debug-ключом. Debug-ключ нужен только для локальной разработки: pipeline GitHub debug-сборки не делает.
- Локальная сборка использует те же ключи, что и GitHub Secrets. Данные берутся из env или из `signature/keystore.properties` (каталог `signature/` в `.gitignore`). Ключи будут добавлены позже.
- Без release-ключа сборка **release-вариантов падает** с понятной ошибкой (какой env/свойство не задано). Fallback на debug-подпись для release нет. Проверка срабатывает только когда в графе есть release-задача (`assemble|package|bundle*Release`, а также `assemble` и `build`, которые их включают) и выполняется до компиляции и R8, поэтому `test`, `ktlintCheck` и debug-сборки работают без ключей.
- Debug-варианты: если debug-ключ не задан, используется стандартный `~/.android/debug.keystore`, чтобы локальная разработка не блокировалась.
- CI: release keystore лежит в GitHub Secrets (base64). Секреты (4 шт.): `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Debug-ключ в CI не используется.
- Release-ключ один для alpha и release. `applicationId` у них разный (alpha `ru.fasdev.ratex.dev`, релиз `ru.fasdev.ratex`), поэтому APK ставятся рядом, а не друг поверх друга.
- Ручной шаг владельца: сгенерировать два keystore и завести секреты. Инструкция в `docs-ai/artifact`.

## 5. Workflows (`.github/workflows/`)
### `ci.yml`
- Триггеры: push и pull_request в `develop`, `master`.
- Шаги: JDK 17 и Gradle-кэш, `ktlintCheck`, `:app:lintProdRelease` (Android lint, ключи не нужны), `:app:testProdDebugUnitTest`. APK в CI не собирается (debug-сборок нет, release требует ключей), секреты не нужны, поэтому fork-PR проходят проверку.

### `publish.yml`
- Триггер: push тега `[0-9]+.[0-9]+.[0-9]+` или `[0-9]+.[0-9]+.[0-9]+-alpha` (glob-фильтр GitHub).
- Шаги:
  1. Определить тип тега по суффиксу `-alpha` и проверить ветку (раздел 1) в одном шаге.
  2. Выбрать окружение: alpha — `dev`, release — `prod`.
  3. `ktlintCheck`, `:app:lint<Env>Release` (Android lint) и `:app:test<Env>DebugUnitTest`, где `<Env>` = `Dev` для alpha, `Prod` для release. AGP 9 создаёт unit-тесты только для debug-вариантов, это JVM-тесты без сборки APK, debug-сборкой pipeline они не являются.
  4. Alpha: `assembleDevRelease`, release: `assembleProdRelease`. Подпись из Secrets.
  5. `gh release create` (alpha с `--prerelease`, `--generate-notes`), приложить APK `ratex-<env>-release-<version>.apk`.

> Решение: pipeline собирает только release. Alpha собирается как `devRelease` (окружение `dev`), release как `prodRelease`. Debug-варианты в pipeline и Releases не участвуют.

## 6. Документация
- `AGENTS.md`: раздел «Релизы и сборки» (схема тегов, варианты сборки, подпись); в Git flow теги `vX.Y.Z` заменить на `X.Y.Z`; команды локальных прогонов используют `dev`: `assembleDevDebug`, `testDevDebugUnitTest`.
- `docs-ai/artifact/YYYY-MM-DD-delivery-app.md`: результат, решения, инструкция по keystore и секретам.

## 7. Тестирование и проверка
- `./gradlew ktlintCheck :app:lintProdRelease test` проходит.
- Все 4 варианта собираются локально: `assembleDevDebug`, `assembleProdDebug`, `assembleDevRelease`, `assembleProdRelease` (release только при наличии ключей; без ключей `assemble*Release` должен падать с понятной ошибкой, это тоже проверяется).
- Проверка версионирования: на тестовом теге `versionName`/`versionCode` совпадают с ожиданием (через `aapt dump badging` или Gradle-вывод).
- Автотеста для проверки ветки/тега нет (решение владельца), отдельного скрипта тоже. Шаг проверен локально как `bash -eo pipefail` на реальных ветках; окончательно проверяется на первом реальном теге: alpha на коммите из `develop` проходит, release на коммите вне `master` отклоняется.
