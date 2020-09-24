package ru.fasdev.ratex.data.currencyRate

import org.junit.Before


class CurrencyRateRepoImplTest
{
    @Before
    fun setUp()
    {

    }
}

/*
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
 */