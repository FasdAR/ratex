# Раскатка приложения: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** CI в GitHub Actions (ktlint, тесты, сборка) и публикация alpha/release APK в GitHub Releases по git-тегам, с версией из git, flavors `dev`/`prod`, build types `debug`/`release` и подписью из Secrets/локального файла.

**Architecture:** Всё в одном модуле `:app`. Версия, flavors, R8 и подпись настраиваются в `app/build.gradle.kts`. Проверка «тег ↔ ветка» вынесена в bash-скрипт `.github/scripts/classify-tag.sh` с собственным тестом, чтобы её можно было прогнать локально. Два workflow: `ci.yml` (проверки) и `publish.yml` (публикация по тегу); общий шаг раскодирования keystore вынесен в composite action.

**Tech Stack:** Gradle 9.8 / AGP 9.4.1 (Kotlin DSL), GitHub Actions, `gh` CLI, bash, JDK 17.

**Spec:** `docs/superpowers/specs/2026-10-06-delivery-app-design.md`

## Global Constraints

- Теги: alpha = `X.Y.Z-alpha` (коммит должен лежать в `develop`), release = `X.Y.Z` (коммит в `master`). Префикса `v` и номера `N` нет.
- `versionName` = `git describe --tags` как есть; fallback без тегов `0.0.0-dev`. `versionCode` = `git rev-list --count HEAD`. Оба переопределяются Gradle-property (`appVersionName`, `appVersionCode`).
- Flavor-измерение `env`: `dev` (`applicationIdSuffix = ".dev"`, имя «Ratex Dev»), `prod` (текущий `applicationId`, имя «Ratex»). Различий URL между flavors нет, URL в коде не трогаем.
- `debug`: `applicationIdSuffix = ".debug"`, `isDebuggable = true`. `release`: `isMinifyEnabled = true`, `isShrinkResources = true`, `isDebuggable = false`.
- Подпись: release-ключ и debug-ключ раздельные. Источники значений: env `RATEX_<NAME>` или `keystore.properties` в корне (ключ `<NAME>`). Имена: `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` и то же для `DEBUG_`.
- Без release-ключа `assemble*Release` / `package*Release` / `bundle*Release` падают с понятной ошибкой, fallback на debug-подпись для release нет. `test`, `ktlintCheck`, debug-сборки работают без ключей. Debug без ключа использует `~/.android/debug.keystore`.
- Alpha и release оба собираются как `prodRelease`; alpha публикуется с `--prerelease`. Flavor `dev` и debug-варианты в Releases не публикуются.
- Имя APK в релизе: `ratex-<env>-<buildType>-<version>.apk`.
- GitHub Secrets (8 шт.): `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`, `DEBUG_KEYSTORE_BASE64`, `DEBUG_STORE_PASSWORD`, `DEBUG_KEY_ALIAS`, `DEBUG_KEY_PASSWORD`.
- JDK 17 (Gradle daemon закреплён `gradle/gradle-daemon-jvm.properties`). Код `.kts` проходит `ktlintCheck` (`.editorconfig`: 4 пробела, 140 колонок).
- Проектные правила (`AGENTS.md`): ветка фичи `feature/05-delivery_app` от `develop`; **коммитить, вливать, ставить теги и пушить агент может только по явной просьбе пользователя**. Шаги «Commit» ниже выполняются только если пользователь подтвердил коммиты; иначе пропускать, оставляя изменения в рабочем дереве. Теги агент не ставит ни при каких условиях.
- После команд `gradlew` пользуемся командами из `AGENTS.md`; после добавления flavors локальные прогоны идут на `dev`, команда одного теста: `./gradlew :app:testDevDebugUnitTest --tests "..."` (CI гоняет `testProdDebugUnitTest`).

## Review Focus

