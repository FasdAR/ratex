# Источники курсов валют (ЕЦБ + Room-кэш) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Заменить ExchangeRateApi первичным источником ЕЦБ за общим контрактом `CurrencyRateSource` (Data), с персистентным Room-кэшем и кросс-пересчётом курсов в Domain.

**Architecture:** Источник (`CurrencyRateSource`) отдаёт снимок курсов к своей базе. `CurrencyRateRepoImpl` держит снимок «память → Room → сеть» с TTL из источника. Интерактор пересчитывает снимок под базу пользователя (`rates[Y] / rates[X]`), список доступных валют берётся из снимка.

**Tech Stack:** Kotlin 2.4, Ktor client (OkHttp / MockEngine), Room 2.8.5 (KSP), Dagger 2 (KSP), Robolectric, Mockito, AssertJ, kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-10-06-source-data-design.md`. Задача: `docs-ai/planning/02-source_data.md`.

**Отличие от спеки (упрощение):** отдельный Data-класс `RateSnapshot` не вводится. Источник возвращает Domain-сущность `RateSnapshotDomain` (data → domain разрешено), иначе пришлось бы держать две идентичные структуры и копировать между ними.

## Global Constraints

- Ветка `feature/source-data` (уже создана). Все коммиты в неё.
- Слои: `ui → domain ← data`; `domain` не знает про Android и `data`. Интерфейсы репозиториев — в `domain/boundaries/repo`, реализации — в `data/repo`.
- Блокирующую работу выполнять через `withContext(ioDispatcher)`, `CoroutineDispatcher` принимается в конструкторе (по умолчанию `Dispatchers.IO`). `CancellationException` всегда пробрасывать.
- Источник ЕЦБ: `https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml`, база EUR, id `"ecb"`, `refreshInterval` = 4 часа.
- Room: `androidx.room` 2.8.5, база `ratex.db`, версия схемы 1, схема экспортируется в `app/schemas`, `fallbackToDestructiveMigration(dropAllTables = true)`.
- Семантика курса: `RateCurrencyDomain.rate` = «1 базовой валюты = rate единиц валюты». База не входит в результат.
- Стиль: `./gradlew ktlintCheck` (правила — `.editorconfig`, max 140 символов).
- Gradle запускать без `JAVA_HOME` (демон сам берёт JDK 17).
- Сообщения коммитов заканчиваются строкой `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

## Review Focus

Входы и условия, которые спека подразумевает, но ни одна «основная» задача не покрывает; у каждой строки есть тест в указанной задаче.

- ЕЦБ вернул курс `0`, отрицательный или не число → запись пропускается, деления на ноль в кросс-пересчёте нет (Task 3, Task 1).
- Ответ не XML ЕЦБ (HTML страницы ошибки, пустой файл) → исключение, а не пустой снимок; кэш при этом не затирается (Task 3, Task 4).
- Файл БД повреждён или чтение/запись Room падает → кэш считается пустым / запись игнорируется, приложение не падает (Task 4).
- Сохранённая база или валюта локали отсутствует в источнике (RUB, BGN, HRK, локаль без валюты) → база = EUR, сохранённое значение не перезаписывается (Task 4).
- Два одновременных запроса снимка (список курсов + база при старте) → один сетевой запрос (Task 4).
- Часы устройства переведены назад (`fetchedAt` в будущем) → снимок считается устаревшим, а не «свежим навсегда» (Task 4).

---

## File Structure

Создаются:
- `app/src/main/java/ru/fasdev/ratex/currency/domain/entity/RateSnapshotDomain.kt` — снимок курсов источника + кросс-пересчёт.
- `app/src/main/java/ru/fasdev/ratex/currency/data/source/CurrencyRateSource.kt` — контракт источника.
- `app/src/main/java/ru/fasdev/ratex/currency/data/source/ecb/EcbXmlParser.kt` — разбор XML ЕЦБ.
- `app/src/main/java/ru/fasdev/ratex/currency/data/source/ecb/EcbRateSource.kt` — сетевой источник ЕЦБ.
- `app/src/main/java/ru/fasdev/ratex/currency/data/storage/RateSnapshotStorage.kt` — интерфейс хранилища + `StoredSnapshot`.
- `app/src/main/java/ru/fasdev/ratex/currency/data/storage/RateSnapshotEntities.kt` — Room-сущности.
- `app/src/main/java/ru/fasdev/ratex/currency/data/storage/RateSnapshotDao.kt` — Room DAO.
- `app/src/main/java/ru/fasdev/ratex/currency/data/storage/CurrencyDatabase.kt` — Room-база.
- `app/src/main/java/ru/fasdev/ratex/currency/data/storage/RoomRateSnapshotStorage.kt` — реализация хранилища.
- `app/src/main/java/ru/fasdev/ratex/currency/di/module/CurrencyDatabaseModule.kt` — Dagger-модуль БД (в `AppComponent`).
- Тесты: `RateSnapshotDomainTest`, `EcbXmlParserTest`, `EcbRateSourceTest`, `RoomRateSnapshotStorageTest`, `Fakes.kt`, `EcbTestData.kt`.

Изменяются: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `CurrencyRateRepo`, `CurrencyRateRepoImpl`, `CurrencyBaseRepoImpl`, `CurrencyRateInteractorImpl`, `CurrencyDomainExtension`, `CurrencyModule`, `AppComponent`, `HttpClientModule`, тесты репозиториев и интерактора.

Удаляются: `data/api/*`, `data/dataStore/*`, `ExchangeRateDataStoreTest`, `TestData`.

---

### Task 1: Domain — снимок курсов и кросс-пересчёт

**Files:**
- Create: `app/src/main/java/ru/fasdev/ratex/currency/domain/entity/RateSnapshotDomain.kt`
- Modify: `app/src/main/java/ru/fasdev/ratex/currency/domain/entity/extension/CurrencyDomainExtension.kt`
- Test: `app/src/test/java/ru/fasdev/ratex/currency/domain/entity/RateSnapshotDomainTest.kt`
- Test: `app/src/test/java/ru/fasdev/ratex/currency/domain/entity/extension/CurrencyDomainExtensionTest.kt` (дополнить)

**Interfaces:**
- Produces:
  - `data class RateSnapshotDomain(val baseCode: String, val date: String, val rates: Map<String, Double>)`
  - `val RateSnapshotDomain.availableCodes: Set<String>` (`rates.keys + baseCode`)
  - `fun RateSnapshotDomain.crossRates(targetCode: String): Map<String, Double>` — курсы всех валют снимка (включая базу источника) к `targetCode`, без самой `targetCode`; пустая карта, если `targetCode` нет в снимке.
  - `fun isKnownCurrencyCode(code: String): Boolean` в `CurrencyDomainExtension.kt`.

- [ ] **Step 1: Write the failing tests**

`RateSnapshotDomainTest.kt`:

```kotlin
package ru.fasdev.ratex.currency.domain.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RateSnapshotDomainTest {
    private val snapshot = RateSnapshotDomain(
        baseCode = "EUR",
        date = "2026-10-05",
        rates = mapOf("USD" to 2.0, "JPY" to 200.0, "GBP" to 0.5)
    )

    @Test
    fun testAvailableCodesContainsBase() {
        assertThat(snapshot.availableCodes).containsExactlyInAnyOrder("EUR", "USD", "JPY", "GBP")
    }

    @Test
    fun testCrossRatesToSourceBase() {
        assertThat(snapshot.crossRates("EUR")).isEqualTo(mapOf("USD" to 2.0, "JPY" to 200.0, "GBP" to 0.5))
    }

    @Test
    fun testCrossRatesToAnotherCurrencyGoesThroughSourceBase() {
        val result = snapshot.crossRates("USD")

        assertThat(result).containsOnlyKeys("EUR", "JPY", "GBP")
        assertThat(result.getValue("EUR")).isEqualTo(0.5)
        assertThat(result.getValue("JPY")).isEqualTo(100.0)
        assertThat(result.getValue("GBP")).isEqualTo(0.25)
    }

    @Test
    fun testCrossRatesDoesNotContainTarget() {
        assertThat(snapshot.crossRates("USD")).doesNotContainKey("USD")
        assertThat(snapshot.crossRates("EUR")).doesNotContainKey("EUR")
    }

    @Test
    fun testCrossRatesUnknownTargetIsEmpty() {
        assertThat(snapshot.crossRates("RUB")).isEmpty()
    }
}
```

