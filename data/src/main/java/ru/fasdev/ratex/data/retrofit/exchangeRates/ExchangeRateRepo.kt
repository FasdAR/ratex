package ru.fasdev.ratex.data.retrofit.exchangeRates

import retrofit2.Retrofit
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateRepo
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain

class ExchangeRateRepo (val retrofit: Retrofit, val exchangeRateApi: ExchangeRateApi): CurrencyRateRepo
{
    override fun getExchangeRates(baseCurrencyCode: String): List<RateCurrencyDomain>
    {
        val response = exchangeRateApi.getProducts(baseCurrencyCode).execute()
        val body = response.body()



        //TODO: CHANGE TO NORMAL LIST
        return emptyList()
    }
}