- Коммит, который одновременно лежит и в `develop`, и в `master` (общий предок): alpha и release на нём проходят проверку ветки (Task 4, тест).
- Тег с «опасными» символами (`1.2.0-alpha;x`, `v1.2.0`, `1.2.0-alpha.1`, `1.2`, `1.2.0-beta`) не публикуется и не исполняется как команда, скрипт выходит с кодом 2 (Task 4, тест).
- Release-сборка без ключей: понятная ошибка со списком недостающих значений; при этом `test`, `ktlintCheck`, `assembleProdDebug` без ключей работают (Task 3, шаги проверки).
- Сборка без git-тегов и в репозитории без git: `versionName = 0.0.0-dev`, сборка не падает (Task 2).
- Пароли с пробелами/спецсимволами в env и `keystore.properties` корректно читаются (Task 3, шаг проверки с паролем с пробелом).

---

### Task 1: Flavors, build types, R8

**Files:**
- Modify: `app/build.gradle.kts:11-47` (блок `android { ... }`)
- Create: `app/src/dev/res/values/strings.xml`
- Modify: `app/proguard-rules.pro` (только если R8 потребует правил)
- Modify: `AGENTS.md:12-16` (команды тестов)

**Interfaces:**
- Produces: варианты `devDebug`, `devRelease`, `prodDebug`, `prodRelease`; Gradle-задачи `assemble{Dev,Prod}{Debug,Release}`, `test{Dev,Prod}{Debug,Release}UnitTest`. Release-варианты пока без подписи (подпись в Task 3).

- [ ] **Step 0: Создать ветку фичи (если пользователь подтвердил git-операции)**

```bash
git switch develop && git switch -c feature/05-delivery_app
```
Незакоммиченное изменение `docs-ai/planning/05-delivery_app.md` переедет вместе с веткой.

- [ ] **Step 1: Убедиться, что новых задач ещё нет (падающая «проверка»)**

Run: `./gradlew :app:assembleProdDebug`
Expected: FAIL, `Cannot locate tasks that match ':app:assembleProdDebug'`.

- [ ] **Step 2: Добавить flavors и build types**

В `app/build.gradle.kts` внутри `android { ... }` заменить блок `buildTypes { ... }` и добавить `flavorDimensions`/`productFlavors` сразу после `defaultConfig { ... }`:

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

- [ ] **Step 4: Собрать debug-варианты и проверить applicationId**

Run: `./gradlew :app:assembleDevDebug :app:assembleProdDebug`
Expected: BUILD SUCCESSFUL.

Run: `grep -h '"applicationId"' app/build/outputs/apk/dev/debug/output-metadata.json app/build/outputs/apk/prod/debug/output-metadata.json`
Expected: `"applicationId": "ru.fasdev.ratex.dev.debug"` и `"applicationId": "ru.fasdev.ratex.debug"`.

- [ ] **Step 5: Прогнать unit-тесты на новых вариантах**

Run: `./gradlew :app:testProdDebugUnitTest`
Expected: BUILD SUCCESSFUL, все тесты проходят (число тестов то же, что до изменений).

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL (запускает тесты всех 4 вариантов; release-тесты идут на несминифицированном коде).

- [ ] **Step 6: Собрать release-варианты (проверка R8)**

Run: `./gradlew :app:assembleProdRelease :app:assembleDevRelease`
Expected: BUILD SUCCESSFUL. APK без подписи (`app-prod-release-unsigned.apk`), подпись добавится в Task 3.

Если упал R8 с `Missing classes detected while running R8`: открыть `app/build/outputs/mapping/prodRelease/missing_rules.txt`, дописать его содержимое в конец `app/proguard-rules.pro` и повторить сборку. Если упал `lintVital*`: исправить указанную проблему в коде, lint не отключать.

- [ ] **Step 7: Обновить команды в AGENTS.md**

В `AGENTS.md` заменить две строки с `testDebugUnitTest`:

```bash
./gradlew :app:testDevDebugUnitTest --tests "package.ClassTest"          # один класс
./gradlew :app:testDevDebugUnitTest --tests "*ClassTest.someMethod"      # один метод
```
и строку `./gradlew assembleDebug  # сборка debug APK` заменить на:

```bash
./gradlew assembleProdDebug                                              # сборка debug APK (prod); dev: assembleDevDebug
```

- [ ] **Step 8: ktlint**

