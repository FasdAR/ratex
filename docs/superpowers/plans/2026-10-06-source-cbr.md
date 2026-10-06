# Источник ЦБ РФ и слияние источников Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Добавить источник курсов ЦБ РФ (база RUB) и объединить его с ЕЦБ в один снимок через мост по общей валюте.

**Architecture:** `CbrRateSource` реализует существующий `CurrencyRateSource`. Каждый источник получает свой `CurrencyRateRepoImpl` (кэш по `sourceId`). Поверх них `MergedCurrencyRateRepo` реализует `CurrencyRateRepo` и отдаёт один `RateSnapshotDomain`, поэтому Domain/UI/Room не меняются. Приоритет задаёт порядок констант enum `RateSourcePriority`.

**Tech Stack:** Kotlin, Ktor (MockEngine в тестах), Robolectric (XmlPull-парсер), Mockito, AssertJ, Dagger 2.

**Spec:** `docs/superpowers/specs/2026-10-06-source-cbr-design.md`. Постановка: `docs-ai/planning/03-source_cbr.md`.

## Global Constraints

- Пакет `ru.fasdev.ratex`, один модуль `:app`; зависимости `ui → domain ← data`, Domain не меняется.
- Контракт `CurrencyRateSource` (`id`, `baseCode`, `refreshInterval`, `suspend fun fetch()`) не меняется.
- ЦБ РФ: `id = "cbr"`, `baseCode = "RUB"`, `refreshInterval = 4.hours`, URL `https://www.cbr.ru/scripts/XML_daily.asp`, кодировка windows-1251, курс к RUB = `Nominal / Value`, десятичный разделитель — запятая.
- Блокирующая/сетевая работа через `withContext(ioDispatcher)`, диспетчер в конструкторе (по умолчанию `Dispatchers.IO`); `CancellationException` всегда rethrow.
- Схема Room не меняется. Тесты лежат в тех же пакетах, что и код.
- Стиль: `./gradlew ktlintCheck` (правила в `.editorconfig`).
- Gradle запускать вне песочницы Claude Code (`dangerouslyDisableSandbox`), т.к. она не пишет в `~/.gradle`.
- В конце — запись результата в `docs-ai/artifact`. Коммиты заканчиваются строкой `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

## Review Focus

- Источник вернул курсы без общей валюты с главным: снимок пропускается, не падает и не портит курсы (тест в Task 3).
- Один из источников недоступен (нет сети до одного хоста): список валют и курсы остаются на втором (Task 3).
- Валюта есть в обоих источниках: курс главного не перезаписывается (Task 3).
- ЦБ РФ с `Nominal` ≠ 1 (JPY 100, KZT 100): курс не искажается в 100 раз (Task 1).
- ЦБ РФ отдал HTML/пустой ответ или мусорные значения: исключение/пропуск, а не пустой снимок (Task 1).
- Перестановка констант `RateSourcePriority` меняет приоритет (Task 3/4).

---

## File Structure

- Create `app/src/main/java/ru/fasdev/ratex/currency/data/source/cbr/CbrXmlParser.kt` — разбор XML ЦБ РФ.
- Create `app/src/main/java/ru/fasdev/ratex/currency/data/source/cbr/CbrRateSource.kt` — HTTP + декодирование.
- Create `app/src/main/java/ru/fasdev/ratex/currency/data/source/RateSourcePriority.kt` — enum приоритета и фабрика источников.
- Create `app/src/main/java/ru/fasdev/ratex/currency/data/repo/MergedCurrencyRateRepo.kt` — слияние.
- Modify `app/src/main/java/ru/fasdev/ratex/currency/di/module/CurrencyModule.kt` — сборка.
- Tests: `.../data/source/cbr/CbrTestData.kt`, `CbrXmlParserTest.kt`, `CbrRateSourceTest.kt`; `.../data/repo/MergedCurrencyRateRepoTest.kt`; modify `CurrencyBaseRepoTest.kt`.
- Create `docs-ai/artifact/2026-10-06-source-cbr.md`.

(Все пути ниже — от корня репозитория; `<T>` = `app/src/test/java/ru/fasdev/ratex/currency`, `<M>` = `app/src/main/java/ru/fasdev/ratex/currency`.)

---

### Task 1: CbrXmlParser

**Files:**
- Create: `<M>/data/source/cbr/CbrXmlParser.kt`
- Create: `<T>/data/source/cbr/CbrTestData.kt`
- Test: `<T>/data/source/cbr/CbrXmlParserTest.kt`

**Interfaces:**
- Consumes: `RateSnapshotDomain(baseCode: String, date: String, rates: Map<String, Double>)`.
- Produces: `internal object CbrXmlParser { const val BASE_CODE = "RUB"; fun parse(xml: String): RateSnapshotDomain }` (дата в ISO `yyyy-MM-dd`); `CbrTestData.XML_DAILY`, `XML_WITH_BAD_VALUES`, `XML_NO_RATES`, `XML_BAD_DATE`, `HTML_ERROR_PAGE` (используются в Task 2).

- [ ] **Step 1: Write the failing test and fixtures**

`CbrTestData.kt`:

```kotlin
package ru.fasdev.ratex.currency.data.source.cbr

