# Раскатка приложения: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Статус:** реализовано в ветке `feature/05-delivery_app`. План обновлён под итоговый код: он содержит решения, принятые по ходу работы (см. «Ловушки» в конце). Чекбоксы оставлены пустыми как шаблон для повторного прохода.

**Goal:** CI в GitHub Actions (ktlint, Android lint, unit-тесты) и публикация в GitHub Releases по git-тегам: alpha (`devRelease`) и release (`prodRelease`), с версией из git, flavors `dev`/`prod`, build types `debug`/`release` и подписью из Secrets или локального файла. Pipeline собирает только release.

**Architecture:** Один модуль `:app`. Версия, flavors, R8 и подпись настраиваются в `app/build.gradle.kts` (helpers в начале файла, выше `android {}`), без `buildSrc`, `build-logic` и script plugins. Проверка «тег ↔ ветка» и выбор типа релиза сделаны одним шагом `publish.yml` на bash (без отдельного скрипта). Два workflow: `ci.yml` (проверки) и `publish.yml` (публикация по тегу); раскодирование keystore в composite action.

**Tech Stack:** Gradle 9.8 / AGP 9.4.1 (Kotlin DSL), GitHub Actions, `gh` CLI, bash, JDK 17.

**Spec:** `docs/superpowers/specs/2026-10-06-delivery-app-design.md`

## Global Constraints