Run: `./gradlew ktlintFormat ktlintCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Commit**

```bash
git add app/build.gradle.kts app/src/dev app/proguard-rules.pro AGENTS.md docs/superpowers docs-ai/planning/05-delivery_app.md
git commit -m "feat: add dev/prod flavors, release minify"
```

---

### Task 2: Версионирование из git

**Files:**
- Modify: `app/build.gradle.kts` (верх файла и `defaultConfig`)

**Interfaces:**
- Produces: в `app/build.gradle.kts` значения `appVersionName: String`, `appVersionCode: Int`; Gradle-property `appVersionName` / `appVersionCode` для переопределения (использует `publish.yml` в Task 5).

- [ ] **Step 1: Зафиксировать текущее (неверное) состояние**

Run: `./gradlew :app:assembleProdDebug && grep -E '"versionCode"|"versionName"' app/build/outputs/apk/prod/debug/output-metadata.json`
Expected: `"versionCode": 1`, `"versionName": "1.0"` (захардкожено, это и меняем).

- [ ] **Step 2: Добавить вычисление версии**

В `app/build.gradle.kts` после блока `plugins { ... }` добавить:

```kotlin
fun gitOutput(vararg args: String): String? = try {
    providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().ifEmpty { null }
} catch (e: Exception) {
    null
}

val appVersionName: String = providers.gradleProperty("appVersionName").orNull
    ?: gitOutput("describe", "--tags", "--match", "[0-9]*.[0-9]*.[0-9]*")
    ?: "0.0.0-dev"
val appVersionCode: Int = providers.gradleProperty("appVersionCode").orNull?.toInt()
    ?: gitOutput("rev-list", "--count", "HEAD")?.toIntOrNull()
    ?: 1
```

В `defaultConfig` заменить `versionCode = 1` и `versionName = "1.0"`:

```kotlin
        versionCode = appVersionCode
        versionName = appVersionName
```

- [ ] **Step 3: Проверить без тегов (fallback и счётчик коммитов)**

Run: `./gradlew :app:assembleProdDebug && grep -E '"versionCode"|"versionName"' app/build/outputs/apk/prod/debug/output-metadata.json && git rev-list --count HEAD`
Expected: `versionName` = `0.0.0-dev`, `versionCode` равен числу из `git rev-list --count HEAD`.

- [ ] **Step 4: Проверить переопределение через property**

Run: `./gradlew :app:assembleProdDebug -PappVersionName=1.2.3-alpha -PappVersionCode=42 && grep -E '"versionCode"|"versionName"' app/build/outputs/apk/prod/debug/output-metadata.json`
Expected: `"versionCode": 42`, `"versionName": "1.2.3-alpha"`.

- [ ] **Step 5: Проверить `git describe` на клоне с тегом (теги в основном репозитории не создаём)**

```bash
clone="$TMPDIR/ratex-ver"; rm -rf "$clone"
git clone -q --no-hardlinks . "$clone"
cp app/build.gradle.kts "$clone/app/build.gradle.kts"
cp local.properties "$clone/local.properties"
git -C "$clone" tag 1.2.0-alpha
(cd "$clone" && ./gradlew :app:assembleProdDebug -q && grep -E '"versionCode"|"versionName"' app/build/outputs/apk/prod/debug/output-metadata.json)
```
Expected: `"versionName": "1.2.0-alpha"` (коммит с тегом), `versionCode` = число коммитов в клоне.

- [ ] **Step 6: Проверить сборку вне git**

```bash
nogit="$TMPDIR/ratex-nogit"; rm -rf "$nogit"; mkdir "$nogit"
git archive HEAD | tar -x -C "$nogit"
cp app/build.gradle.kts "$nogit/app/build.gradle.kts"; cp local.properties "$nogit/local.properties"
(cd "$nogit" && ./gradlew :app:assembleProdDebug -q && grep -E '"versionCode"|"versionName"' app/build/outputs/apk/prod/debug/output-metadata.json)
```
Expected: BUILD SUCCESSFUL, `"versionCode": 1`, `"versionName": "0.0.0-dev"`.
(Если в `$nogit` сборка падает по причинам, не связанным с версией, например `git archive` не включил `local.properties`, достаточно убедиться, что ошибка не из `gitOutput`.)

- [ ] **Step 7: ktlint и commit**

Run: `./gradlew ktlintFormat ktlintCheck`
Expected: BUILD SUCCESSFUL.

```bash
git add app/build.gradle.kts
git commit -m "feat: derive versionName/versionCode from git"
```

---

### Task 3: Подпись и падение release без ключей

**Files:**
- Modify: `app/build.gradle.kts` (импорт, подпись, `signingConfigs`, `buildTypes.release`, задача проверки)
- Modify: `.gitignore`

**Interfaces:**
- Consumes: блок `android { buildTypes { release { ... } } }` из Task 1.
- Produces: функция `signingValue(name)`, env `RATEX_<NAME>` и `keystore.properties` (ключи `RELEASE_*`, `DEBUG_*`); Gradle-задача `checkReleaseSigning`. Контракт env-переменных используется composite action в Task 5.

- [ ] **Step 1: Убедиться, что сейчас release собирается без ключей (проверка падения ещё не работает)**

Run: `./gradlew :app:assembleProdRelease`
Expected: BUILD SUCCESSFUL (APK `unsigned`). Это поведение мы убираем.

- [ ] **Step 2: .gitignore**

Дописать в `.gitignore`:

```
/keystore.properties
*.jks
*.keystore
```

- [ ] **Step 3: Чтение значений подписи**

В начало `app/build.gradle.kts` добавить `import java.util.Properties` (перед `plugins`), а после блока вычисления версии добавить:

```kotlin
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(name: String): String? = providers.environmentVariable("RATEX_$name").orNull?.takeIf { it.isNotBlank() }
    ?: keystoreProperties.getProperty(name)?.takeIf { it.isNotBlank() }