В `CurrencyDomainExtensionTest.kt` добавить (импорт `org.assertj.core.api.Assertions.assertThat` уже должен быть; если нет — добавить):

```kotlin
    @Test
    fun testIsKnownCurrencyCode() {
        assertThat(isKnownCurrencyCode("USD")).isTrue()
        assertThat(isKnownCurrencyCode("ZZZ")).isFalse()
        assertThat(isKnownCurrencyCode("")).isFalse()
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*RateSnapshotDomainTest" --tests "*CurrencyDomainExtensionTest"`
Expected: FAIL (compilation error: unresolved `RateSnapshotDomain`, `isKnownCurrencyCode`).

- [ ] **Step 3: Write minimal implementation**

`RateSnapshotDomain.kt`:

```kotlin
package ru.fasdev.ratex.currency.domain.entity

/** Снимок курсов одного источника: «1 [baseCode] = rates[X] единиц X». Самой базы в [rates] нет. */
data class RateSnapshotDomain(val baseCode: String, val date: String, val rates: Map<String, Double>) {
    val availableCodes: Set<String>
        get() = rates.keys + baseCode

    /**
     * Курсы всех валют снимка к [targetCode]: «1 [targetCode] = N единиц X».
     * Считается через базу источника: rate(target→X) = rates[X] / rates[target], где rates[base] = 1.
     * Если [targetCode] нет в снимке — пустая карта.
     */
    fun crossRates(targetCode: String): Map<String, Double> {
        val withBase = rates + (baseCode to 1.0)
        val targetRate = withBase[targetCode] ?: return emptyMap()

        return withBase
            .filterKeys { it != targetCode }
            .mapValues { it.value / targetRate }
    }
}
```

В конец `CurrencyDomainExtension.kt` добавить:

```kotlin

fun isKnownCurrencyCode(code: String): Boolean = runCatching { Currency.getInstance(code) }.isSuccess
```

(`java.util.*` в файле уже импортирован.)

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*RateSnapshotDomainTest" --tests "*CurrencyDomainExtensionTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers app/src/main/java/ru/fasdev/ratex/currency/domain app/src/test/java/ru/fasdev/ratex/currency/domain
git commit -m "$(cat <<'EOF'
Add RateSnapshotDomain with cross-rate calculation

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>
EOF
)"
```

(В этот коммит попадают и спека с планом из `docs/superpowers`.)

---

### Task 2: Room-хранилище снимков

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/ru/fasdev/ratex/currency/data/storage/RateSnapshotStorage.kt`
- Create: `app/src/main/java/ru/fasdev/ratex/currency/data/storage/RateSnapshotEntities.kt`
- Create: `app/src/main/java/ru/fasdev/ratex/currency/data/storage/RateSnapshotDao.kt`
- Create: `app/src/main/java/ru/fasdev/ratex/currency/data/storage/CurrencyDatabase.kt`
- Create: `app/src/main/java/ru/fasdev/ratex/currency/data/storage/RoomRateSnapshotStorage.kt`
- Test: `app/src/test/java/ru/fasdev/ratex/currency/data/storage/RoomRateSnapshotStorageTest.kt`
- Generated, коммитить: `app/schemas/ru.fasdev.ratex.currency.data.storage.CurrencyDatabase/1.json`

**Interfaces:**
- Consumes: `RateSnapshotDomain` (Task 1).
- Produces:
  - `data class StoredSnapshot(val snapshot: RateSnapshotDomain, val fetchedAt: Long)`
  - `interface RateSnapshotStorage { suspend fun load(sourceId: String): StoredSnapshot?; suspend fun save(sourceId: String, snapshot: RateSnapshotDomain, fetchedAt: Long) }`
  - `abstract class CurrencyDatabase : RoomDatabase()` с `abstract fun rateSnapshotDao(): RateSnapshotDao`
  - `class RoomRateSnapshotStorage(private val dao: RateSnapshotDao) : RateSnapshotStorage`

- [ ] **Step 1: Add Room dependencies**

В `gradle/libs.versions.toml`: в `[versions]` после строки `kotlinxSerialization = "1.11.0"` добавить

```toml
room = "2.8.5"
```

в `[libraries]` после строки `dagger-compiler = ...` добавить

```toml
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
```

В `app/build.gradle.kts`: после блока `// Dagger` (`api(libs.dagger)` / `ksp(libs.dagger.compiler)`) добавить

```kotlin

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
```

и после закрывающей скобки блока `android { ... }` (перед `dependencies {`) добавить