- Теги: alpha = `X.Y.Z-alpha` (коммит должен лежать в `develop`), release = `X.Y.Z` (коммит в `master`). Префикса `v` и номера `N` нет.
- Pipeline собирает **только release**: alpha = `devRelease` (`ru.fasdev.ratex.dev`), release = `prodRelease` (`ru.fasdev.ratex`). Debug-сборок и debug-ключей в pipeline нет. Alpha публикуется с `--prerelease`. Имя APK: `ratex-<env>-release-<tag>.apk`.
- `versionName` = `git describe --tags --match "[0-9]*.[0-9]*.[0-9]*"`, fallback `0.0.0-dev`. `versionCode` = `git rev-list --count HEAD`, fallback `0`. Переопределение: `-PappVersionName`, `-PappVersionCode`. `gitOutput` не подавляет ошибки запуска `git` (`throw e`); код выхода `git` игнорируется.
- Flavor-измерение `env`: `dev` (`applicationIdSuffix = ".dev"`, имя «Ratex Dev»), `prod` («Ratex»). Различий URL между flavors нет, URL в коде не трогаем.
- `debug`: `applicationIdSuffix = ".debug"`, `isDebuggable = true`. `release`: `isMinifyEnabled = true`, `isShrinkResources = true`, `isDebuggable = false`.
- Подпись: параметры `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` и то же с `DEBUG_`. Источники: env `RATEX_<NAME>` (приоритет) или `signature/keystore.properties` (каталог `signature/` в `.gitignore`). `STORE_FILE` относительный путь разрешается от корня проекта.
- Без release-ключа `assemble|package|bundle*Release` (а также `assemble` и `build`) падают с понятной ошибкой до компиляции и R8. Fallback на debug-подпись для release нет. `test`, `ktlintCheck`, debug-сборки работают без ключей. Debug без ключа использует `~/.android/debug.keystore`; debug-ключ нужен только локально.
- GitHub Secrets (4 шт.): `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.
- AGP 9 создаёт unit-тесты **только для debug-вариантов**: задач `test*ReleaseUnitTest` нет. CI гоняет `:app:testProdDebugUnitTest`; publish гоняет `:app:test<Dev|Prod>DebugUnitTest`. Это JVM-тесты, APK не собирается. Локально: `testDevDebugUnitTest`, `assembleDevDebug`.
- Android lint в pipeline: `:app:lintProdRelease` (CI) и `:app:lint<Dev|Prod>Release` (publish); ключи не нужны, на текущем коде проходит (около 30 с, конфигурации `lint.xml` нет).
- Отдельного скрипта и автотеста для проверки тега нет (решение владельца): формат тега гарантирует glob-фильтр триггера `publish.yml`, шаг только определяет тип по суффиксу `-alpha` и проверяет ветку.
- JDK 17 (Gradle daemon закреплён `gradle/gradle-daemon-jvm.properties`). Код `.kts` проходит `ktlintCheck` (`.editorconfig`: 4 пробела, 140 колонок; функции с одним выражением в виде `= ...`).
- Проектные правила (`AGENTS.md`): ветка фичи `feature/05-delivery_app` от `develop`; **коммитить, вливать, ставить теги и пушить агент может только по явной просьбе пользователя**. Шаги «Commit» выполнять только если пользователь подтвердил. Теги агент не ставит.
- Для запуска Gradle вне sandbox (кэш `~/.gradle`) агент использует `dangerouslyDisableSandbox` на этих командах. Временные файлы и логи писать в scratchpad, а не в `$TMPDIR` (он различается в sandbox и вне него).

## Review Focus

- Коммит, лежащий и в `develop`, и в `master` (общий предок): alpha и release на нём проходят проверку ветки (Task 4, шаг 5).
- Теги с «опасными» символами (`1.2.0-alpha;touch x`): значение идёт через `env`, а не подставляется в `run`, поэтому не исполняется; посторонние форматы (`v1.2.0`, `1.2`) отсекает фильтр триггера (проверяется на первом реальном теге) (Task 4, шаг 5).
- Release без ключей: понятная ошибка, проверка идёт первой задачей; при этом `test`, `ktlintCheck`, `assembleDevDebug` без ключей работают. Проверять **в чистой копии без `signature/`**: на машине владельца реальные ключи есть (Task 3).
- Alpha собирается как `dev`, release как `prod`: имена задач, путь APK и `applicationId` соответствуют (Task 4).
- Пароли с пробелами/спецсимволами через env корректно читаются (Task 3).

---

### Task 1: Flavors, build types, R8

**Files:**
- Modify: `app/build.gradle.kts` (блок `android { ... }`)
- Create: `app/src/dev/res/values/strings.xml`
- Modify: `app/proguard-rules.pro` (только если R8 потребует правил; в реализации не потребовалось)
- Modify: `AGENTS.md` (команды)

**Interfaces:**
- Produces: варианты `devDebug`, `devRelease`, `prodDebug`, `prodRelease`; задачи `assemble{Dev,Prod}{Debug,Release}`, `test{Dev,Prod}DebugUnitTest`.

- [ ] **Step 0: Ветка фичи (если пользователь подтвердил git-операции)**

```bash
git switch develop && git switch -c feature/05-delivery_app
```

- [ ] **Step 1: Убедиться, что задач ещё нет**

Run: `./gradlew :app:assembleProdDebug`
Expected: FAIL, `Cannot locate tasks that match ':app:assembleProdDebug'`.

- [ ] **Step 2: Flavors и build types**

В `android { ... }` после `defaultConfig { ... }`:

```kotlin
    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
        }
        create("prod") {
            dimension = "env"
        }
    }