fun missingSigningValues(prefix: String): List<String> =
    listOf("STORE_FILE", "STORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
        .map { "${prefix}_$it" }
        .filter { signingValue(it) == null }
```

- [ ] **Step 4: signingConfigs и привязка к release**

В `android { ... }` перед `buildTypes` добавить:

```kotlin
    signingConfigs {
        create("release") {
            if (missingSigningValues("RELEASE").isEmpty()) {
                storeFile = rootProject.file(signingValue("RELEASE_STORE_FILE")!!)
                storePassword = signingValue("RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("RELEASE_KEY_ALIAS")
                keyPassword = signingValue("RELEASE_KEY_PASSWORD")
            }
        }
        getByName("debug") {
            if (missingSigningValues("DEBUG").isEmpty()) {
                storeFile = rootProject.file(signingValue("DEBUG_STORE_FILE")!!)
                storePassword = signingValue("DEBUG_STORE_PASSWORD")
                keyAlias = signingValue("DEBUG_KEY_ALIAS")
                keyPassword = signingValue("DEBUG_KEY_PASSWORD")
            }
        }
    }
```

В `buildTypes { release { ... } }` добавить строку:

```kotlin
            signingConfig = signingConfigs.getByName("release").takeIf { missingSigningValues("RELEASE").isEmpty() }
```

- [ ] **Step 5: Задача проверки ключей**

После блока `android { ... }` (рядом с `ksp { ... }`) добавить:

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
}
```

- [ ] **Step 6: Проверить падение без ключей**

Run: `./gradlew :app:assembleProdRelease`
Expected: FAIL, сообщение `Release signing is not configured... for: RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD`.

Run: `./gradlew :app:assembleDevRelease`
Expected: FAIL с тем же сообщением.

- [ ] **Step 7: Проверить, что без ключей работают не-release задачи**

Run: `./gradlew ktlintCheck :app:testProdDebugUnitTest :app:assembleProdDebug :app:assembleDevDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL (release unit-тесты не требуют подписи).

- [ ] **Step 8: Проверить подпись одноразовым ключом (env, пароль с пробелом)**

```bash
ks="$TMPDIR/test-release.jks"; rm -f "$ks"
keytool -genkeypair -keystore "$ks" -alias testkey -keyalg RSA -keysize 2048 -validity 30 \
  -storepass "pass word 1" -keypass "pass word 1" -dname "CN=test"
RATEX_RELEASE_STORE_FILE="$ks" RATEX_RELEASE_STORE_PASSWORD="pass word 1" \
RATEX_RELEASE_KEY_ALIAS=testkey RATEX_RELEASE_KEY_PASSWORD="pass word 1" \
  ./gradlew :app:assembleProdRelease
```
Expected: BUILD SUCCESSFUL, APK `app/build/outputs/apk/prod/release/app-prod-release.apk` (без `unsigned`).

Run: `$ANDROID_HOME/build-tools/*/apksigner verify --print-certs app/build/outputs/apk/prod/release/app-prod-release.apk | head -3`
(если `ANDROID_HOME` не задан, взять `sdk.dir` из `local.properties`; при выборе нескольких версий build-tools взять последнюю)
Expected: `Verifies` и `CN=test`.

- [ ] **Step 9: Проверить keystore.properties**

```bash
cat > keystore.properties <<EOF
RELEASE_STORE_FILE=$TMPDIR/test-release.jks
RELEASE_STORE_PASSWORD=pass word 1
RELEASE_KEY_ALIAS=testkey
RELEASE_KEY_PASSWORD=pass word 1
EOF
./gradlew :app:assembleDevRelease
rm keystore.properties
```
Expected: BUILD SUCCESSFUL. Затем `git status --short` не показывает `keystore.properties` и `.jks` (файл удалён, правило в `.gitignore` проверено шагом `git check-ignore keystore.properties`; повторить создание пустого файла и убедиться, что он игнорируется).

- [ ] **Step 10: ktlint и commit**

Run: `./gradlew ktlintFormat ktlintCheck`
Expected: BUILD SUCCESSFUL.

```bash
git add app/build.gradle.kts .gitignore
git commit -m "feat: sign release/debug from env or keystore.properties, fail release without keys"
```

---

### Task 4: Скрипт «тег ↔ ветка» с тестом

**Files:**
- Create: `.github/scripts/classify-tag.sh`
- Create: `.github/scripts/classify-tag_test.sh`

**Interfaces:**
- Produces: `.github/scripts/classify-tag.sh <tag> <commit-sha>`. stdout: `alpha` или `release`. Код выхода: 0 ок; 1 коммит не в нужной ветке или ветка не найдена; 2 тег не релизный. Переменные `DEVELOP_REF` (по умолчанию `origin/develop`) и `MASTER_REF` (по умолчанию `origin/master`). Используется в `publish.yml` (Task 5).

- [ ] **Step 1: Написать тест (падает, пока скрипта нет)**

Create `.github/scripts/classify-tag_test.sh`:

```bash
#!/usr/bin/env bash
# Тест classify-tag.sh на временном git-репозитории: m1 (общий предок), d1 (только develop), m2 (только master).
set -uo pipefail

script="$(cd "$(dirname "$0")" && pwd)/classify-tag.sh"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
cd "$work"

git init -q -b master .
git config user.email test@example.com
git config user.name test
git commit -q --allow-empty -m m1
m1=$(git rev-parse HEAD)
git switch -q -c develop
git commit -q --allow-empty -m d1
d1=$(git rev-parse HEAD)
git switch -q master
git commit -q --allow-empty -m m2
m2=$(git rev-parse HEAD)

export DEVELOP_REF=develop MASTER_REF=master
failures=0

check() {
  local name=$1 expected_code=$2 expected_out=$3 tag=$4 sha=$5 out code
  out=$("$script" "$tag" "$sha" 2>/dev/null)
  code=$?
  if [[ $code -ne $expected_code || $out != "$expected_out" ]]; then
    echo "FAIL: $name (code=$code out='$out', expected code=$expected_code out='$expected_out')"
    failures=$((failures + 1))
  else
    echo "ok: $name"
  fi
}

check "alpha on develop commit" 0 alpha 1.2.0-alpha "$d1"
check "release on master commit" 0 release 1.2.0 "$m2"
check "alpha on common ancestor" 0 alpha 1.2.0-alpha "$m1"
check "release on common ancestor" 0 release 1.2.0 "$m1"
check "alpha on master-only commit" 1 "" 1.2.0-alpha "$m2"
check "release on develop-only commit" 1 "" 1.2.0 "$d1"
check "v prefix is not a release tag" 2 "" v1.2.0 "$m2"
check "alpha with counter is not a release tag" 2 "" 1.2.0-alpha.1 "$d1"
check "two-part version is not a release tag" 2 "" 1.2 "$m2"
check "beta is not a release tag" 2 "" 1.2.0-beta "$d1"
check "shell metacharacters are rejected" 2 "" '1.2.0-alpha;touch pwned' "$d1"

DEVELOP_REF=missing-branch check "missing ref fails" 1 "" 1.2.0-alpha "$d1"

if [[ $failures -ne 0 ]]; then
  echo "$failures check(s) failed"
  exit 1
fi
echo "all checks passed"
```

Run: `chmod +x .github/scripts/classify-tag_test.sh && .github/scripts/classify-tag_test.sh`
Expected: FAIL (скрипт `classify-tag.sh` не найден, проверки печатают `FAIL`, итог `check(s) failed`).

- [ ] **Step 2: Реализовать скрипт**

Create `.github/scripts/classify-tag.sh`:

```bash
#!/usr/bin/env bash
# Определяет тип релиза по тегу и проверяет, что коммит лежит в нужной ветке.
# Использование: classify-tag.sh <tag> <commit-sha>
# stdout: "alpha" (X.Y.Z-alpha, коммит в develop) или "release" (X.Y.Z, коммит в master).
# Коды выхода: 0 - ок; 1 - коммит не в нужной ветке; 2 - тег не релизный (пропустить).
set -euo pipefail

tag="${1:?tag is required}"
sha="${2:?commit sha is required}"
develop_ref="${DEVELOP_REF:-origin/develop}"
master_ref="${MASTER_REF:-origin/master}"

if [[ "$tag" =~ ^[0-9]+\.[0-9]+\.[0-9]+-alpha$ ]]; then
  kind=alpha
  ref="$develop_ref"
elif [[ "$tag" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  kind=release
  ref="$master_ref"
else
  echo "Tag '$tag' is not a release tag, skipping" >&2
  exit 2
fi

if ! git rev-parse --verify --quiet "$ref" >/dev/null; then
  echo "Reference '$ref' not found; fetch full history (fetch-depth: 0)" >&2
  exit 1
fi

if ! git merge-base --is-ancestor "$sha" "$ref"; then
  echo "Tag '$tag' ($kind) must point to a commit on $ref, but $sha is not reachable from it" >&2
  exit 1
fi

echo "$kind"
```

- [ ] **Step 3: Запустить тест**

Run: `chmod +x .github/scripts/classify-tag.sh && .github/scripts/classify-tag_test.sh`
Expected: все строки `ok:`, итог `all checks passed`, код выхода 0.

Run: `shellcheck .github/scripts/*.sh` (если shellcheck установлен)
Expected: без замечаний.

- [ ] **Step 4: Commit**

```bash
git add .github/scripts
git commit -m "ci: add tag/branch classification script with tests"
```

---

### Task 5: Workflows

**Files:**
- Create: `.github/actions/decode-keystore/action.yml`
- Create: `.github/workflows/ci.yml`
- Create: `.github/workflows/publish.yml`

**Interfaces:**
- Consumes: env-контракт подписи `RATEX_<PREFIX>_{STORE_FILE,STORE_PASSWORD,KEY_ALIAS,KEY_PASSWORD}` (Task 3); property `appVersionName` (Task 2); `classify-tag.sh` (Task 4); задачи `:app:testProdReleaseUnitTest`, `:app:assembleProdRelease`, `:app:assembleDevDebug` (Task 1).
- Produces: workflow `CI` (push/PR в `develop`, `master`) и `Publish` (push тега).

- [ ] **Step 1: Composite action раскодирования keystore**

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

- [ ] **Step 2: CI workflow**

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

      - name: Decode debug keystore (skipped when secrets are unavailable, e.g. fork PRs)
        uses: ./.github/actions/decode-keystore
        with:
          prefix: DEBUG
          keystore-base64: ${{ secrets.DEBUG_KEYSTORE_BASE64 }}
          store-password: ${{ secrets.DEBUG_STORE_PASSWORD }}
          key-alias: ${{ secrets.DEBUG_KEY_ALIAS }}
          key-password: ${{ secrets.DEBUG_KEY_PASSWORD }}

      - name: Lint, unit tests, debug build
        run: ./gradlew ktlintCheck :app:testProdDebugUnitTest :app:assembleDevDebug

      - uses: actions/upload-artifact@v4
        with:
          name: ratex-dev-debug
          path: app/build/outputs/apk/dev/debug/*.apk
```

- [ ] **Step 3: Publish workflow**

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
          set +e
          kind=$(.github/scripts/classify-tag.sh "$TAG" "$GITHUB_SHA")
          code=$?
          set -e
          case $code in
            0) echo "kind=$kind" >> "$GITHUB_OUTPUT" ;;
            2) echo "kind=skip" >> "$GITHUB_OUTPUT" ;;
            *) exit "$code" ;;
          esac

      - uses: actions/setup-java@v4
        if: steps.classify.outputs.kind != 'skip'
        with:
          distribution: temurin
          java-version: 17

      - uses: gradle/actions/setup-gradle@v4
        if: steps.classify.outputs.kind != 'skip'

      - name: Decode release keystore
        if: steps.classify.outputs.kind != 'skip'
        uses: ./.github/actions/decode-keystore
        with:
          prefix: RELEASE
          keystore-base64: ${{ secrets.RELEASE_KEYSTORE_BASE64 }}
          store-password: ${{ secrets.RELEASE_STORE_PASSWORD }}
          key-alias: ${{ secrets.RELEASE_KEY_ALIAS }}
          key-password: ${{ secrets.RELEASE_KEY_PASSWORD }}

      - name: Lint, tests, signed release build
        if: steps.classify.outputs.kind != 'skip'
        env:
          TAG: ${{ github.ref_name }}
        run: ./gradlew ktlintCheck :app:testProdReleaseUnitTest :app:assembleProdRelease "-PappVersionName=$TAG"

      - name: Publish GitHub release
        if: steps.classify.outputs.kind != 'skip'
        env:
          GH_TOKEN: ${{ github.token }}
          TAG: ${{ github.ref_name }}
          KIND: ${{ steps.classify.outputs.kind }}
        run: |
          apk="ratex-prod-release-$TAG.apk"
          cp app/build/outputs/apk/prod/release/app-prod-release.apk "$apk"
          flags=()
          if [ "$KIND" = "alpha" ]; then flags+=(--prerelease); fi
          gh release create "$TAG" "$apk" --title "$TAG" --generate-notes --verify-tag "${flags[@]}"
```

- [ ] **Step 4: Проверить синтаксис workflow и action**

Run: `python3 -c "import yaml,sys; [yaml.safe_load(open(f)) for f in sys.argv[1:]]; print('yaml ok')" .github/workflows/ci.yml .github/workflows/publish.yml .github/actions/decode-keystore/action.yml`
Expected: `yaml ok`.

Run: `which actionlint && actionlint .github/workflows/*.yml`
Expected: без замечаний; если `actionlint` не установлен, шаг пропустить и отметить это в отчёте.

- [ ] **Step 5: Проверить shell-логику шага classify локально**

```bash
export DEVELOP_REF=develop MASTER_REF=master GITHUB_SHA=$(git rev-parse develop) TAG=1.2.0-alpha GITHUB_OUTPUT="$TMPDIR/out"; : > "$GITHUB_OUTPUT"
bash -c 'set +e; kind=$(.github/scripts/classify-tag.sh "$TAG" "$GITHUB_SHA"); code=$?; set -e; case $code in 0) echo "kind=$kind" >> "$GITHUB_OUTPUT" ;; 2) echo "kind=skip" >> "$GITHUB_OUTPUT" ;; *) exit "$code" ;; esac'
cat "$GITHUB_OUTPUT"
```
Expected: `kind=alpha`. Повторить с `TAG=v1.2.0` → `kind=skip`; с `TAG=1.2.0` и `GITHUB_SHA` коммитом, которого нет в `master` (если таковой есть в `develop`), → код 1 и сообщение об ошибке.

- [ ] **Step 6: Commit**

```bash
git add .github
git commit -m "ci: add CI and tag-based publish workflows"
```

---

### Task 6: Документация

**Files:**
- Modify: `AGENTS.md` (Git flow + новый раздел «Релизы»)
- Create: `docs-ai/artifact/2026-10-06-delivery-app.md`

**Interfaces:**
- Consumes: все предыдущие задачи (описывает их результат).

- [ ] **Step 1: Править Git flow в AGENTS.md**

В `AGENTS.md`:
- В списке веток заменить в строке про `master` `теги \`vX.Y.Z\`` на `теги \`X.Y.Z\``.
- В блоке команд слияния заменить последнюю строку на:

```bash
git switch master && git merge --no-ff develop -m "Release X.Y.Z" && git tag X.Y.Z                # релиз
```

- [ ] **Step 2: Добавить раздел «Релизы» в AGENTS.md**

Вставить перед разделом `## docs-ai`:

````markdown
## Релизы и сборки

- Окружения (flavor `env`): `dev` (`ru.fasdev.ratex.dev`, «Ratex Dev») и `prod` (`ru.fasdev.ratex`, «Ratex»). Типы сборки: `debug` (суффикс `.debug`, логи, отладка) и `release` (R8, без отладки). Варианты: `devDebug`, `devRelease`, `prodDebug`, `prodRelease`.
- Версия берётся из git: `versionName` = `git describe --tags`, `versionCode` = число коммитов. Без тегов `0.0.0-dev`.
- Тестовый билд (pre-release): тег `X.Y.Z-alpha` на коммит из `develop`. Релиз: тег `X.Y.Z` на коммит из `master`. Префикса `v` и номера нет. GitHub Actions (`publish.yml`) проверит ветку, прогонит ktlint и тесты, соберёт подписанный `prodRelease` и опубликует его в GitHub Releases.
- Повторный alpha той же версии: удалить тег (локально и на origin) и GitHub release, поставить заново.
- Подпись: release- и debug-ключи берутся из env `RATEX_<NAME>` или `keystore.properties` (в `.gitignore`), имена `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` и `DEBUG_*`. Без release-ключа `assemble*Release` падает. Агентам теги не ставить и не пушить.
````

- [ ] **Step 3: Записать результат сессии**

Create `docs-ai/artifact/2026-10-06-delivery-app.md` (папка в `.gitignore`, файл не коммитится) с разделами: «Что сделано» (flavors, версия из git, подпись, workflows, скрипт), «Решения» (теги без `v` и `N`, один alpha на версию, alpha = `prodRelease`, release без ключей падает, URL не трогали), «Ручные шаги владельца» и «Отложено» (URL dev/prod при появлении бэкенда, автоочистка alpha, Play). В «Ручных шагах» привести:

```bash
keytool -genkeypair -v -keystore release.jks -alias ratex -keyalg RSA -keysize 2048 -validity 10000
keytool -genkeypair -v -keystore debug-ci.jks -alias ratex-debug -keyalg RSA -keysize 2048 -validity 10000
base64 -i release.jks | pbcopy    # значение секрета RELEASE_KEYSTORE_BASE64; то же для debug-ci.jks -> DEBUG_KEYSTORE_BASE64
```
и список 8 секретов (GitHub → Settings → Secrets and variables → Actions), а также пример `keystore.properties` для локальной сборки:

```properties
RELEASE_STORE_FILE=/абсолютный/путь/release.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=ratex
RELEASE_KEY_PASSWORD=...
```
Также указать проверку, оставленную на человека: поставить `prodRelease` APK на устройство и пройти основной сценарий (R8 не ловится unit-тестами), и первый запуск workflow на реальном теге (проверить, что Gradle находит JDK 17, который ставит `setup-java`).

- [ ] **Step 4: Финальная проверка**

Run: `./gradlew ktlintCheck test && .github/scripts/classify-tag_test.sh`
Expected: BUILD SUCCESSFUL и `all checks passed`.

Run: `./gradlew :app:assembleProdRelease`
Expected: FAIL с сообщением про release-подпись (ключей нет), это штатное поведение.

- [ ] **Step 5: Commit**

```bash
git add AGENTS.md
git commit -m "docs: describe releases, flavors and tag scheme"
```
