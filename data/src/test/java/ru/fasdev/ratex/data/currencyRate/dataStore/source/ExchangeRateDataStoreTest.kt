package ru.fasdev.ratex.data.currencyRate.dataStore.source

import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.fasdev.ratex.data.TestData
import ru.fasdev.ratex.data.source.retrofit.exchangeRates.ExchangeRateApi
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain

class ExchangeRateDataStoreTest {
    @get:Rule val mockitoJunit = MockitoJUnit.rule()

    @Mock private lateinit var imageRepo: CurrencyImageRepo

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
                    "/latest?base=USD" -> return MockResponse().setResponseCode(200)
                        .setBody(TestData.JSON_EXCHANGE_RATES)
                    else -> return MockResponse().setResponseCode(404)
                }
            }
        }

        exchangeRateApi = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
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
}
