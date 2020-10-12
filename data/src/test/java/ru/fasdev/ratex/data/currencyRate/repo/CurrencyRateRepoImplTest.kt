package ru.fasdev.ratex.data.currencyRate.repo

import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import ru.fasdev.ratex.data.currencyRate.dataStore.CurrencyRateDataStore
import ru.fasdev.ratex.data.source.retrofit.exchangeRates.ExchangeRateApi
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain
import java.util.concurrent.TimeUnit


class CurrencyRateRepoImplTest
{
    private lateinit var currencyBaseRepo: CurrencyBaseRepo
    private lateinit var currencyRateDataStore: CurrencyRateDataStore
    private lateinit var currencyRateRepo: CurrencyRateRepo

    @Before fun setUp()
    {
        currencyBaseRepo = Mockito.mock(CurrencyBaseRepo::class.java)
        currencyRateDataStore = Mockito.mock(CurrencyRateDataStore::class.java)

        currencyRateRepo = CurrencyRateRepoImpl(currencyRateDataStore, currencyBaseRepo)
    }

    @Test
    fun testGetExchangeRates()
    {
        val testCurrencyDomain = CurrencyDomain.getInstance("RUB")

        val testListData = listOf(
            RateCurrencyDomain(CurrencyDomain.getInstance("USD"), 73.0),
            RateCurrencyDomain(CurrencyDomain.getInstance("EUR"), 90.5)
        )

        Mockito.`when`(currencyBaseRepo.getBaseCurrency())
            .thenReturn(Single.just(testCurrencyDomain))

        Mockito
            .`when`(currencyRateDataStore.getExchangeRates(testCurrencyDomain))
            .thenReturn(
            Single.just(
                testListData
            )
        )

        val testObserver: TestObserver<List<RateCurrencyDomain>> = TestObserver()

        currencyRateRepo.getExchangeRates().subscribe(testObserver)

        testObserver
            .assertComplete()
            .assertValue(testListData)
    }
}