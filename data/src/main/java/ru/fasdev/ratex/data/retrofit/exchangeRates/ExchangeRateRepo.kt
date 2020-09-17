package ru.fasdev.ratex.data.retrofit.exchangeRates

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import retrofit2.Retrofit
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateRepo
import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain

class ExchangeRateRepo(val retrofit: Retrofit, val exchangeRateApi: ExchangeRateApi): CurrencyRateRepo
{
    override fun getExchangeRates(baseCurrencyCode: String): List<RateCurrencyDomain>
    {
        val response = exchangeRateApi.getProducts(baseCurrencyCode).execute()
        val body = response.body()?.string()

        val rateList: MutableList<RateCurrencyDomain> = arrayListOf()

        //Parse Json To Array
        body?.let {
            val jsonElement: JsonElement = JsonParser().parse(it)
            val rates = jsonElement.asJsonObject.getAsJsonObject("rates");

            rateList.addAll(parseRatesObjToList(rates.entrySet()))
        }

        return rateList
    }

    private fun parseRatesObjToList(setRates: Set<Map.Entry<String, JsonElement>>): MutableList<RateCurrencyDomain>
    {
        val rateList: MutableList<RateCurrencyDomain> = arrayListOf()

        setRates.forEach {
            rateList.add(RateCurrencyDomain(CurrencyDomain.getInstance(it.key), it.value.asDouble))
        }

        return rateList
    }
}