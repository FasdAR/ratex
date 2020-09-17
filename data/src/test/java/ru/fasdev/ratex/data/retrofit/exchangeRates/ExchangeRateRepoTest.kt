package ru.fasdev.ratex.data.retrofit.exchangeRates

import junit.framework.Assert.assertNotNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateRepo

@RunWith(RobolectricTestRunner::class)
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
            .client(okHttpClient)
            .build()

        exchangeRateRepo = ExchangeRateRepo(retrofit, retrofit.create(ExchangeRateApi::class.java))
    }

    @Test
    fun getRealData()
    {
        val result = exchangeRateRepo.getExchangeRates("USD")
        assertNotNull(result)
    }
}