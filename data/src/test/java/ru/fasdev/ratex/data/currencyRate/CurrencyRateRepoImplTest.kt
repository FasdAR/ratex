package ru.fasdev.ratex.data.currencyRate

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
import ru.fasdev.ratex.data.retrofit.exchangeRates.ExchangeRateApi
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain
import java.util.concurrent.TimeUnit


class CurrencyRateRepoImplTest
{
    private lateinit var currencyBaseRepo: CurrencyBaseRepo

    @Before
    fun setUp()
    {
        currencyBaseRepo = Mockito.mock(CurrencyBaseRepo::class.java)
        Mockito.`when`(currencyBaseRepo.getBaseCurrency()).thenReturn(Single.just(CurrencyDomain.getInstance("USD")))
    }

    @Test
    fun testGetRealData()
    {
        var exchangeRateRepo: CurrencyRateRepo

        //#region Init Retrofit
        val httpLoggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val okHttpClient: OkHttpClient = OkHttpClient.Builder().addInterceptor(
            httpLoggingInterceptor
        ).build()

        val retrofit = Retrofit
            .Builder()
            .baseUrl("https://api.exchangeratesapi.io")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .client(okHttpClient)
            .build()

        exchangeRateRepo = CurrencyRateRepoImpl(retrofit.create(ExchangeRateApi::class.java), currencyBaseRepo, FlagCdnRepoImpl())
        //#endregion

        val testObserver: TestObserver<List<RateCurrencyDomain>> = TestObserver()

        exchangeRateRepo.getExchangeRates()
            .subscribe(testObserver)

        testObserver
            .awaitDone(60, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue { it -> !it.isNullOrEmpty() }
    }
}