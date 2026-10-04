package ru.fasdev.ratex.currency.data.dataStore.source

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import ru.fasdev.ratex.currency.data.TestData
import ru.fasdev.ratex.currency.data.api.ExchangeRateApi
import ru.fasdev.ratex.currency.data.api.ExchangeRateApiImpl
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

class ExchangeRateDataStoreTest {
    @get:Rule val mockitoJunit = MockitoJUnit.rule()

    @Mock private lateinit var imageRepo: CurrencyImageRepo

    private var usdStatus: HttpStatusCode = HttpStatusCode.OK
    private var usdBody: String = TestData.JSON_EXCHANGE_RATES

    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var httpClient: HttpClient

    private lateinit var exchangeRateApi: ExchangeRateApi

    private lateinit var exchangeRateDataStore: ExchangeRateDataStore

    @Before
    fun setUp() {
        val engine = MockEngine { request ->
            val url = request.url
            if (url.encodedPath == "/latest" && url.parameters["base"] == "USD") {
                respond(usdBody, usdStatus, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond("", HttpStatusCode.NotFound)
            }
        }

        httpClient = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(json) }
            defaultRequest { url("https://api.test/") }
        }

        exchangeRateApi = ExchangeRateApiImpl(httpClient)

        exchangeRateDataStore = ExchangeRateDataStore(exchangeRateApi, imageRepo, UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        httpClient.close()
    }

    @Test
    fun testGetExchangeRates() = runTest {
        val testData = CurrencyDomain.getInstance("USD")

        val result = exchangeRateDataStore.getExchangeRates(testData)

        assertThat(result).hasSize(32)
    }

    @Test
    fun testGetExchangeRatesExcludesBaseCurrency() = runTest {
        val result = exchangeRateDataStore.getExchangeRates(CurrencyDomain.getInstance("USD"))

        assertThat(result.map { it.currency.currencyCode }).doesNotContain("USD")
    }

    @Test
    fun testGetExchangeRatesIgnoresUnknownFields() = runTest {
        usdBody = TestData.JSON_EXCHANGE_RATES_EXTRA_FIELDS

        val result = exchangeRateDataStore.getExchangeRates(CurrencyDomain.getInstance("USD"))

        assertThat(result.map { it.currency.currencyCode to it.rate })
            .containsExactly("CAD" to 0.0170693549, "HKD" to 0.100746984)
    }

    @Test
    fun testGetExchangeRatesInvalidJson() = runTest {
        usdBody = TestData.JSON_INVALID

        val error = runCatching { exchangeRateDataStore.getExchangeRates(CurrencyDomain.getInstance("USD")) }.exceptionOrNull()

        assertThat(error).isInstanceOf(JsonConvertException::class.java)
    }

    @Test
    fun testGetExchangeRatesHttpError() = runTest {
        usdStatus = HttpStatusCode.InternalServerError
        usdBody = "{\"error\":\"server\"}"

        val error = runCatching { exchangeRateDataStore.getExchangeRates(CurrencyDomain.getInstance("USD")) }.exceptionOrNull()

        assertThat(error).isInstanceOf(ResponseException::class.java)
    }
}