```

`dimension = "env"` обязателен для каждого flavor (требование AGP, даже при одном измерении); имя `env` произвольное. Build types (подпись добавляется в Task 3):

```kotlin
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
```

- [ ] **Step 3: Имя приложения для dev**

Create `app/src/dev/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Ratex Dev</string>
</resources>
```
(ресурс flavor перекрывает `app_name` из `main`; `prod` берёт «Ratex» из `main`).

- [ ] **Step 4: Собрать debug и проверить applicationId**

Run: `./gradlew :app:assembleDevDebug :app:assembleProdDebug`
Expected: BUILD SUCCESSFUL.

Run: `grep -h '"applicationId"' app/build/outputs/apk/dev/debug/output-metadata.json app/build/outputs/apk/prod/debug/output-metadata.json`
Expected: `ru.fasdev.ratex.dev.debug` и `ru.fasdev.ratex.debug`.

- [ ] **Step 5: Unit-тесты**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL. Запускает тесты debug-вариантов (`testDevDebugUnitTest`, `testProdDebugUnitTest`); release unit-тестов в AGP 9 нет.

- [ ] **Step 6: Release (проверка R8)**

Run: `./gradlew :app:assembleProdRelease :app:assembleDevRelease`
Expected: BUILD SUCCESSFUL (до Task 3 APK без подписи).
Если R8 падает с `Missing classes detected`: дописать содержимое `app/build/outputs/mapping/prodRelease/missing_rules.txt` в `app/proguard-rules.pro`. Если падает `lintVital*`: исправить код, lint не отключать.

- [ ] **Step 7: AGENTS.md, команды**

Заменить блок команд на:

```bash
./gradlew assembleDevDebug                                               # сборка debug APK (dev); prod: assembleProdDebug
./gradlew test                                                           # все unit-тесты
./gradlew :app:testDevDebugUnitTest --tests "package.ClassTest"          # один класс
./gradlew :app:testDevDebugUnitTest --tests "*ClassTest.someMethod"      # один метод
```

- [ ] **Step 8: ktlint и commit**

Run: `./gradlew ktlintFormat ktlintCheck` — Expected: BUILD SUCCESSFUL.

```bash
git add app/build.gradle.kts app/src/dev AGENTS.md
git commit -m "feat: add dev/prod flavors, release minify"
```

---

### Task 2: Версионирование из git

**Files:**
- Modify: `app/build.gradle.kts` (helpers в начале файла)

**Interfaces:**
- Produces: `fun gitOutput(vararg args: String): String?`, `val appVersionName: String`, `val appVersionCode: Int` (используются в `defaultConfig`); property `-PappVersionName` / `-PappVersionCode` (использует `publish.yml`).

- [ ] **Step 1: Текущее состояние**

Run: `./gradlew :app:assembleProdDebug && grep -E '"versionCode"|"versionName"' app/build/outputs/apk/prod/debug/output-metadata.json`
Expected: `1` и `"1.0"` (захардкожено).

- [ ] **Step 2: Helpers версии**

В начале `app/build.gradle.kts` (после `import`, **выше блока `android {}`**: `val` в скрипте инициализируются сверху вниз, значение, объявленное ниже `android {}`, там будет `null`; `by lazy` не помогает):

```kotlin
//region Version block
/**
 * Выполняет `git` с переданными аргументами и возвращает его stdout.
 *
 * Код выхода игнорируется: например, `git describe` без тегов завершается с ошибкой и ничего не печатает.
 *
 * @param args аргументы команды `git`, например `"rev-list", "--count", "HEAD"`.
 * @return обрезанный по краям stdout, либо `null`, если вывод пустой.
 */
fun gitOutput(vararg args: String): String? = try {
    providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().ifEmpty { null }
} catch (e: Exception) {
    throw e
}

val appVersionName: String
    get() {
        return providers.gradleProperty("appVersionName").orNull
            ?: gitOutput("describe", "--tags", "--match", "[0-9]*.[0-9]*.[0-9]*")
            ?: "0.0.0-dev"
    }
val appVersionCode: Int
    get() {
        return providers.gradleProperty("appVersionCode").orNull?.toInt()
            ?: gitOutput("rev-list", "--count", "HEAD")?.toIntOrNull()
            ?: 0
    }
//endregion
```

В `defaultConfig` заменить `versionCode = 1` и `versionName = "1.0"`:

```kotlin
        versionCode = appVersionCode
        versionName = appVersionName
