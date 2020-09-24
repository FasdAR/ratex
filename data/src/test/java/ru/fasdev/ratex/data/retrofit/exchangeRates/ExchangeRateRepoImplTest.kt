package ru.fasdev.ratex.data.retrofit.exchangeRates

import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import ru.fasdev.ratex.currencyRate.FlagCdnRepoImpl
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain
import java.util.concurrent.TimeUnit


class ExchangeRateRepoImplTest
{
    @Test
    fun testRealData()
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

        exchangeRateRepo = ExchangeRateRepoImpl(retrofit.create(ExchangeRateApi::class.java), FlagCdnRepoImpl())
        //#endregion

        val testObserver: TestObserver<List<RateCurrencyDomain>> = TestObserver()

        exchangeRateRepo.getExchangeRates("USD")
            .subscribe(testObserver)

        testObserver
            .awaitDone(60, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue { it -> !it.isNullOrEmpty() }
    }

    @Test
    fun testFakeData()
    {
        //TODO: IMPL TEST FOR FAKE DATA
    }
}