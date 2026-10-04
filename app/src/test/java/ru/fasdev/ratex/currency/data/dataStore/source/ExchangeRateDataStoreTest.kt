package ru.fasdev.ratex.currency.data.dataStore.source

import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import ru.fasdev.ratex.currency.data.TestData
import ru.fasdev.ratex.currency.data.api.ExchangeRateApi
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

class ExchangeRateDataStoreTest {
    @get:Rule val mockitoJunit = MockitoJUnit.rule()

    @Mock private lateinit var imageRepo: CurrencyImageRepo

    private var usdResponse: MockResponse = MockResponse().setResponseCode(200).setBody(TestData.JSON_EXCHANGE_RATES)

    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var mockWebServer: MockWebServer

    private lateinit var exchangeRateApi: ExchangeRateApi

    private lateinit var exchangeRateDataStore: ExchangeRateDataStore

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                when (request.path) {
                    "/latest?base=USD" -> return usdResponse
                    else -> return MockResponse().setResponseCode(404)
                }
            }
        }

        exchangeRateApi = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(OkHttpClient())
            .build()
            .create(ExchangeRateApi::class.java)

        exchangeRateDataStore = ExchangeRateDataStore(exchangeRateApi, imageRepo, UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
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
        usdResponse = MockResponse().setResponseCode(200).setBody(TestData.JSON_EXCHANGE_RATES_EXTRA_FIELDS)

        val result = exchangeRateDataStore.getExchangeRates(CurrencyDomain.getInstance("USD"))

        assertThat(result.map { it.currency.currencyCode to it.rate })
            .containsExactly("CAD" to 0.0170693549, "HKD" to 0.100746984)
    }

    @Test
    fun testGetExchangeRatesInvalidJson() = runTest {
        usdResponse = MockResponse().setResponseCode(200).setBody(TestData.JSON_INVALID)

        val error = runCatching { exchangeRateDataStore.getExchangeRates(CurrencyDomain.getInstance("USD")) }.exceptionOrNull()

        assertThat(error).isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun testGetExchangeRatesHttpError() = runTest {
        usdResponse = MockResponse().setResponseCode(500).setBody("{\"error\":\"server\"}")

        val error = runCatching { exchangeRateDataStore.getExchangeRates(CurrencyDomain.getInstance("USD")) }.exceptionOrNull()

        assertThat(error).isInstanceOf(HttpException::class.java)
    }
}