```
(`extra[...]` внутри `android {}` не использовать: там `extra` означает `extra` блока Android, а не проекта.)

- [ ] **Step 3: Без тегов**

Run: `./gradlew :app:assembleProdDebug && grep -E '"versionCode"|"versionName"' app/build/outputs/apk/prod/debug/output-metadata.json && git rev-list --count HEAD`
Expected: `versionName` = `0.0.0-dev`, `versionCode` = число из `git rev-list`.

- [ ] **Step 4: Переопределение**

Run: `./gradlew :app:assembleProdDebug -PappVersionName=1.2.3-alpha -PappVersionCode=42 && grep -E '"versionCode"|"versionName"' app/build/outputs/apk/prod/debug/output-metadata.json`
Expected: `42` и `1.2.3-alpha`.

- [ ] **Step 5: `git describe` на клоне с тегом (теги в основном репозитории не создаём)**

```bash
S=<scratchpad>; clone="$S/ratex-ver"; rm -rf "$clone"
git clone -q --no-hardlinks . "$clone"
cp app/build.gradle.kts "$clone/app/build.gradle.kts"; cp -R app/src/dev "$clone/app/src/dev"; cp local.properties "$clone/local.properties"
git -C "$clone" tag 1.2.0-alpha
(cd "$clone" && ./gradlew :app:assembleProdDebug -q && grep -E '"versionCode"|"versionName"' app/build/outputs/apk/prod/debug/output-metadata.json)
```
Expected: `"versionName": "1.2.0-alpha"`.

- [ ] **Step 6: ktlint и commit**

Run: `./gradlew ktlintFormat ktlintCheck` — Expected: BUILD SUCCESSFUL.

```bash
git add app/build.gradle.kts
git commit -m "feat: derive versionName/versionCode from git"
```

---

### Task 3: Подпись и падение release без ключей

**Files:**
- Modify: `app/build.gradle.kts` (helpers подписи, `signingConfigs`, `buildTypes`, задача проверки)
- Modify: `.gitignore`

**Interfaces:**
- Consumes: блок `buildTypes` из Task 1.
- Produces: `signingValue(name)`, `missingSigningValues(prefix)`, задача `checkReleaseSigning`; контракт env `RATEX_<PREFIX>_{STORE_FILE,STORE_PASSWORD,KEY_ALIAS,KEY_PASSWORD}` (использует composite action в Task 5).

- [ ] **Step 1: Проверить «до»**

Run: `./gradlew :app:assembleProdRelease`
Expected: BUILD SUCCESSFUL (APK без подписи). Это поведение убираем.

- [ ] **Step 2: .gitignore**

Дописать:

```
/keystore.properties
*.jks
*.keystore
/signature
```

- [ ] **Step 3: Helpers подписи (в начале файла, рядом с блоком версии, выше `android {}`)**

Добавить `import java.util.Properties` и:

```kotlin
//region Signature block
val keystoreProperties = Properties().apply {
    val file = rootProject.file("signature/keystore.properties")
    if (file.exists()) {
        file.inputStream().use {
            load(it)
        }
    }
}

/**
 * Возвращает значение параметра подписи.
 *
 * Источники по приоритету: переменная окружения `RATEX_<name>`, затем ключ `<name>` в `keystore.properties`.
 *
 * @param name имя параметра без префикса `RATEX_`, например `RELEASE_STORE_PASSWORD`.
 * @return значение или `null`, если оно нигде не задано или состоит из пробелов.
 */
fun signingValue(name: String): String? = providers.environmentVariable("RATEX_$name").orNull
    ?.takeIf { it.isNotBlank() }
    ?: keystoreProperties.getProperty(name)
        ?.takeIf { it.isNotBlank() }

/**
 * Возвращает имена параметров подписи, которые не заданы для указанного набора ключей.
 *
 * Полный набор: `<prefix>_STORE_FILE`, `<prefix>_STORE_PASSWORD`, `<prefix>_KEY_ALIAS`, `<prefix>_KEY_PASSWORD`.
 *
 * @param prefix `RELEASE` или `DEBUG`.
 * @return незаданные имена; пустой список означает, что ключ настроен полностью.
 */