object CbrTestData {
    val XML_DAILY: String = """
        <?xml version="1.0" encoding="windows-1251"?>
        <ValCurs Date="05.10.2026" name="Foreign Currency Market">
            <Valute ID="R01235">
                <NumCode>840</NumCode>
                <CharCode>USD</CharCode>
                <Nominal>1</Nominal>
                <Name>Доллар США</Name>
                <Value>92,5000</Value>
                <VunitRate>92,5</VunitRate>
            </Valute>
            <Valute ID="R01820">
                <NumCode>392</NumCode>
                <CharCode>JPY</CharCode>
                <Nominal>100</Nominal>
                <Name>Японских иен</Name>
                <Value>61,2000</Value>
                <VunitRate>0,612</VunitRate>
            </Valute>
            <Valute ID="R01335">
                <NumCode>398</NumCode>
                <CharCode>KZT</CharCode>
                <Nominal>100</Nominal>
                <Name>Казахстанских тенге</Name>
                <Value>18,9000</Value>
                <VunitRate>0,189</VunitRate>
            </Valute>
            <Valute ID="R01100">
                <NumCode>975</NumCode>
                <CharCode>BGN</CharCode>
                <Nominal>1</Nominal>
                <Name>Болгарский лев</Name>
                <Value>50,0000</Value>
                <VunitRate>50</VunitRate>
            </Valute>
        </ValCurs>
    """.trimIndent()

    val XML_WITH_BAD_VALUES: String = """
        <?xml version="1.0" encoding="windows-1251"?>
        <ValCurs Date="05.10.2026" name="Foreign Currency Market">
            <Valute><CharCode>USD</CharCode><Nominal>1</Nominal><Value>92,5000</Value></Valute>
            <Valute><CharCode>JPY</CharCode><Nominal>0</Nominal><Value>61,2000</Value></Valute>
            <Valute><CharCode>KZT</CharCode><Nominal>100</Nominal><Value>0</Value></Valute>
            <Valute><CharCode>BGN</CharCode><Nominal>1</Nominal><Value>abc</Value></Valute>
            <Valute><CharCode>CNY</CharCode><Nominal>x</Nominal><Value>12,5</Value></Valute>
            <Valute><Nominal>1</Nominal><Value>10,0</Value></Valute>
        </ValCurs>
    """.trimIndent()

    val XML_NO_RATES: String = """
        <?xml version="1.0" encoding="windows-1251"?>
        <ValCurs Date="05.10.2026" name="Foreign Currency Market"></ValCurs>
    """.trimIndent()