```kotlin

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

(Room Gradle-плагин не подключаем: схема задаётся аргументом KSP, меньше риска несовместимости с AGP 9.)

- [ ] **Step 2: Write the failing test**

`RoomRateSnapshotStorageTest.kt`:

```kotlin
package ru.fasdev.ratex.currency.data.storage

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RoomRateSnapshotStorageTest {
    private lateinit var database: CurrencyDatabase
    private lateinit var storage: RateSnapshotStorage

    private val ecb = RateSnapshotDomain("EUR", "2026-10-05", mapOf("USD" to 1.1, "JPY" to 160.0))

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, CurrencyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storage = RoomRateSnapshotStorage(database.rateSnapshotDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testLoadUnknownSourceReturnsNull() = runTest {
        assertThat(storage.load("ecb")).isNull()
    }

    @Test
    fun testSaveAndLoad() = runTest {
        storage.save("ecb", ecb, fetchedAt = 123L)

        assertThat(storage.load("ecb")).isEqualTo(StoredSnapshot(ecb, 123L))
    }

    @Test
    fun testSaveReplacesPreviousSnapshotOfSource() = runTest {
        storage.save("ecb", ecb, fetchedAt = 1L)
        val newer = RateSnapshotDomain("EUR", "2026-10-06", mapOf("USD" to 1.2, "PLN" to 4.3))

        storage.save("ecb", newer, fetchedAt = 2L)

        val loaded = storage.load("ecb")
        assertThat(loaded).isEqualTo(StoredSnapshot(newer, 2L))
        assertThat(loaded!!.snapshot.rates).doesNotContainKey("JPY")
    }

    @Test
    fun testSnapshotsOfDifferentSourcesAreIsolated() = runTest {
        val cbr = RateSnapshotDomain("RUB", "2026-10-06", mapOf("USD" to 0.011))

        storage.save("ecb", ecb, fetchedAt = 1L)
        storage.save("cbr", cbr, fetchedAt = 2L)

        assertThat(storage.load("ecb")).isEqualTo(StoredSnapshot(ecb, 1L))
        assertThat(storage.load("cbr")).isEqualTo(StoredSnapshot(cbr, 2L))
    }

    @Test
    fun testDeletingSnapshotCascadesToRates() = runTest {
        storage.save("ecb", ecb, fetchedAt = 1L)

        database.rateSnapshotDao().deleteSnapshot("ecb")

        assertThat(database.rateSnapshotDao().getRates("ecb")).isEmpty()
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*RoomRateSnapshotStorageTest"`
Expected: FAIL (compilation error: unresolved `CurrencyDatabase`, `RoomRateSnapshotStorage`, ...).

- [ ] **Step 4: Write the implementation**

`RateSnapshotStorage.kt`:

```kotlin
package ru.fasdev.ratex.currency.data.storage

import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

data class StoredSnapshot(val snapshot: RateSnapshotDomain, val fetchedAt: Long)

/** Персистентное хранилище снимков курсов: по одному снимку на источник. */
interface RateSnapshotStorage {
    suspend fun load(sourceId: String): StoredSnapshot?
    suspend fun save(sourceId: String, snapshot: RateSnapshotDomain, fetchedAt: Long)
}
```

`RateSnapshotEntities.kt`:

```kotlin
package ru.fasdev.ratex.currency.data.storage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "rate_snapshot")
data class RateSnapshotEntity(@PrimaryKey val sourceId: String, val baseCode: String, val date: String, val fetchedAt: Long)

@Entity(
    tableName = "rate",
    primaryKeys = ["sourceId", "code"],
    foreignKeys = [
        ForeignKey(
            entity = RateSnapshotEntity::class,
            parentColumns = ["sourceId"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RateEntity(val sourceId: String, val code: String, val value: Double)
```

`RateSnapshotDao.kt`:

```kotlin
package ru.fasdev.ratex.currency.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class RateSnapshotDao {
    @Query("SELECT * FROM rate_snapshot WHERE sourceId = :sourceId")
    abstract suspend fun getSnapshot(sourceId: String): RateSnapshotEntity?

    @Query("SELECT * FROM rate WHERE sourceId = :sourceId")
    abstract suspend fun getRates(sourceId: String): List<RateEntity>

    @Insert
    abstract suspend fun insertSnapshot(snapshot: RateSnapshotEntity)

    @Insert
    abstract suspend fun insertRates(rates: List<RateEntity>)

    // Строки rate удаляются каскадно
    @Query("DELETE FROM rate_snapshot WHERE sourceId = :sourceId")
    abstract suspend fun deleteSnapshot(sourceId: String)

    @Transaction
    open suspend fun replaceSnapshot(snapshot: RateSnapshotEntity, rates: List<RateEntity>) {
        deleteSnapshot(snapshot.sourceId)
        insertSnapshot(snapshot)
        insertRates(rates)
    }

    @Transaction
    open suspend fun loadSnapshot(sourceId: String): Pair<RateSnapshotEntity, List<RateEntity>>? {
        val snapshot = getSnapshot(sourceId) ?: return null
        return snapshot to getRates(sourceId)
    }
}
```

`CurrencyDatabase.kt`:

```kotlin
package ru.fasdev.ratex.currency.data.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RateSnapshotEntity::class, RateEntity::class], version = 1, exportSchema = true)
abstract class CurrencyDatabase : RoomDatabase() {
    abstract fun rateSnapshotDao(): RateSnapshotDao

    companion object {
        const val NAME = "ratex.db"
    }
}
```

`RoomRateSnapshotStorage.kt`:

```kotlin
package ru.fasdev.ratex.currency.data.storage

import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

class RoomRateSnapshotStorage(private val dao: RateSnapshotDao) : RateSnapshotStorage {
    override suspend fun load(sourceId: String): StoredSnapshot? {
        val (snapshot, rates) = dao.loadSnapshot(sourceId) ?: return null

        return StoredSnapshot(
            snapshot = RateSnapshotDomain(snapshot.baseCode, snapshot.date, rates.associate { it.code to it.value }),
            fetchedAt = snapshot.fetchedAt
        )
    }

    override suspend fun save(sourceId: String, snapshot: RateSnapshotDomain, fetchedAt: Long) {
        dao.replaceSnapshot(
            RateSnapshotEntity(sourceId, snapshot.baseCode, snapshot.date, fetchedAt),
            snapshot.rates.map { RateEntity(sourceId, it.key, it.value) }
        )
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*RoomRateSnapshotStorageTest"`
Expected: PASS. Проверить, что появился `app/schemas/ru.fasdev.ratex.currency.data.storage.CurrencyDatabase/1.json`.

Если KSP/Room не собирается из-за несовместимости с AGP 9.4.1 или Kotlin 2.4.20 — остановиться и сообщить пользователю точную ошибку (не подбирать версии вслепую).

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/schemas app/src/main/java/ru/fasdev/ratex/currency/data/storage app/src/test/java/ru/fasdev/ratex/currency/data/storage
git commit -m "$(cat <<'EOF'
Add Room storage for rate snapshots

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Контракт источника и источник ЕЦБ

**Files:**
- Create: `app/src/main/java/ru/fasdev/ratex/currency/data/source/CurrencyRateSource.kt`
- Create: `app/src/main/java/ru/fasdev/ratex/currency/data/source/ecb/EcbXmlParser.kt`
- Create: `app/src/main/java/ru/fasdev/ratex/currency/data/source/ecb/EcbRateSource.kt`
- Test: `app/src/test/java/ru/fasdev/ratex/currency/data/source/ecb/EcbTestData.kt`
- Test: `app/src/test/java/ru/fasdev/ratex/currency/data/source/ecb/EcbXmlParserTest.kt`
- Test: `app/src/test/java/ru/fasdev/ratex/currency/data/source/ecb/EcbRateSourceTest.kt`

**Interfaces:**
- Consumes: `RateSnapshotDomain` (Task 1).
- Produces:
  - `interface CurrencyRateSource { val id: String; val baseCode: String; val refreshInterval: Duration; suspend fun fetch(): RateSnapshotDomain }` (`Duration` = `kotlin.time.Duration`)
  - `internal object EcbXmlParser { fun parse(xml: String): RateSnapshotDomain }` — бросает `IllegalStateException`, если нет даты или ни одного валидного курса; `XmlPullParserException` на некорректном XML.
  - `class EcbRateSource(httpClient: HttpClient, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : CurrencyRateSource`, `EcbRateSource.ID = "ecb"`, `EcbRateSource.URL`.

- [ ] **Step 1: Write the fixtures and failing tests**

`EcbTestData.kt` (структура файла ЕЦБ проверена 2026-10-06; список валют сокращён):

```kotlin
package ru.fasdev.ratex.currency.data.source.ecb

object EcbTestData {
    val XML_DAILY: String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gesmes:Envelope xmlns:gesmes="http://www.gesmes.org/xml/2002-08-01" xmlns="http://www.ecb.int/vocabulary/2002-08-01/eurofxref">
            <gesmes:subject>Reference rates</gesmes:subject>
            <gesmes:Sender>
                <gesmes:name>European Central Bank</gesmes:name>
            </gesmes:Sender>
            <Cube>
                <Cube time='2026-10-05'>
                    <Cube currency='USD' rate='1.0850'/>
                    <Cube currency='JPY' rate='162.40'/>
                    <Cube currency='GBP' rate='0.8412'/>
                    <Cube currency='PLN' rate='4.2791'/>
                </Cube>
            </Cube>
        </gesmes:Envelope>
    """.trimIndent()

    val XML_WITH_BAD_RATES: String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gesmes:Envelope xmlns:gesmes="http://www.gesmes.org/xml/2002-08-01" xmlns="http://www.ecb.int/vocabulary/2002-08-01/eurofxref">
            <Cube>
                <Cube time='2026-10-05'>
                    <Cube currency='USD' rate='1.0850'/>
                    <Cube currency='JPY' rate='0'/>
                    <Cube currency='GBP' rate='-0.84'/>
                    <Cube currency='PLN' rate='abc'/>
                    <Cube currency='CHF' rate='NaN'/>
                </Cube>
            </Cube>
        </gesmes:Envelope>
    """.trimIndent()

    val XML_NO_RATES: String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gesmes:Envelope xmlns:gesmes="http://www.gesmes.org/xml/2002-08-01" xmlns="http://www.ecb.int/vocabulary/2002-08-01/eurofxref">
            <Cube>
                <Cube time='2026-10-05'/>
            </Cube>
        </gesmes:Envelope>
    """.trimIndent()

    const val HTML_ERROR_PAGE: String = "<html><body><h1>503 Service Unavailable</h1></body></html>"
    const val NOT_XML: String = "this is not xml at all <<<"
}
```

`EcbXmlParserTest.kt` (Robolectric нужен: используется `android.util.Xml`):

```kotlin
package ru.fasdev.ratex.currency.data.source.ecb

import android.os.Build
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class EcbXmlParserTest {
    @Test
    fun testParseDaily() {
        val result = EcbXmlParser.parse(EcbTestData.XML_DAILY)

        assertThat(result.baseCode).isEqualTo("EUR")
        assertThat(result.date).isEqualTo("2026-10-05")
        assertThat(result.rates).containsExactlyEntriesOf(
            linkedMapOf("USD" to 1.0850, "JPY" to 162.40, "GBP" to 0.8412, "PLN" to 4.2791)
        )
    }

    @Test
    fun testParseSkipsNonPositiveAndNonNumericRates() {
        val result = EcbXmlParser.parse(EcbTestData.XML_WITH_BAD_RATES)

        assertThat(result.rates).containsOnlyKeys("USD")
    }

    @Test
    fun testParseWithoutRatesThrows() {
        val error = runCatching { EcbXmlParser.parse(EcbTestData.XML_NO_RATES) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testParseHtmlErrorPageThrows() {
        val error = runCatching { EcbXmlParser.parse(EcbTestData.HTML_ERROR_PAGE) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testParseMalformedXmlThrows() {
        val error = runCatching { EcbXmlParser.parse(EcbTestData.NOT_XML) }.exceptionOrNull()

        assertThat(error).isNotNull()
    }
}
```

`EcbRateSourceTest.kt`:

```kotlin
package ru.fasdev.ratex.currency.data.source.ecb

import android.os.Build
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class EcbRateSourceTest {
    private var status: HttpStatusCode = HttpStatusCode.OK
    private var body: String = EcbTestData.XML_DAILY
    private var requestedUrl: String? = null

    private lateinit var httpClient: HttpClient
    private lateinit var source: EcbRateSource

    @Before
    fun setUp() {
        val engine = MockEngine { request ->
            requestedUrl = request.url.toString()
            respond(body, status, headersOf(HttpHeaders.ContentType, "text/xml"))
        }

        httpClient = HttpClient(engine) { expectSuccess = true }
        source = EcbRateSource(httpClient, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        httpClient.close()
    }

    @Test
    fun testSourceConditions() {
        assertThat(source.id).isEqualTo("ecb")
        assertThat(source.baseCode).isEqualTo("EUR")
        assertThat(source.refreshInterval).isEqualTo(4.hours)
    }

    @Test
    fun testFetch() = runTest {
        val result = source.fetch()

        assertThat(requestedUrl).isEqualTo("https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml")
        assertThat(result.baseCode).isEqualTo("EUR")
        assertThat(result.rates).containsKeys("USD", "JPY", "GBP", "PLN")
    }

    @Test
    fun testFetchHttpError() = runTest {
        status = HttpStatusCode.InternalServerError
        body = "error"

        val error = runCatching { source.fetch() }.exceptionOrNull()

        assertThat(error).isInstanceOf(ResponseException::class.java)
    }

    @Test
    fun testFetchHtmlInsteadOfXmlThrows() = runTest {
        body = EcbTestData.HTML_ERROR_PAGE

        val error = runCatching { source.fetch() }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*EcbXmlParserTest" --tests "*EcbRateSourceTest"`
Expected: FAIL (compilation error: unresolved `EcbXmlParser`, `EcbRateSource`).

- [ ] **Step 3: Write the implementation**

`CurrencyRateSource.kt`:

```kotlin
package ru.fasdev.ratex.currency.data.source

import kotlin.time.Duration
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

/**
 * Первичный источник курсов. Знает условия источника: откуда брать, в какой валюте он считает и как часто его имеет смысл опрашивать.
 * Кэш и пересчёт под выбранную пользователем базу — задача репозитория и Domain.
 */
interface CurrencyRateSource {
    /** Стабильный ключ источника: под ним снимок хранится в [ru.fasdev.ratex.currency.data.storage.RateSnapshotStorage]. */
    val id: String

    /** Валюта, к которой источник публикует курсы. */
    val baseCode: String

    /** Через сколько снимок считается устаревшим. */
    val refreshInterval: Duration

    suspend fun fetch(): RateSnapshotDomain
}
```

`EcbXmlParser.kt`:

```kotlin
package ru.fasdev.ratex.currency.data.source.ecb

import android.util.Xml
import java.io.StringReader
import org.xmlpull.v1.XmlPullParser
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

internal object EcbXmlParser {
    const val BASE_CODE = "EUR"

    /**
     * Структура: `<Cube><Cube time="YYYY-MM-DD"><Cube currency="USD" rate="1.08"/>...`.
     * Курсы не больше нуля, не числа и NaN/Infinity пропускаются: из них нельзя считать кросс-курс.
     */
    fun parse(xml: String): RateSnapshotDomain {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        var date: String? = null
        val rates = LinkedHashMap<String, Double>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "Cube") {
                val time = parser.getAttributeValue(null, "time")
                val currency = parser.getAttributeValue(null, "currency")
                val rate = parser.getAttributeValue(null, "rate")?.toDoubleOrNull()

                if (time != null) {
                    date = time
                } else if (currency != null && rate != null && rate > 0.0 && rate.isFinite()) {
                    rates[currency] = rate
                }
            }
            event = parser.next()
        }

        check(date != null) { "ECB response has no date" }
        check(rates.isNotEmpty()) { "ECB response has no rates" }

        return RateSnapshotDomain(BASE_CODE, date, rates)
    }
}
```

`EcbRateSource.kt`:

```kotlin
package ru.fasdev.ratex.currency.data.source.ecb

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fasdev.ratex.currency.data.source.CurrencyRateSource
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

/**
 * Референсные курсы ЕЦБ: к EUR, около 30 валют, обновляются в рабочие дни около 16:00 CET. Ключ не нужен.
 * ЕЦБ публикует курсы «для информации» и не рекомендует использовать их для транзакций. RUB в файле нет (приостановлен с 2022-03-01).
 */
class EcbRateSource(private val httpClient: HttpClient, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) :
    CurrencyRateSource {
    override val id: String = ID
    override val baseCode: String = EcbXmlParser.BASE_CODE
    override val refreshInterval: Duration = 4.hours

    override suspend fun fetch(): RateSnapshotDomain = withContext(ioDispatcher) {
        EcbXmlParser.parse(httpClient.get(URL).bodyAsText())
    }

    companion object {
        const val ID = "ecb"
        const val URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*EcbXmlParserTest" --tests "*EcbRateSourceTest"`
Expected: PASS.

Если `testParseMalformedXmlThrows` падает на `NOT_XML` без исключения — проверить, что парсер не терпит текст вне корня; ожидаемо `XmlPullParserException`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/ru/fasdev/ratex/currency/data/source app/src/test/java/ru/fasdev/ratex/currency/data/source
git commit -m "$(cat <<'EOF'
Add CurrencyRateSource contract and ECB source

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Переключение репозиториев, интерактора и DI на источник

Контракт `CurrencyRateRepo` меняется (`getSnapshot()` вместо `getExchangeRates()`), поэтому репозитории, интерактор и DI переключаются одной задачей: иначе проект не соберётся.

**Files:**
- Modify: `app/src/main/java/ru/fasdev/ratex/currency/domain/boundaries/repo/CurrencyRateRepo.kt`
- Modify: `app/src/main/java/ru/fasdev/ratex/currency/data/repo/CurrencyRateRepoImpl.kt`
- Modify: `app/src/main/java/ru/fasdev/ratex/currency/data/repo/CurrencyBaseRepoImpl.kt`
- Modify: `app/src/main/java/ru/fasdev/ratex/currency/domain/interactor/CurrencyRateInteractorImpl.kt`
- Modify: `app/src/main/java/ru/fasdev/ratex/currency/di/module/CurrencyModule.kt`
- Modify: `app/src/main/java/ru/fasdev/ratex/main/di/component/AppComponent.kt`
- Modify: `app/src/main/java/ru/fasdev/ratex/core/di/module/HttpClientModule.kt`
- Create: `app/src/main/java/ru/fasdev/ratex/currency/di/module/CurrencyDatabaseModule.kt`
- Delete: `app/src/main/java/ru/fasdev/ratex/currency/data/api/` (весь каталог), `app/src/main/java/ru/fasdev/ratex/currency/data/dataStore/` (весь каталог)
- Delete: `app/src/test/java/ru/fasdev/ratex/currency/data/dataStore/` (весь каталог), `app/src/test/java/ru/fasdev/ratex/currency/data/TestData.kt`
- Test: `app/src/test/java/ru/fasdev/ratex/currency/data/Fakes.kt` (create)
- Test: `app/src/test/java/ru/fasdev/ratex/currency/data/repo/CurrencyRateRepoTest.kt` (rewrite)
- Test: `app/src/test/java/ru/fasdev/ratex/currency/data/repo/CurrencyBaseRepoTest.kt` (rewrite)
- Test: `app/src/test/java/ru/fasdev/ratex/currency/domain/interactor/CurrencyRateInteractorTest.kt` (rewrite)

**Interfaces:**
- Consumes: `RateSnapshotDomain`/`crossRates`/`availableCodes`/`isKnownCurrencyCode` (Task 1), `RateSnapshotStorage`/`StoredSnapshot`/`RoomRateSnapshotStorage`/`CurrencyDatabase` (Task 2), `CurrencyRateSource`/`EcbRateSource` (Task 3).
- Produces:
  - `CurrencyRateRepo.getSnapshot(): RateSnapshotDomain` (suspend) вместо `getExchangeRates()`.
  - `CurrencyRateRepoImpl(source: CurrencyRateSource, storage: RateSnapshotStorage, clock: () -> Long = System::currentTimeMillis)`
  - `CurrencyBaseRepoImpl(sharedPrefencesRepo, currencyImageRepo, currencyRateRepo: CurrencyRateRepo, ioDispatcher = Dispatchers.IO)`
  - `CurrencyRateInteractorImpl(currencyRateRepo, currencyBaseRepo, currencyImageRepo)`
  - Интерфейсы `CurrencyRateInteractor`, `CurrencyBaseInteractor`, `CurrencyBaseRepo` не меняются, ViewModel не трогаем.

- [ ] **Step 1: Write fakes and failing tests**

`Fakes.kt`:

```kotlin
package ru.fasdev.ratex.currency.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.delay
import ru.fasdev.ratex.currency.data.source.CurrencyRateSource
import ru.fasdev.ratex.currency.data.storage.RateSnapshotStorage
import ru.fasdev.ratex.currency.data.storage.StoredSnapshot
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

class FakeRateSource(
    var snapshot: RateSnapshotDomain,
    override val id: String = "fake",
    override val baseCode: String = snapshot.baseCode,
    override val refreshInterval: Duration = 4.hours
) : CurrencyRateSource {
    var fetchCount = 0
    var error: Exception? = null
    var fetchDelayMs = 0L

    override suspend fun fetch(): RateSnapshotDomain {
        fetchCount++
        if (fetchDelayMs > 0) delay(fetchDelayMs)
        error?.let { throw it }
        return snapshot
    }
}

class FakeRateSnapshotStorage : RateSnapshotStorage {
    val saved = mutableMapOf<String, StoredSnapshot>()
    var loadError: Exception? = null
    var saveError: Exception? = null

    override suspend fun load(sourceId: String): StoredSnapshot? {
        loadError?.let { throw it }
        return saved[sourceId]
    }

    override suspend fun save(sourceId: String, snapshot: RateSnapshotDomain, fetchedAt: Long) {
        saveError?.let { throw it }
        saved[sourceId] = StoredSnapshot(snapshot, fetchedAt)
    }
}
```

`CurrencyRateRepoTest.kt` (полная замена):

```kotlin
package ru.fasdev.ratex.currency.data.repo

import java.io.IOException
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.fasdev.ratex.currency.data.FakeRateSnapshotStorage
import ru.fasdev.ratex.currency.data.FakeRateSource
import ru.fasdev.ratex.currency.data.storage.StoredSnapshot
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

class CurrencyRateRepoTest {
    private val snapshotV1 = RateSnapshotDomain("EUR", "2026-10-05", mapOf("USD" to 1.1))
    private val snapshotV2 = RateSnapshotDomain("EUR", "2026-10-06", mapOf("USD" to 1.2))

    private lateinit var source: FakeRateSource
    private lateinit var storage: FakeRateSnapshotStorage
    private var now = 10_000_000_000L

    @Before
    fun setUp() {
        source = FakeRateSource(snapshotV2)
        storage = FakeRateSnapshotStorage()
    }

    private fun createRepo() = CurrencyRateRepoImpl(source, storage) { now }

    private fun storedAgo(hours: Int, snapshot: RateSnapshotDomain = snapshotV1) =
        StoredSnapshot(snapshot, now - hours.hours.inWholeMilliseconds)

    @Test
    fun testEmptyStorageFetchesAndPersists() = runTest {
        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV2)
        assertThat(source.fetchCount).isEqualTo(1)
        assertThat(storage.saved["fake"]).isEqualTo(StoredSnapshot(snapshotV2, now))
    }

    @Test
    fun testFreshSnapshotInStorageIsUsedWithoutNetwork() = runTest {
        // «процесс перезапущен»: память пуста, снимок лежит на диске
        storage.saved["fake"] = storedAgo(hours = 1)

        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV1)
        assertThat(source.fetchCount).isEqualTo(0)
    }

    @Test
    fun testStaleSnapshotInStorageIsRefetched() = runTest {
        storage.saved["fake"] = storedAgo(hours = 5)

        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV2)
        assertThat(source.fetchCount).isEqualTo(1)
        assertThat(storage.saved["fake"]).isEqualTo(StoredSnapshot(snapshotV2, now))
    }

    @Test
    fun testSecondCallUsesMemory() = runTest {
        val repo = createRepo()

        repo.getSnapshot()
        repo.getSnapshot()

        assertThat(source.fetchCount).isEqualTo(1)
    }

    @Test
    fun testNetworkFailureReturnsStaleSnapshotAndKeepsStorage() = runTest {
        val stale = storedAgo(hours = 30)
        storage.saved["fake"] = stale
        source.error = IOException("no network")

        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV1)
        assertThat(storage.saved["fake"]).isEqualTo(stale)
    }

    @Test
    fun testNetworkFailureWithoutSnapshotThrows() = runTest {
        source.error = IOException("no network")

        val error = runCatching { createRepo().getSnapshot() }.exceptionOrNull()

        assertThat(error).isInstanceOf(IOException::class.java)
    }

    @Test
    fun testStoredSnapshotOfAnotherBaseIsIgnored() = runTest {
        storage.saved["fake"] = storedAgo(hours = 1, snapshot = RateSnapshotDomain("RUB", "2026-10-05", mapOf("USD" to 0.011)))

        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV2)
        assertThat(source.fetchCount).isEqualTo(1)
    }

    @Test
    fun testUnreadableStorageIsTreatedAsEmpty() = runTest {
        storage.loadError = IllegalStateException("database corrupted")

        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV2)
    }

    @Test
    fun testFailedSaveDoesNotFailFetch() = runTest {
        storage.saveError = IOException("disk full")

        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV2)
    }

    @Test
    fun testConcurrentCallsFetchOnce() = runTest {
        source.fetchDelayMs = 1_000

        val repo = createRepo()

        val results = (1..3).map { async { repo.getSnapshot() } }.awaitAll()

        assertThat(results).allMatch { it == snapshotV2 }
        assertThat(source.fetchCount).isEqualTo(1)
    }

    @Test
    fun testClockMovedBackMakesSnapshotStale() = runTest {
        storage.saved["fake"] = StoredSnapshot(snapshotV1, fetchedAt = now + 1.hours.inWholeMilliseconds)

        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV2)
        assertThat(source.fetchCount).isEqualTo(1)
    }
}
```

`CurrencyBaseRepoTest.kt` (полная замена):

```kotlin
package ru.fasdev.ratex.currency.data.repo

import java.util.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import ru.fasdev.ratex.core.domain.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

class CurrencyBaseRepoTest {
    @get:Rule val mockitoJUnit = MockitoJUnit.rule()

    @Mock lateinit var sharedPrefencesRepo: SharedPrefencesRepo

    @Mock lateinit var currencyImageRepo: CurrencyImageRepo

    @Mock lateinit var currencyRateRepo: CurrencyRateRepo

    lateinit var currencyBaseRepo: CurrencyBaseRepo

    private val defaultLocale = Locale.getDefault()
    private val snapshot = RateSnapshotDomain("EUR", "2026-10-05", mapOf("USD" to 1.1, "JPY" to 160.0, "ZZZ" to 3.0))

    @Before
    fun setUp() = runTest {
        Mockito.`when`(currencyRateRepo.getSnapshot()).thenReturn(snapshot)
        currencyBaseRepo = CurrencyBaseRepoImpl(sharedPrefencesRepo, currencyImageRepo, currencyRateRepo, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun testGetBaseCurrencyNullPreferencesUsesLocale() = runTest {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(null)
        Locale.setDefault(Locale.US)

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo("USD")
    }

    @Test
    fun testGetBaseCurrencyFromPreferences() = runTest {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn("JPY")

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo("JPY")
    }

    @Test
    fun testGetBaseCurrencySourceBaseIsAvailable() = runTest {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn("EUR")

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo("EUR")
    }

    @Test
    fun testGetBaseCurrencyFromPreferencesMissingInSourceFallsBackToSourceBase() = runTest {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn("RUB")

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo("EUR")
        Mockito.verify(sharedPrefencesRepo, Mockito.never()).setBaseCurrencyCode(Mockito.anyString())
    }

    @Test
    fun testGetBaseCurrencyLocaleMissingInSourceFallsBackToSourceBase() = runTest {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(null)
        Locale.setDefault(Locale("ru", "RU"))

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo("EUR")
    }

    @Test
    fun testGetBaseCurrencyLocaleWithoutCurrencyFallsBackToSourceBase() = runTest {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(null)
        Locale.setDefault(Locale.ROOT)

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo("EUR")
    }

    @Test
    fun testSetBaseCurrency() {
        val testCurrencyCode = "JPY"
        currencyBaseRepo.setBaseCurrency(CurrencyDomain.getInstance(testCurrencyCode))

        Mockito.verify(sharedPrefencesRepo).setBaseCurrencyCode(testCurrencyCode)
    }

    @Test
    fun testGetAvailableCurrenciesComeFromSnapshotWithoutUnknownCodes() = runTest {
        val result = currencyBaseRepo.getAvailableCurrencies()

        assertThat(result.map { it.currencyCode }).containsExactlyInAnyOrder("EUR", "USD", "JPY")
    }
}
```

`CurrencyRateInteractorTest.kt` (полная замена):

```kotlin
package ru.fasdev.ratex.currency.domain.interactor

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

class CurrencyRateInteractorTest {
    @get:Rule val mockitoJUnit = MockitoJUnit.rule()

    @Mock private lateinit var currencyRateRepo: CurrencyRateRepo

    @Mock private lateinit var currencyBaseRepo: CurrencyBaseRepo

    @Mock private lateinit var currencyImageRepo: CurrencyImageRepo

    private lateinit var currencyRateInteractor: CurrencyRateInteractor

    private val snapshot = RateSnapshotDomain(
        "EUR",
        "2026-10-05",
        mapOf("USD" to 2.0, "JPY" to 200.0, "GBP" to 0.5, "ZZZ" to 9.0)
    )

    @Before
    fun setUp() = runTest {
        Mockito.`when`(currencyRateRepo.getSnapshot()).thenReturn(snapshot)
        currencyRateInteractor = CurrencyRateInteractorImpl(currencyRateRepo, currencyBaseRepo, currencyImageRepo)
    }

    private suspend fun ratesFor(baseCode: String): Map<String, Double> {
        Mockito.`when`(currencyBaseRepo.getBaseCurrency()).thenReturn(CurrencyDomain.getInstance(baseCode))
        return currencyRateInteractor.getExchangeRates().associate { it.currency.currencyCode to it.rate }
    }

    @Test
    fun testGetExchangeRatesForSourceBase() = runTest {
        assertThat(ratesFor("EUR")).isEqualTo(mapOf("USD" to 2.0, "JPY" to 200.0, "GBP" to 0.5))
    }

    @Test
    fun testGetExchangeRatesForOtherBaseIsCrossRate() = runTest {
        assertThat(ratesFor("USD")).isEqualTo(mapOf("EUR" to 0.5, "JPY" to 100.0, "GBP" to 0.25))
    }

    @Test
    fun testGetExchangeRatesSortedByDisplayName() = runTest {
        Mockito.`when`(currencyBaseRepo.getBaseCurrency()).thenReturn(CurrencyDomain.getInstance("EUR"))

        val result = currencyRateInteractor.getExchangeRates()

        assertThat(result.map { it.currency.displayName }).isEqualTo(result.map { it.currency.displayName }.sorted())
    }

    @Test
    fun testGetExchangeRatesBaseMissingInSnapshotIsEmpty() = runTest {
        assertThat(ratesFor("RUB")).isEmpty()
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*CurrencyRateRepoTest" --tests "*CurrencyBaseRepoTest" --tests "*CurrencyRateInteractorTest"`
Expected: FAIL (compilation errors: `getSnapshot`, new constructor signatures).

- [ ] **Step 3: Switch Domain and Data to the snapshot**

`CurrencyRateRepo.kt` (полностью):

```kotlin
package ru.fasdev.ratex.currency.domain.boundaries.repo

import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

interface CurrencyRateRepo {
    suspend fun getSnapshot(): RateSnapshotDomain
}
```

`CurrencyRateRepoImpl.kt` (полностью):

```kotlin
package ru.fasdev.ratex.currency.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.fasdev.ratex.currency.data.source.CurrencyRateSource
import ru.fasdev.ratex.currency.data.storage.RateSnapshotStorage
import ru.fasdev.ratex.currency.data.storage.StoredSnapshot
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

/**
 * Снимок курсов источника: память → хранилище → сеть. Свежесть считается по [CurrencyRateSource.refreshInterval].
 * Сеть упала, а снимок есть (любой давности) — отдаём его. Параллельные вызовы делят один запрос.
 */
class CurrencyRateRepoImpl(
    private val source: CurrencyRateSource,
    private val storage: RateSnapshotStorage,
    private val clock: () -> Long = System::currentTimeMillis
) : CurrencyRateRepo {
    private val mutex = Mutex()
    private var memory: StoredSnapshot? = null

    override suspend fun getSnapshot(): RateSnapshotDomain = mutex.withLock {
        val cached = memory ?: loadFromStorage()?.also { memory = it }

        if (cached != null && isFresh(cached)) {
            return cached.snapshot
        }

        val fetched = try {
            source.fetch()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return cached?.snapshot ?: throw e
        }

        val stored = StoredSnapshot(fetched, clock())
        memory = stored
        persist(stored)

        fetched
    }

    private fun isFresh(stored: StoredSnapshot): Boolean {
        val age = clock() - stored.fetchedAt
        // age < 0 — часы переведены назад: такому снимку верить нельзя
        return age >= 0 && age < source.refreshInterval.inWholeMilliseconds
    }

    private suspend fun loadFromStorage(): StoredSnapshot? = try {
        storage.load(source.id)?.takeIf { it.snapshot.baseCode == source.baseCode }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private suspend fun persist(stored: StoredSnapshot) {
        try {
            storage.save(source.id, stored.snapshot, stored.fetchedAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Кэш не критичен: свежий снимок уже в памяти, в следующий раз запишем снова
        }
    }
}
```

`CurrencyBaseRepoImpl.kt` (полностью):

```kotlin
package ru.fasdev.ratex.currency.data.repo

import java.util.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fasdev.ratex.core.domain.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.extension.isKnownCurrencyCode
import ru.fasdev.ratex.currency.domain.entity.extension.toCurrencyDomain

class CurrencyBaseRepoImpl(
    val sharedPrefencesRepo: SharedPrefencesRepo,
    val currencyImageRepo: CurrencyImageRepo,
    val currencyRateRepo: CurrencyRateRepo,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CurrencyBaseRepo {
    /**
     * Сохранённая база, а если её нет — валюта локали. Если источник такой валюты не знает (RUB, BGN, HRK, локаль без валюты),
     * берём базу источника. Сохранённое значение при этом не перезаписывается.
     */
    override suspend fun getBaseCurrency(): CurrencyDomain {
        val snapshot = currencyRateRepo.getSnapshot()

        return withContext(ioDispatcher) {
            val preferredCode = sharedPrefencesRepo.getBaseCurrencyCode().takeUnless { it.isNullOrEmpty() }
                ?: runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull()

            val code = if (preferredCode != null && preferredCode in snapshot.availableCodes) preferredCode else snapshot.baseCode
            Currency.getInstance(code).toCurrencyDomain()
        }
    }

    override fun setBaseCurrency(baseCurrency: CurrencyDomain) {
        sharedPrefencesRepo.setBaseCurrencyCode(baseCurrency.currencyCode)
    }

    override suspend fun getAvailableCurrencies(): List<CurrencyDomain> {
        val snapshot = currencyRateRepo.getSnapshot()

        return withContext(ioDispatcher) {
            snapshot.availableCodes
                .filter { isKnownCurrencyCode(it) }
                .map { CurrencyDomain.getInstance(it, currencyImageRepo) }
        }
    }
}
```

`CurrencyRateInteractorImpl.kt` (полностью):

```kotlin
package ru.fasdev.ratex.currency.domain.interactor

import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.extension.isKnownCurrencyCode

class CurrencyRateInteractorImpl(
    val currencyRateRepo: CurrencyRateRepo,
    val currencyBaseRepo: CurrencyBaseRepo,
    val currencyImageRepo: CurrencyImageRepo
) : CurrencyRateInteractor {
    override suspend fun getExchangeRates(): List<RateCurrencyDomain> {
        val snapshot = currencyRateRepo.getSnapshot()
        val baseCurrency = currencyBaseRepo.getBaseCurrency()

        return snapshot
            .crossRates(baseCurrency.currencyCode)
            .filterKeys { isKnownCurrencyCode(it) }
            .map { RateCurrencyDomain(CurrencyDomain.getInstance(it.key, currencyImageRepo), it.value) }
            .sortedBy { it.currency.displayName }
    }
}
```

- [ ] **Step 4: Rewire DI**

`CurrencyDatabaseModule.kt` (создать):

```kotlin
package ru.fasdev.ratex.currency.di.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import ru.fasdev.ratex.currency.data.storage.CurrencyDatabase
import ru.fasdev.ratex.main.di.scope.AppScope

/** База в `AppComponent`: `CurrencyComponent` пересоздаётся вместе с Activity, а экземпляр Room-базы должен быть один. */
@Module
class CurrencyDatabaseModule {
    @Provides
    @AppScope
    fun provideCurrencyDatabase(context: Context): CurrencyDatabase = Room
        .databaseBuilder(context, CurrencyDatabase::class.java, CurrencyDatabase.NAME)
        .fallbackToDestructiveMigration(dropAllTables = true) // это кэш курсов, не пользовательские данные
        .build()
}
```

`AppComponent.kt`: в импорты добавить `ru.fasdev.ratex.currency.data.storage.CurrencyDatabase` и `ru.fasdev.ratex.currency.di.module.CurrencyDatabaseModule`; модули: `@Component(modules = [AppModule::class, SettingsModule::class, HttpClientModule::class, CurrencyDatabaseModule::class])`; в блок `// Child dependencies` добавить `fun currencyDatabase(): CurrencyDatabase`.

`HttpClientModule.kt`: удалить импорт `io.ktor.client.plugins.defaultRequest` и блок

```kotlin

        defaultRequest {
            url("https://api.exchangeratesapi.io/") // TODO: CHANGE BASE URL TO DYNAMIC
        }
```

`CurrencyModule.kt`: заменить импорты `ExchangeRateApi`, `ExchangeRateApiImpl`, `CurrencyRateDataStore`, `ExchangeRateDataStore` и `io.ktor.client.HttpClient` оставить; добавить импорты
`ru.fasdev.ratex.currency.data.source.CurrencyRateSource`, `ru.fasdev.ratex.currency.data.source.ecb.EcbRateSource`, `ru.fasdev.ratex.currency.data.storage.CurrencyDatabase`, `ru.fasdev.ratex.currency.data.storage.RateSnapshotStorage`, `ru.fasdev.ratex.currency.data.storage.RoomRateSnapshotStorage`. Заменить провайдеры `provideExchangeRateApi`, `provideCurrencyBaseRepo`, `currencyRateDataStore`, `provideCurrencyRateRepo`, `provideCurrencyRateInteractor` на:

```kotlin
    @Provides
    @CurrencyScope
    fun provideCurrencyRateSource(httpClient: HttpClient): CurrencyRateSource = EcbRateSource(httpClient)

    @Provides
    @CurrencyScope
    fun provideRateSnapshotStorage(database: CurrencyDatabase): RateSnapshotStorage = RoomRateSnapshotStorage(database.rateSnapshotDao())

    @Provides
    @CurrencyScope
    fun provideCurrencyRateRepo(source: CurrencyRateSource, storage: RateSnapshotStorage): CurrencyRateRepo =
        CurrencyRateRepoImpl(source, storage)

    @Provides
    @CurrencyScope
    fun provideCurrencyBaseRepo(
        sharedPrefencesRepo: SharedPrefencesRepo,
        currencyImageRepo: CurrencyImageRepo,
        currencyRateRepo: CurrencyRateRepo
    ): CurrencyBaseRepo = CurrencyBaseRepoImpl(sharedPrefencesRepo, currencyImageRepo, currencyRateRepo)

    @Provides
    @CurrencyScope
    fun provideCurrencyRateInteractor(
        currencyRateRepo: CurrencyRateRepo,
        currencyBaseRepo: CurrencyBaseRepo,
        currencyImageRepo: CurrencyImageRepo
    ): CurrencyRateInteractor = CurrencyRateInteractorImpl(currencyRateRepo, currencyBaseRepo, currencyImageRepo)
```

(Остальные провайдеры — `provideCurrencyImageRepo`, `currencyBaseInteractor`, `provideViewModelFactory` — без изменений.)

- [ ] **Step 5: Delete the old ExchangeRateApi code**

Убедиться, что `TestData` больше нигде не используется:

Run: `grep -rn "TestData\|ExchangeRateApi\|ExchangeRatesResponse\|CurrencyRateDataStore\|ExchangeRateDataStore" app/src`
Expected: совпадения только внутри удаляемых файлов.

Run: `git rm -r app/src/main/java/ru/fasdev/ratex/currency/data/api app/src/main/java/ru/fasdev/ratex/currency/data/dataStore app/src/test/java/ru/fasdev/ratex/currency/data/dataStore app/src/test/java/ru/fasdev/ratex/currency/data/TestData.kt`

- [ ] **Step 6: Run the full unit-test suite and build**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS, включая `ListCurrencyRateViewModelTest`, `SelectCurrencyViewModelTest` и `CurrencyBaseInteractorTest` без правок.

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (проверяет граф Dagger: `CurrencyDatabase` из `AppComponent` доходит до `CurrencyComponent`).

- [ ] **Step 7: Commit**

```bash
git add -A app/src gradle app/schemas
git commit -m "$(cat <<'EOF'
Switch rates to ECB source with Room-backed snapshot cache

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: Стиль, итоговая проверка и артефакт

**Files:**
- Create: `docs-ai/artifact/2026-10-06-source-data.md` (каталог в `.gitignore`, в git не попадает)

**Interfaces:**
- Consumes: результаты Task 1–4.

- [ ] **Step 1: Format and lint**

Run: `./gradlew ktlintFormat` затем `./gradlew ktlintCheck`
Expected: ktlintCheck без ошибок. Если формат что-то изменил — закоммитить отдельным коммитом `Format with ktlint`.

- [ ] **Step 2: Final verification**

Run: `./gradlew test assembleDebug ktlintCheck`
Expected: все три успешны.

Проверить вручную (по коду, без запуска приложения): `grep -rn "exchangeratesapi" app/src` — пусто; `grep -rn "arrayActualCodeCurrency" app/src` — пусто.

- [ ] **Step 3: Write the artifact**

Создать `docs-ai/artifact/2026-10-06-source-data.md` по образцу `2026-10-06-jdk-fix.md` (разделы: Проблема, Что сделано, Решения, Проверка, Отложено). Обязательное содержание:

- **Проблема:** ключ ExchangeRateApi протух, список валют узкий (33 захардкожены).
- **Что сделано:** контракт `CurrencyRateSource` (Data), `EcbRateSource` (XML ЕЦБ), Room-кэш (`rate_snapshot` / `rate`, ключ по `sourceId`), `CurrencyRateRepoImpl` (память → Room → сеть, TTL 4 ч, устаревший кэш при ошибке сети), кросс-пересчёт в `RateSnapshotDomain.crossRates` и интеракторе, список валют из снимка.
- **Источники:** таблица из спеки. ЕЦБ реализован (29 валют, база EUR, ~16:00 CET). **TODO:** ЦБ РФ (~55 валют, база RUB, XML windows-1251; нужен для RUB), США (ФРС H.10 / Treasury; контракт не изучался).
- **Пометка (по просьбе пользователя): мост между источниками не реализован.** Идея: если у источника нет курса к базе пользователя, но есть у другого, считать через валюту, общую для обоих (ЦБ РФ: RUB→USD и ЕЦБ: EUR→USD, мост по USD). Контракт уже даёт `baseCode` и состав валют каждого источника, схема Room хранит снимки по `sourceId`, поэтому второй источник не ломает интерфейсы. Сейчас реализован только пересчёт внутри одного источника через его базу.
- **Решения:** RUB/BGN/HRK недоступны, пока нет ЦБ РФ; база по умолчанию откатывается на EUR, сохранённое значение в SharedPrefs не перезаписывается; единый `RateSnapshotDomain` вместо отдельного Data-класса; Room без Gradle-плагина (схема через KSP-аргумент), `fallbackToDestructiveMigration`; `fetchedAt` по часам устройства (перевод часов назад → снимок считается устаревшим); дисклеймер ЕЦБ: курсы информационные, не для транзакций; сеть до ecb.europa.eu из `curl` в песочнице не проходит (SSL через прокси), формат проверен через WebFetch.
- **Проверка:** фактический вывод `./gradlew test assembleDebug ktlintCheck` (вставить итог, не пересказ ожиданий).
- **Отложено:** второй и третий источники, мост между источниками, привязка TTL к расписанию ЕЦБ (16:00 CET), текст ошибки для пользователя вместо `e.message` во ViewModel (существующий TODO).

- [ ] **Step 4: Report**

Сообщить пользователю: ветка `feature/source-data`, список коммитов (`git log --oneline master..HEAD`), результат проверок, путь к артефакту, что RUB временно недоступен.

---

## Self-Review

**Spec coverage:**
- Контракт источника в Data (`id`, `baseCode`, `refreshInterval`, `fetch`) → Task 3. `EcbRateSource` с URL/форматом/интервалом 4 ч → Task 3.
- Room-кэш, схема `rate_snapshot`/`rate`, транзакция замены, `ratex.db`, v1, экспорт схемы, destructive fallback → Task 2 и Task 4 (DI-модуль).
- `RateSnapshotStorage` как интерфейс, репозиторий зависит от интерфейса → Task 2, Task 4.
- «Память → Room → сеть», TTL, устаревший кэш при ошибке сети, исключение без кэша, игнор чужой `baseCode` → Task 4 (`CurrencyRateRepoTest`).
- Кросс-пересчёт в Domain (три случая одной формулой), пропуск базы и неизвестных валют → Task 1, Task 4 (интерактор).
- Список валют из источника, массив удалён → Task 4 (`CurrencyBaseRepoImpl`).
- Откат на базу источника, SharedPrefs не перезаписывается → Task 4.
- Пропуск кодов вне `java.util.Currency` → Task 1 (`isKnownCurrencyCode`), Task 4.
- Удаление `ExchangeRateApi*`, `ExchangeRatesResponse`, DataStore, правка `HttpClientModule`/`CurrencyModule` → Task 4.
- Тесты (MockEngine для ЕЦБ, интерактор, репозитории, Room на Robolectric) → Tasks 1–4.
- Артефакт с пометкой про мост между источниками, RUB, дисклеймером → Task 5.
- Отклонение от спеки (единый `RateSnapshotDomain`) оговорено в шапке плана.

**Placeholder scan:** TBD/«добавить обработку ошибок» нет; шаг 3 Task 5 описывает содержимое артефакта списком обязательных пунктов, а не кодом, так как это документ.

**Type consistency:** `getSnapshot()`, `RateSnapshotDomain(baseCode, date, rates)`, `StoredSnapshot(snapshot, fetchedAt)`, `RateSnapshotStorage.load/save`, `CurrencyRateRepoImpl(source, storage, clock)`, `CurrencyBaseRepoImpl(prefs, imageRepo, rateRepo, dispatcher)`, `CurrencyRateInteractorImpl(rateRepo, baseRepo, imageRepo)`, `EcbRateSource.ID/URL`, `CurrencyDatabase.NAME`/`rateSnapshotDao()` согласованы между задачами и тестами.