fun missingSigningValues(prefix: String): List<String> = listOf("STORE_FILE", "STORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
    .map { "${prefix}_$it" }
    .filter { signingValue(it) == null }
//endregion
```

- [ ] **Step 4: signingConfigs и привязка к build types**

В `android { ... }` перед `buildTypes`:

```kotlin
    signingConfigs {
        create("release") {
            val hasReleaseConfig = missingSigningValues("RELEASE").isEmpty()
            if (hasReleaseConfig) {
                storeFile = rootProject.file(signingValue("RELEASE_STORE_FILE")!!)
                storePassword = signingValue("RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("RELEASE_KEY_ALIAS")
                keyPassword = signingValue("RELEASE_KEY_PASSWORD")
            }
        }
        getByName("debug") {
            val hasDebugConfig = missingSigningValues("DEBUG").isEmpty()
            if (hasDebugConfig) {
                storeFile = rootProject.file(signingValue("DEBUG_STORE_FILE")!!)
                storePassword = signingValue("DEBUG_STORE_PASSWORD")
                keyAlias = signingValue("DEBUG_KEY_ALIAS")
                keyPassword = signingValue("DEBUG_KEY_PASSWORD")
            }
        }
    }
```

В `buildTypes`: в `debug` добавить `signingConfig = signingConfigs.getByName("debug").takeIf { missingSigningValues("DEBUG").isEmpty() }`, в `release` добавить `signingConfig = signingConfigs.getByName("release").takeIf { missingSigningValues("RELEASE").isEmpty() }`.

- [ ] **Step 5: Задача проверки ключей (после блока `android {}`, перед `ksp {}`)**

```kotlin
val checkReleaseSigning = tasks.register("checkReleaseSigning") {
    group = "verification"
    description = "Fails if release signing keys are not configured"
    doLast {
        val missing = missingSigningValues("RELEASE")
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Release signing is not configured. Set env RATEX_<NAME> or keystore.properties <NAME> for: " +
                    missing.joinToString()
            )
        }
    }
}

val releaseTaskPattern = Regex("(assemble|package|bundle)(Dev|Prod)?Release")
tasks.configureEach {
    if (name.matches(releaseTaskPattern)) dependsOn(checkReleaseSigning)
    // Без ключей release падает сразу, до компиляции, R8 и lint
    if (name != checkReleaseSigning.name && name.contains("Release")) mustRunAfter(checkReleaseSigning)
}
```

- [ ] **Step 6: Проверить падение без ключей (в чистой копии без `signature/`)**

На машине с реальными ключами в `signature/` проверка «без ключей» ничего не покажет. Делать в клоне:

```bash
S=<scratchpad>; x="$S/exp"; rm -rf "$x"; git clone -q --no-hardlinks . "$x"
cp app/build.gradle.kts "$x/app/"; cp local.properties "$x/"; cd "$x"
./gradlew :app:assembleDevRelease -q 2>&1 | grep -m1 "Release signing"
./gradlew :app:assembleProdRelease -q >/dev/null 2>&1; echo "exit=$?"
./gradlew :app:assembleProdRelease --console=plain 2>&1 | grep -E "^> Task" | head -3
./gradlew ktlintCheck :app:testProdDebugUnitTest :app:assembleDevDebug test -q >/dev/null 2>&1; echo "others exit=$?"
```
Expected: сообщение `Release signing is not configured ... RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD`; `exit=1`; первая и единственная задача `:app:checkReleaseSigning FAILED`; `others exit=0`.

- [ ] **Step 7: Подпись одноразовым ключом (env, пароль с пробелом)**

```bash
ks="<scratchpad>/test-release.jks"; rm -f "$ks"
keytool -genkeypair -keystore "$ks" -alias testkey -keyalg RSA -keysize 2048 -validity 30 \
  -storepass "pass word 1" -keypass "pass word 1" -dname "CN=test"
RATEX_RELEASE_STORE_FILE="$ks" RATEX_RELEASE_STORE_PASSWORD="pass word 1" \
RATEX_RELEASE_KEY_ALIAS=testkey RATEX_RELEASE_KEY_PASSWORD="pass word 1" \
  ./gradlew :app:assembleProdRelease
```
Expected: BUILD SUCCESSFUL; `app/build/outputs/apk/prod/release/app-prod-release.apk` без `unsigned`.

Run: `<sdk.dir>/build-tools/<последняя>/apksigner verify --print-certs app/build/outputs/apk/prod/release/app-prod-release.apk | head -1`
Expected: `Signer #1 certificate DN: CN=test`.

- [ ] **Step 8: ktlint и commit**

Run: `./gradlew ktlintFormat ktlintCheck` — Expected: BUILD SUCCESSFUL.

```bash
git add app/build.gradle.kts .gitignore
git commit -m "feat: sign release/debug from env or keystore file, fail release without keys"
```