    val XML_BAD_DATE: String = """
        <?xml version="1.0" encoding="windows-1251"?>
        <ValCurs Date="yesterday" name="Foreign Currency Market">
            <Valute><CharCode>USD</CharCode><Nominal>1</Nominal><Value>92,5000</Value></Valute>
        </ValCurs>
    """.trimIndent()

    const val HTML_ERROR_PAGE: String = "<html><body><h1>503 Service Unavailable</h1></body></html>"
}
```

`CbrXmlParserTest.kt`:

```kotlin
package ru.fasdev.ratex.currency.data.source.cbr

import android.os.Build
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class CbrXmlParserTest {
    @Test
    fun testParseDailyUsesNominalAndComma() {
        val result = CbrXmlParser.parse(CbrTestData.XML_DAILY)

        assertThat(result.baseCode).isEqualTo("RUB")
        assertThat(result.date).isEqualTo("2026-10-05")
        assertThat(result.rates).containsOnlyKeys("USD", "JPY", "KZT", "BGN")
        // 1 RUB = Nominal / Value единиц валюты
        assertThat(result.rates.getValue("USD")).isCloseTo(1 / 92.5, Offset.offset(1e-9))
        assertThat(result.rates.getValue("JPY")).isCloseTo(100 / 61.2, Offset.offset(1e-9))
        assertThat(result.rates.getValue("KZT")).isCloseTo(100 / 18.9, Offset.offset(1e-9))
        assertThat(result.rates.getValue("BGN")).isCloseTo(1 / 50.0, Offset.offset(1e-9))
    }

    @Test
    fun testParseSkipsInvalidEntries() {
        val result = CbrXmlParser.parse(CbrTestData.XML_WITH_BAD_VALUES)

        assertThat(result.rates).containsOnlyKeys("USD")
    }

    @Test
    fun testParseWithoutRatesThrows() {
        val error = runCatching { CbrXmlParser.parse(CbrTestData.XML_NO_RATES) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testParseBadDateThrows() {
        val error = runCatching { CbrXmlParser.parse(CbrTestData.XML_BAD_DATE) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testParseHtmlErrorPageThrows() {
        val error = runCatching { CbrXmlParser.parse(CbrTestData.HTML_ERROR_PAGE) }.exceptionOrNull()

        assertThat(error).isNotNull()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "ru.fasdev.ratex.currency.data.source.cbr.CbrXmlParserTest"`
Expected: FAIL (compilation: unresolved reference `CbrXmlParser`).

- [ ] **Step 3: Write minimal implementation**

```kotlin
package ru.fasdev.ratex.currency.data.source.cbr

import android.util.Xml
import java.io.StringReader
import org.xmlpull.v1.XmlPullParser
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

internal object CbrXmlParser {
    const val BASE_CODE = "RUB"

    private val DATE_REGEX = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")

    /**
     * Структура: `<ValCurs Date="dd.MM.yyyy"><Valute><CharCode>USD</CharCode><Nominal>1</Nominal><Value>92,5</Value></Valute>...`.
     * `Value` — рублей за `Nominal` единиц валюты, поэтому «1 RUB = Nominal / Value». Запись с кривым кодом, `Nominal` или `Value`
     * (не число, ≤ 0, NaN/Infinity) пропускается: из неё нельзя считать кросс-курс.
     */
    fun parse(xml: String): RateSnapshotDomain {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        var date: String? = null
        val rates = LinkedHashMap<String, Double>()

        var code: String? = null
        var nominal: Double? = null
        var value: Double? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "ValCurs" -> date = parseDate(parser.getAttributeValue(null, "Date"))
                    "Valute" -> {
                        code = null
                        nominal = null
                        value = null
                    }
                    "CharCode" -> code = parser.nextText().trim()
                    "Nominal" -> nominal = parser.nextText().trim().toDoubleOrNull()
                    "Value" -> value = parser.nextText().trim().replace(',', '.').toDoubleOrNull()
                }
            } else if (event == XmlPullParser.END_TAG && parser.name == "Valute") {
                val rate = if (nominal != null && value != null && nominal > 0.0 && value > 0.0) nominal / value else null
                if (!code.isNullOrEmpty() && rate != null && rate > 0.0 && rate.isFinite()) {
                    rates[code] = rate
                }
            }
            event = parser.next()
        }

        check(date != null) { "CBR response has no date" }
        check(rates.isNotEmpty()) { "CBR response has no rates" }

        return RateSnapshotDomain(BASE_CODE, date, rates)
    }

    private fun parseDate(raw: String?): String? {
        val match = raw?.let { DATE_REGEX.matchEntire(it.trim()) } ?: return null
        val (day, month, year) = match.destructured
        return "$year-$month-$day"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "ru.fasdev.ratex.currency.data.source.cbr.CbrXmlParserTest"`
Expected: PASS (5 tests). Если `testParseHtmlErrorPageThrows` падает по другой причине — тест требует лишь любое исключение.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/ru/fasdev/ratex/currency/data/source/cbr app/src/test/java/ru/fasdev/ratex/currency/data/source/cbr
git commit -m "feat: add CBR XML parser" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: CbrRateSource

**Files:**
- Create: `<M>/data/source/cbr/CbrRateSource.kt`
- Test: `<T>/data/source/cbr/CbrRateSourceTest.kt`

**Interfaces:**
- Consumes: `CbrXmlParser.parse(String): RateSnapshotDomain`, `CbrXmlParser.BASE_CODE`, `CbrTestData.*` (Task 1).
- Produces: `class CbrRateSource(httpClient: HttpClient, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : CurrencyRateSource` с `companion { const val ID = "cbr"; const val URL = "https://www.cbr.ru/scripts/XML_daily.asp" }`.

- [ ] **Step 1: Write the failing test**

```kotlin
package ru.fasdev.ratex.currency.data.source.cbr

import android.os.Build
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.nio.charset.Charset
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
class CbrRateSourceTest {
    private var status: HttpStatusCode = HttpStatusCode.OK
    private var body: ByteArray = CbrTestData.XML_DAILY.toByteArray(Charset.forName("windows-1251"))
    private var requestedUrl: String? = null

    private lateinit var httpClient: HttpClient
    private lateinit var source: CbrRateSource

    @Before
    fun setUp() {
        val engine = MockEngine { request ->
            requestedUrl = request.url.toString()
            // Без charset в заголовке: источник обязан декодировать windows-1251 сам
            respond(body, status, headersOf(HttpHeaders.ContentType, "text/xml"))
        }

        httpClient = HttpClient(engine) { expectSuccess = true }
        source = CbrRateSource(httpClient, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        httpClient.close()
    }

    @Test
    fun testSourceConditions() {
        assertThat(source.id).isEqualTo("cbr")
        assertThat(source.baseCode).isEqualTo("RUB")
        assertThat(source.refreshInterval).isEqualTo(4.hours)
    }

    @Test
    fun testFetchDecodesWindows1251() = runTest {
        val result = source.fetch()

        assertThat(requestedUrl).isEqualTo("https://www.cbr.ru/scripts/XML_daily.asp")
        assertThat(result.baseCode).isEqualTo("RUB")
        assertThat(result.rates).containsKeys("USD", "JPY", "KZT", "BGN")
    }

    @Test
    fun testFetchHttpError() = runTest {
        status = HttpStatusCode.InternalServerError
        body = "error".toByteArray()

        val error = runCatching { source.fetch() }.exceptionOrNull()

        assertThat(error).isInstanceOf(ResponseException::class.java)
    }

    @Test
    fun testFetchHtmlInsteadOfXmlThrows() = runTest {
        body = CbrTestData.HTML_ERROR_PAGE.toByteArray()

        val error = runCatching { source.fetch() }.exceptionOrNull()

        assertThat(error).isNotNull()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "ru.fasdev.ratex.currency.data.source.cbr.CbrRateSourceTest"`
Expected: FAIL (unresolved reference `CbrRateSource`).

- [ ] **Step 3: Write minimal implementation**

```kotlin
package ru.fasdev.ratex.currency.data.source.cbr

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import java.nio.charset.Charset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fasdev.ratex.currency.data.source.CurrencyRateSource
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

/**
 * Официальные курсы ЦБ РФ: к RUB, около 55 валют, ответ в windows-1251. Ключ не нужен.
 * Курсы на следующий день публикуются вечером, поэтому в течение дня файл не меняется.
 */
class CbrRateSource(private val httpClient: HttpClient, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) :
    CurrencyRateSource {
    override val id: String = ID
    override val baseCode: String = CbrXmlParser.BASE_CODE
    override val refreshInterval: Duration = 4.hours

    override suspend fun fetch(): RateSnapshotDomain = withContext(ioDispatcher) {
        // Кодировку читаем из файла сами: charset в заголовке ответа ненадёжен
        val xml = String(httpClient.get(URL).bodyAsBytes(), Charset.forName("windows-1251"))
        CbrXmlParser.parse(xml)
    }

    companion object {
        const val ID = "cbr"
        const val URL = "https://www.cbr.ru/scripts/XML_daily.asp"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "ru.fasdev.ratex.currency.data.source.cbr.*"`
Expected: PASS (9 tests: 5 parser + 4 source).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/ru/fasdev/ratex/currency/data/source/cbr app/src/test/java/ru/fasdev/ratex/currency/data/source/cbr
git commit -m "feat: add CBR rate source" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: MergedCurrencyRateRepo и RateSourcePriority

**Files:**
- Create: `<M>/data/repo/MergedCurrencyRateRepo.kt`
- Create: `<M>/data/source/RateSourcePriority.kt`
- Test: `<T>/data/repo/MergedCurrencyRateRepoTest.kt`

**Interfaces:**
- Consumes: `CurrencyRateRepo.getSnapshot(): RateSnapshotDomain`; `EcbRateSource(httpClient)`, `CbrRateSource(httpClient)` (Task 2).
- Produces:
  - `class MergedCurrencyRateRepo(private val repos: List<CurrencyRateRepo>) : CurrencyRateRepo` — репозитории в порядке приоритета.
  - `enum class RateSourcePriority(val create: (HttpClient) -> CurrencyRateSource) { ECB, CBR }` — порядок констант = приоритет.

Алгоритм слияния (`merge(primary, other)`): `acc = primary.rates + (primary.baseCode to 1.0)`; `otherAll = other.rates + (other.baseCode to 1.0)`; мост — `"USD"`, если он есть в обоих, иначе первая общая валюта из `otherAll`; общей нет → `other` пропускается. `scale = acc[bridge] / otherAll[bridge]`; каждая валюта из `otherAll`, которой нет в `acc`, добавляется со значением `rate * scale`. Результат: `RateSnapshotDomain(primary.baseCode, maxOf(date), acc - primary.baseCode)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package ru.fasdev.ratex.currency.data.repo

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test
import ru.fasdev.ratex.currency.data.source.RateSourcePriority
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

class MergedCurrencyRateRepoTest {
    private class FakeRateRepo(var snapshot: RateSnapshotDomain? = null, var error: Exception? = null) : CurrencyRateRepo {
        override suspend fun getSnapshot(): RateSnapshotDomain {
            error?.let { throw it }
            return snapshot!!
        }
    }

    private val ecb = RateSnapshotDomain("EUR", "2026-10-05", mapOf("USD" to 1.10, "JPY" to 160.0, "CNY" to 7.8))

    // 1 RUB = 1/90 USD, но курс USD у ЕЦБ главнее; CNY у ЕЦБ тоже есть
    private val cbr = RateSnapshotDomain(
        "RUB",
        "2026-10-06",
        mapOf("USD" to 1 / 90.0, "CNY" to 1 / 12.0, "BGN" to 1 / 50.0, "KZT" to 100 / 18.9)
    )

    private val offset = Offset.offset(1e-9)

    @Test
    fun testBridgeAddsMissingCurrenciesInPrimaryScale() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(ecb), FakeRateRepo(cbr)))

        val result = repo.getSnapshot()

        assertThat(result.baseCode).isEqualTo("EUR")
        assertThat(result.availableCodes).contains("EUR", "USD", "JPY", "CNY", "RUB", "BGN", "KZT")
        // мост по USD: 1 EUR = 1.10 USD = 1.10 * 90 RUB
        assertThat(result.rates.getValue("RUB")).isCloseTo(1.10 * 90.0, offset)
        assertThat(result.rates.getValue("BGN")).isCloseTo(1.10 * 90.0 / 50.0, offset)
        assertThat(result.rates.getValue("KZT")).isCloseTo(1.10 * 90.0 * 100 / 18.9, offset)
    }

    @Test
    fun testPrimaryRatesAreNotOverwritten() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(ecb), FakeRateRepo(cbr)))

        val result = repo.getSnapshot()

        assertThat(result.rates.getValue("USD")).isEqualTo(1.10)
        assertThat(result.rates.getValue("CNY")).isEqualTo(7.8)
        assertThat(result.rates).doesNotContainKey("EUR")
    }

    @Test
    fun testDateIsTheLatest() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(ecb), FakeRateRepo(cbr)))

        assertThat(repo.getSnapshot().date).isEqualTo("2026-10-06")
    }

    @Test
    fun testOrderIsPriority() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(cbr), FakeRateRepo(ecb)))

        val result = repo.getSnapshot()

        assertThat(result.baseCode).isEqualTo("RUB")
        assertThat(result.rates.getValue("USD")).isEqualTo(1 / 90.0)
        assertThat(result.availableCodes).contains("EUR", "JPY")
        assertThat(result.rates.getValue("EUR")).isCloseTo(1 / 90.0 / 1.10, offset)
    }

    @Test
    fun testBridgeFallsBackToAnyCommonCurrencyWhenNoUsd() = runTest {
        val first = RateSnapshotDomain("EUR", "2026-10-05", mapOf("CNY" to 8.0))
        val second = RateSnapshotDomain("RUB", "2026-10-05", mapOf("CNY" to 1 / 10.0, "BGN" to 1 / 50.0))
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(first), FakeRateRepo(second)))

        val result = repo.getSnapshot()

        // 1 EUR = 8 CNY = 80 RUB
        assertThat(result.rates.getValue("RUB")).isCloseTo(80.0, offset)
        assertThat(result.rates.getValue("BGN")).isCloseTo(80.0 / 50.0, offset)
    }

    @Test
    fun testSnapshotWithoutCommonCurrencyIsSkipped() = runTest {
        val isolated = RateSnapshotDomain("RUB", "2026-10-06", mapOf("BGN" to 1 / 50.0))
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(ecb), FakeRateRepo(isolated)))

        val result = repo.getSnapshot()

        assertThat(result).isEqualTo(ecb)
    }

    @Test
    fun testOneSourceFailsUsesTheOther() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(error = java.io.IOException("offline")), FakeRateRepo(cbr)))

        val result = repo.getSnapshot()

        assertThat(result).isEqualTo(cbr)
    }

    @Test
    fun testPrimaryFailsMergeKeepsSecondaryScale() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(error = java.io.IOException("offline")), FakeRateRepo(cbr)))

        assertThat(repo.getSnapshot().baseCode).isEqualTo("RUB")
    }

    @Test
    fun testAllSourcesFailThrowsFirstError() = runTest {
        val first = java.io.IOException("first")
        val repo = MergedCurrencyRateRepo(
            listOf(FakeRateRepo(error = first), FakeRateRepo(error = java.io.IOException("second")))
        )

        val error = runCatching { repo.getSnapshot() }.exceptionOrNull()

        assertThat(error).isSameAs(first)
    }

    @Test
    fun testCancellationIsRethrown() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(ecb), FakeRateRepo(error = CancellationException("cancelled"))))

        val error = runCatching { repo.getSnapshot() }.exceptionOrNull()

        assertThat(error).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun testPriorityEnumOrderIsEcbThenCbr() {
        assertThat(RateSourcePriority.entries).containsExactly(RateSourcePriority.ECB, RateSourcePriority.CBR)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "ru.fasdev.ratex.currency.data.repo.MergedCurrencyRateRepoTest"`
Expected: FAIL (unresolved references `MergedCurrencyRateRepo`, `RateSourcePriority`).

- [ ] **Step 3: Write minimal implementation**

`RateSourcePriority.kt`:

```kotlin
package ru.fasdev.ratex.currency.data.source

import io.ktor.client.HttpClient
import ru.fasdev.ratex.currency.data.source.cbr.CbrRateSource
import ru.fasdev.ratex.currency.data.source.ecb.EcbRateSource

/**
 * Источники курсов в порядке приоритета: курс валюты, которая есть у нескольких источников, берётся у того, что выше.
 * Чтобы поменять приоритет, переставьте константы.
 */
enum class RateSourcePriority(val create: (HttpClient) -> CurrencyRateSource) {
    ECB({ EcbRateSource(it) }),
    CBR({ CbrRateSource(it) })
}
```

`MergedCurrencyRateRepo.kt`:

```kotlin
package ru.fasdev.ratex.currency.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

/**
 * Один снимок из нескольких источников. [repos] — по убыванию приоритета: шкалой (базой) снимка становится база первого
 * успешного источника, курсы остальных пересчитываются в неё через общую валюту (мост). Валюта, которая уже есть у более
 * приоритетного источника, не перезаписывается. Источник без общей валюты пропускается, упавший — тоже, если жив хоть один.
 */
class MergedCurrencyRateRepo(private val repos: List<CurrencyRateRepo>) : CurrencyRateRepo {
    override suspend fun getSnapshot(): RateSnapshotDomain {
        val results = coroutineScope {
            repos.map { repo ->
                async {
                    try {
                        Result.success(repo.getSnapshot())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            }.awaitAll()
        }

        val snapshots = results.mapNotNull { it.getOrNull() }
        if (snapshots.isEmpty()) {
            throw results.firstNotNullOfOrNull { it.exceptionOrNull() } ?: IllegalStateException("No rate sources")
        }

        return snapshots.reduce(::merge)
    }

    private fun merge(primary: RateSnapshotDomain, other: RateSnapshotDomain): RateSnapshotDomain {
        val merged = primary.rates + (primary.baseCode to 1.0)
        val otherAll = other.rates + (other.baseCode to 1.0)

        val bridge = if (BRIDGE_CODE in merged && BRIDGE_CODE in otherAll) {
            BRIDGE_CODE
        } else {
            otherAll.keys.firstOrNull { it in merged }
        } ?: return primary

        val scale = merged.getValue(bridge) / otherAll.getValue(bridge)

        val result = LinkedHashMap(merged)
        otherAll.forEach { (code, rate) ->
            if (code !in result) result[code] = rate * scale
        }
        result.remove(primary.baseCode)

        return RateSnapshotDomain(primary.baseCode, maxOf(primary.date, other.date), result)
    }

    private companion object {
        const val BRIDGE_CODE = "USD"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "ru.fasdev.ratex.currency.data.repo.MergedCurrencyRateRepoTest"`
Expected: PASS (11 tests). Если `testSnapshotWithoutCommonCurrencyIsSkipped` падает на `isEqualTo(ecb)` — `merge` должен вернуть `primary` без изменений (так и написано).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/ru/fasdev/ratex/currency/data app/src/test/java/ru/fasdev/ratex/currency/data/repo
git commit -m "feat: merge rate sources through a common currency bridge" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: DI, откат базы и документация

**Files:**
- Modify: `<M>/di/module/CurrencyModule.kt` (убрать `provideCurrencyRateSource`, переписать `provideCurrencyRateRepo`, убрать неиспользуемые импорты `CurrencyRateSource`, `EcbRateSource`)
- Modify: `<T>/data/repo/CurrencyBaseRepoTest.kt` (добавить тест)
- Create: `docs-ai/artifact/2026-10-06-source-cbr.md`

**Interfaces:**
- Consumes: `RateSourcePriority.entries`, `RateSourcePriority.create(HttpClient)`, `CurrencyRateRepoImpl(source, storage)`, `MergedCurrencyRateRepo(repos)` (Task 3).
- Produces: `CurrencyRateRepo`, собранный из двух источников.

- [ ] **Step 1: Write the failing test**

Добавить в `CurrencyBaseRepoTest` (после `testGetBaseCurrencyLocaleMissingInSourceFallsBackToSourceBase`):

```kotlin
    @Test
    fun testGetBaseCurrencyRussianLocaleWithMergedSnapshotIsRub() = runTest {
        Mockito.`when`(currencyRateRepo.getSnapshot())
            .thenReturn(RateSnapshotDomain("EUR", "2026-10-05", mapOf("USD" to 1.1, "RUB" to 99.0)))
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(null)
        Locale.setDefault(Locale("ru", "RU"))

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo("RUB")
    }
```

- [ ] **Step 2: Run test to verify it passes already (логика не меняется)**

Run: `./gradlew :app:testDebugUnitTest --tests "*CurrencyBaseRepoTest"`
Expected: PASS. Тест фиксирует поведение; он не должен падать, потому что `getBaseCurrency()` не меняется. Если упал — это находка, остановиться и разобраться (systematic-debugging), а не править тест.

- [ ] **Step 3: Подключить источники в DI**

В `CurrencyModule.kt` удалить метод `provideCurrencyRateSource`, импорты `CurrencyRateSource`, `EcbRateSource`, и заменить `provideCurrencyRateRepo`:

```kotlin
    @Provides
    @CurrencyScope
    fun provideCurrencyRateRepo(httpClient: HttpClient, storage: RateSnapshotStorage): CurrencyRateRepo = MergedCurrencyRateRepo(
        RateSourcePriority.entries.map { CurrencyRateRepoImpl(it.create(httpClient), storage) }
    )
```

Добавить импорты `ru.fasdev.ratex.currency.data.repo.MergedCurrencyRateRepo` и `ru.fasdev.ratex.currency.data.source.RateSourcePriority`.

- [ ] **Step 4: Полная проверка**

Run: `./gradlew test assembleDebug ktlintCheck`
Expected: BUILD SUCCESSFUL, все unit-тесты зелёные. Если ktlint ругается — `./gradlew ktlintFormat` и повторить.

- [ ] **Step 5: Артефакт docs-ai**

Создать `docs-ai/artifact/2026-10-06-source-cbr.md` в стиле `2026-10-06-source-data.md`, разделы: Проблема, Что сделано (`CbrRateSource`/`CbrXmlParser`, `MergedCurrencyRateRepo`, `RateSourcePriority`, DI), Решения (один слитый снимок, мост по USD, приоритет ЕЦБ > ЦБ РФ, шкала = база первого успешного источника, откат базы не менялся), Проверка (реальные команды и числа тестов из шага 4), Отложено (источник США, TTL по расписанию, проверка TLS `cbr.ru` на эмуляторе API 34 и русской локали с RUB вручную; разница дат между снимками). Обновить в `2026-10-06-source-data.md` таблицу источников и раздел «мост не реализован» ссылкой на новый артефакт.

- [ ] **Step 6: Commit**

```bash
git add app/src docs-ai
git commit -m "feat: wire ECB and CBR sources through merged repo" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```
