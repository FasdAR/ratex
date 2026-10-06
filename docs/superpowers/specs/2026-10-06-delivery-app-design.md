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
- Остальные теги workflow игнорирует.
- Проверка ветки в CI: checkout с `fetch-depth: 0`, затем `git merge-base --is-ancestor $GITHUB_SHA origin/develop` (alpha) или `origin/master` (release). При несоответствии job падает с понятным сообщением, публикации нет.

## 2. Версионирование
- `versionName` = вывод `git describe --tags` как есть (для тега: `1.2.0-alpha`, `1.2.0`).
- `versionCode` = `git rev-list --count HEAD`.
- Оба значения можно переопределить через Gradle-property / env (для CI).
- Без тегов (локально, свежий клон) fallback: `versionName = "0.0.0-dev"`; сборка не ломается.
- Реализация: script plugin `gradle/versioning.gradle.kts` (без `buildSrc`), значения отдаются через `extra`; `app/build.gradle.kts` подключает его через `apply(from = ...)` и читает `val appVersionName: String by extra` / `val appVersionCode: Int by extra`.
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
- Unit-тесты не ловят проблемы R8. Проверка: `assembleProdRelease` / `assembleDevRelease` собираются без ошибок; перед первым релизом ручная проверка установленного release-APK (основной сценарий приложения).

## 4. Подпись
- `release`-варианты подписаны release-ключом, `debug`-варианты отдельным debug-ключом.
- Локальная сборка использует те же ключи, что и GitHub Secrets. Данные берутся из env или из `keystore.properties` в корне проекта (файл и keystore в `.gitignore`). Ключи будут добавлены позже.
- Без release-ключа сборка **release-вариантов падает** с понятной ошибкой (какой env/свойство не задано). Fallback на debug-подпись для release нет. Проверка срабатывает только когда в графе есть release-задача (`assemble|package|bundle*Release`, а также `assemble` и `build`, которые их включают) и выполняется до компиляции и R8, поэтому `test`, `ktlintCheck` и debug-сборки работают без ключей.
- Debug-варианты: если debug-ключ не задан, используется стандартный `~/.android/debug.keystore`, чтобы локальная разработка не блокировалась.
- CI: оба keystore лежат в GitHub Secrets (base64). Секреты (8 шт.): keystore, store password, key alias, key password для каждого из двух ключей. Названия фиксируются в плане.
- Release-ключ один для alpha и release, плюс одинаковый `applicationId` (оба `prodRelease`), поэтому APK ставятся друг поверх друга.
- Ручной шаг владельца: сгенерировать два keystore и завести секреты. Инструкция в `docs-ai/artifact`.

## 5. Workflows (`.github/workflows/`)
### `ci.yml`
- Триггеры: push и pull_request в `develop`, `master`.
- Шаги: JDK 17 и Gradle-кэш, тест скрипта `classify-tag_test.sh`, `ktlintCheck`, `:app:testProdDebugUnitTest`, `assembleDevDebug`; APK загружается как artifact workflow.

### `publish.yml`
- Триггер: push тега `[0-9]+.[0-9]+.[0-9]+*` (точная фильтрация regex-ом в первом шаге).
- Шаги:
  1. Определить тип тега: `^[0-9]+\.[0-9]+\.[0-9]+-alpha$` (alpha) или `^[0-9]+\.[0-9]+\.[0-9]+$` (release), иначе выход без ошибки.
  2. Проверить ветку (раздел 1).
  3. `ktlintCheck` и `:app:testProdReleaseUnitTest`.
  4. И alpha, и release: `assembleProdRelease`. Подпись из Secrets.
  5. `gh release create` (alpha с `--prerelease`, `--generate-notes`), приложить APK `ratex-<env>-<buildType>-<version>.apk`.

> Решение: и alpha, и release собираются как `prodRelease`; различаются тегом, `versionName` и флагом pre-release. Flavor `dev` и debug-варианты в Releases не публикуются.

## 6. Документация
- `AGENTS.md`: раздел «Релизы и сборки» (схема тегов, варианты сборки, подпись); в Git flow теги `vX.Y.Z` заменить на `X.Y.Z`; команды локальных прогонов используют `dev`: `assembleDevDebug`, `testDevDebugUnitTest`.
- `docs-ai/artifact/YYYY-MM-DD-delivery-app.md`: результат, решения, инструкция по keystore и секретам.

## 7. Тестирование и проверка
- `./gradlew ktlintCheck test` проходит.
- Все 4 варианта собираются: `assembleDevDebug`, `assembleProdDebug`, `assembleDevRelease`, `assembleProdRelease` (release только при наличии ключей; без ключей `assemble*Release` должен падать с понятной ошибкой, это тоже проверяется).
- Проверка версионирования: на тестовом теге `versionName`/`versionCode` совпадают с ожиданием (через `aapt dump badging` или Gradle-вывод).
- Проверка логики ветка/тег в `publish.yml` на уровне скрипта (позитивный и негативный случай), т.к. сам workflow локально не запустить.