---

### Task 4: Workflows

**Files:**
- Create: `.github/actions/decode-keystore/action.yml`
- Create: `.github/workflows/ci.yml`
- Create: `.github/workflows/publish.yml`

**Interfaces:**
- Consumes: env-контракт `RATEX_<PREFIX>_*` (Task 3); `-PappVersionName` (Task 2); задачи `test{Dev,Prod}DebugUnitTest`, `assemble{Dev,Prod}Release` (Task 1).

- [ ] **Step 1: Composite action**

Create `.github/actions/decode-keystore/action.yml`:

```yaml
name: Decode keystore
description: Decodes a base64 keystore into RUNNER_TEMP and exports RATEX_<PREFIX>_* variables for Gradle. Does nothing if the keystore is empty.
inputs:
  prefix:
    description: RELEASE or DEBUG
    required: true
  keystore-base64:
    description: Base64-encoded keystore
    required: false
    default: ''
  store-password:
    required: false
    default: ''
  key-alias:
    required: false
    default: ''
  key-password:
    required: false
    default: ''
runs:
  using: composite
  steps:
    - shell: bash
      env:
        PREFIX: ${{ inputs.prefix }}
        KEYSTORE_BASE64: ${{ inputs.keystore-base64 }}
        STORE_PASSWORD: ${{ inputs.store-password }}
        KEY_ALIAS: ${{ inputs.key-alias }}
        KEY_PASSWORD: ${{ inputs.key-password }}
      run: |
        if [ -z "$KEYSTORE_BASE64" ]; then
          echo "No $PREFIX keystore provided, skipping"
          exit 0
        fi
        path="$RUNNER_TEMP/${PREFIX,,}.jks"
        echo "$KEYSTORE_BASE64" | base64 --decode > "$path"
        {
          echo "RATEX_${PREFIX}_STORE_FILE=$path"
          echo "RATEX_${PREFIX}_STORE_PASSWORD=$STORE_PASSWORD"
          echo "RATEX_${PREFIX}_KEY_ALIAS=$KEY_ALIAS"
          echo "RATEX_${PREFIX}_KEY_PASSWORD=$KEY_PASSWORD"
        } >> "$GITHUB_ENV"
```

- [ ] **Step 2: CI workflow (без APK, без секретов)**

Create `.github/workflows/ci.yml`:

```yaml
name: CI

on:
  push:
    branches: [develop, master]
  pull_request:
    branches: [develop, master]

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - uses: gradle/actions/setup-gradle@v4

      - name: ktlint, Android lint and unit tests (JVM tests of the debug variant; AGP has no release unit tests; no APK is built, no keys needed)
        run: ./gradlew ktlintCheck :app:lintProdRelease :app:testProdDebugUnitTest
```

- [ ] **Step 3: Publish workflow (alpha = dev, release = prod)**

Create `.github/workflows/publish.yml`:

