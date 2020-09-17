package ru.fasdev.ratex.data.retrofit.exchangeRates

import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import junit.framework.Assert.assertNotNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateRepo
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain
import java.util.concurrent.TimeUnit

class ExchangeRateRepoTest
{
    lateinit var retrofit: Retrofit
    lateinit var exchangeRateRepo: CurrencyRateRepo

    @Before
    fun setUp()
    {
        val httpLoggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val okHttpClient: OkHttpClient = OkHttpClient.Builder().addInterceptor(httpLoggingInterceptor).build()

        retrofit = Retrofit
            .Builder()
            .baseUrl("https://api.exchangeratesapi.io")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .client(okHttpClient)
            .build()

        exchangeRateRepo = ExchangeRateRepo(retrofit, retrofit.create(ExchangeRateApi::class.java))
    }

    //Тестирование соединения и получения реальных данных
    @Test
    fun getRealData()
    {
        val testObserver: TestObserver<List<RateCurrencyDomain>> = TestObserver()

        exchangeRateRepo.getExchangeRates("USD")
            .subscribe(testObserver)

        testObserver
            .awaitDone(60, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue { it -> !it.isNullOrEmpty() }
    }
}