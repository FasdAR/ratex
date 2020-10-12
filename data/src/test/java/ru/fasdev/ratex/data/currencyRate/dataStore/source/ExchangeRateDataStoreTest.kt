package ru.fasdev.ratex.data.currencyRate.dataStore.source

import android.util.Log
import io.reactivex.observers.TestObserver
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import ru.fasdev.ratex.data.TestData
import ru.fasdev.ratex.data.source.retrofit.exchangeRates.ExchangeRateApi
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain


class ExchangeRateDataStoreTest
{
    private lateinit var mockWebServer: MockWebServer

    private lateinit var exchangeRateApi: ExchangeRateApi
    private lateinit var imageRepo: CurrencyImageRepo

    private lateinit var exchangeRateDataStore: ExchangeRateDataStore

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        mockWebServer.dispatcher = object : QueueDispatcher() {
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
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(OkHttpClient())
            .build()
            .create(ExchangeRateApi::class.java)

        imageRepo = Mockito.mock(CurrencyImageRepo::class.java)

        exchangeRateDataStore = ExchangeRateDataStore(exchangeRateApi, imageRepo)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testGetExchangeRates() {
        val testData = CurrencyDomain.getInstance("USD")

        val testObserver: TestObserver<List<RateCurrencyDomain>> = TestObserver()

        exchangeRateDataStore.getExchangeRates(testData).subscribe(testObserver)

        testObserver
            .assertComplete()
            .assertValue{ it.size == 32 }
            .assertValue{ it.isNotEmpty() }
    }
}