```yaml
name: Publish

on:
  push:
    tags:
      - '[0-9]+.[0-9]+.[0-9]+'
      - '[0-9]+.[0-9]+.[0-9]+-alpha'

permissions:
  contents: write

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Classify tag and verify branch
        id: classify
        env:
          TAG: ${{ github.ref_name }}
        run: |
          case "$TAG" in
            *-alpha) kind=alpha; ref=origin/develop ;;
            *)       kind=release; ref=origin/master ;;
          esac
          git merge-base --is-ancestor "$GITHUB_SHA" "$ref" \
            || { echo "Tag '$TAG' ($kind) must point to a commit on $ref" >&2; exit 1; }
          echo "kind=$kind" >> "$GITHUB_OUTPUT"

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - uses: gradle/actions/setup-gradle@v4

      - name: Decode release keystore
        uses: ./.github/actions/decode-keystore
        with:
          prefix: RELEASE
          keystore-base64: ${{ secrets.RELEASE_KEYSTORE_BASE64 }}
          store-password: ${{ secrets.RELEASE_STORE_PASSWORD }}
          key-alias: ${{ secrets.RELEASE_KEY_ALIAS }}
          key-password: ${{ secrets.RELEASE_KEY_PASSWORD }}

      - name: Select environment (alpha is dev, release is prod)
        id: variant
        env:
          KIND: ${{ steps.classify.outputs.kind }}
        run: |
          if [ "$KIND" = "alpha" ]; then
            echo "env=dev" >> "$GITHUB_OUTPUT"
            echo "gradle=Dev" >> "$GITHUB_OUTPUT"
          else
            echo "env=prod" >> "$GITHUB_OUTPUT"
            echo "gradle=Prod" >> "$GITHUB_OUTPUT"
          fi

      - name: ktlint, Android lint, unit tests (AGP has no release unit tests, JVM only, no debug APK), signed release build
        env:
          TAG: ${{ github.ref_name }}
          VARIANT: ${{ steps.variant.outputs.gradle }}
        run: ./gradlew ktlintCheck ":app:lint${VARIANT}Release" ":app:test${VARIANT}DebugUnitTest" ":app:assemble${VARIANT}Release" "-PappVersionName=$TAG"

      - name: Publish GitHub release
        env:
          GH_TOKEN: ${{ github.token }}
          TAG: ${{ github.ref_name }}
          KIND: ${{ steps.classify.outputs.kind }}
          ENV: ${{ steps.variant.outputs.env }}
        run: |
          apk="ratex-$ENV-release-$TAG.apk"
          cp "app/build/outputs/apk/$ENV/release/app-$ENV-release.apk" "$apk"
          flags=()
          if [ "$KIND" = "alpha" ]; then flags+=(--prerelease); fi
          gh release create "$TAG" "$apk" --title "$TAG" --generate-notes --verify-tag "${flags[@]}"
```

- [ ] **Step 4: Проверка синтаксиса**

Run: `for f in .github/workflows/*.yml .github/actions/decode-keystore/action.yml; do ruby -ryaml -e 'YAML.load_file(ARGV[0]); puts "ok #{ARGV[0]}"' $f; done`
Expected: три `ok`. (PyYAML может быть не установлен; `actionlint` и `shellcheck`, если стоят, прогнать тоже.)

- [ ] **Step 5: Проверить шаг classify локально (только чтение)**

Вынуть `run` шага `classify` из YAML и выполнить как GitHub (`bash -eo pipefail`) на реальных ветках:

```bash
S=<scratchpad>
ruby -ryaml -e 'y=YAML.load_file(".github/workflows/publish.yml"); puts y["jobs"]["publish"]["steps"].find{|s| s["id"]=="classify"}["run"]' > $S/classify-step.sh
run() { out="$S/gho.txt"; : > "$out"; TAG=$1 GITHUB_SHA=$2 GITHUB_OUTPUT="$out" bash --noprofile --norc -eo pipefail $S/classify-step.sh; echo "tag=$1 exit=$? output=[$(cat $out)]"; }
dev=$(git rev-parse origin/develop); mas=$(git rev-parse origin/master)
run 1.2.0-alpha $dev          # exit=0 kind=alpha
run 1.2.0 $dev                # exit=1, коммита develop нет в master
run 1.2.0 $mas                # exit=0 kind=release
run '1.2.0-alpha;touch pwned' $dev; ls pwned    # exit=1, файл pwned не создан
```

- [ ] **Step 6: Прогнать команды pipeline локально с одноразовым ключом**

Alpha (dev):
```bash
RATEX_RELEASE_STORE_FILE="$ks" RATEX_RELEASE_STORE_PASSWORD="pass word 1" RATEX_RELEASE_KEY_ALIAS=testkey RATEX_RELEASE_KEY_PASSWORD="pass word 1" \
  ./gradlew ktlintCheck :app:lintDevRelease :app:testDevDebugUnitTest :app:assembleDevRelease -PappVersionName=1.2.0-alpha
grep -E '"applicationId"|"versionName"' app/build/outputs/apk/dev/release/output-metadata.json
```
Expected: `ru.fasdev.ratex.dev`, `1.2.0-alpha`. Release (prod) аналогично с `:app:lintProdRelease :app:testProdDebugUnitTest :app:assembleProdRelease -PappVersionName=1.2.0` → `ru.fasdev.ratex`, `1.2.0`. CI-команда: `./gradlew ktlintCheck :app:lintProdRelease :app:testProdDebugUnitTest` проходит без ключей.

- [ ] **Step 7: Commit**

```bash
git add .github
git commit -m "ci: add CI and tag-based publish workflows"
```

---

### Task 5: Документация

**Files:**
- Modify: `AGENTS.md`
- Create: `docs-ai/artifact/2026-10-06-delivery-app.md` (каталог в `.gitignore`, не коммитится)

- [ ] **Step 1: Git flow в AGENTS.md**

Теги `vX.Y.Z` заменить на `X.Y.Z`; команда релиза: `git switch master && git merge --no-ff develop -m "Release X.Y.Z" && git tag X.Y.Z`.

- [ ] **Step 2: Раздел «Релизы и сборки» в AGENTS.md (перед `## docs-ai`)**

Содержание: окружения и варианты; версия из git и ограничение немонотонного `versionCode` (release с `master` не ставится поверх более нового alpha, сначала влить `develop` в `master`); схема тегов и что собирает pipeline (alpha = `devRelease`, release = `prodRelease`, только release, без debug-ключей); повтор alpha (удалить тег локально и на origin и GitHub release); подпись (`RATEX_<NAME>` или `signature/keystore.properties`, имена параметров, падение `assemble*Release`/`package*Release`/`bundle*Release`/`assemble`/`build` без ключа, fallback debug на `~/.android/debug.keystore`); агентам теги не ставить и не пушить.

- [ ] **Step 3: Артефакт сессии**

Разделы: «Что сделано», «Решения» (теги без `v` и `N`; alpha = `devRelease`, release = `prodRelease`; pipeline только release; unit-тесты через debug-вариант; release без ключей падает; URL не трогали; helpers выше `android {}`; `catch { throw e }` и fallback `versionCode = 0`; отказ от `buildSrc`/`build-logic`/script plugins), «Ручные шаги владельца» (keytool для release keystore, `base64 -i release.jks | pbcopy`, 4 секрета GitHub, `signature/keystore.properties` для локальной сборки), «Что не проверено» (R8 на устройстве `devRelease` и `prodRelease`; первый запуск workflow на теге: JDK 17 от `setup-java`, Android SDK для `compileSdk = 37` на раннере, права `GITHUB_TOKEN`; `actionlint`/`shellcheck`), «Отложено» (URL dev/prod при появлении бэкенда, автоочистка alpha, Play).

- [ ] **Step 4: Финальная проверка**

Run: `./gradlew ktlintCheck :app:lintProdRelease test` — Expected: BUILD SUCCESSFUL.
В чистой копии без `signature/`: `./gradlew :app:assembleProdRelease` — Expected: FAIL с сообщением про release-подпись.

- [ ] **Step 5: Commit**

```bash
git add AGENTS.md
git commit -m "docs: describe releases, flavors and tag scheme"
```

---

## Ловушки, найденные при реализации

1. **AGP 9 не создаёт release unit-тесты.** Задачи `testProdReleaseUnitTest` нет; использовать `test<Env>DebugUnitTest`.
2. **`extra` внутри `android {}`** резолвится в `extra` блока Android, а не проекта. Читать значения напрямую из `val` на верхнем уровне скрипта.
3. **Порядок объявлений в `.gradle.kts`:** `val` на верхнем уровне инициализируются сверху вниз; helpers с `val` должны стоять выше `android {}`. `by lazy` не спасает.
4. **`mktemp` в sandbox** без `$TMPDIR` падает; скрипт-тест, не проверивший это, выполнился в самом репозитории и создал пустые коммиты. Скрипты, создающие временные репозитории, обязаны завершаться при ошибке `mktemp`/`cd`.
5. **`$c:app/...` в zsh** трактуется как модификатор; писать `"${c}:app/..."`.
6. **Проверка «без ключей»** на машине с реальными ключами в `signature/` ничего не показывает: проверять в чистой копии.
7. **ktlint** требует expression body для функций из одного выражения (`fun f(): T = ...`).
