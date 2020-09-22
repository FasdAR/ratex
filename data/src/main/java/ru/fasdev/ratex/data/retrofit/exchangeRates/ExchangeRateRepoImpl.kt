package ru.fasdev.ratex.data.retrofit.exchangeRates

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.reactivex.Single
import retrofit2.Retrofit
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyImageProvider
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateRepo
import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain

class ExchangeRateRepoImpl(val exchangeRateApi: ExchangeRateApi, val currencyImageProvider: CurrencyImageProvider): CurrencyRateRepo
{
    override fun getExchangeRates(baseCurrencyCode: String): Single<List<RateCurrencyDomain>>
    {
        return exchangeRateApi
            .getProducts(baseCurrencyCode)
            .map { it -> it.string() }
            .map { it -> JsonParser().parse(it) }
            .map { it -> it.asJsonObject.getAsJsonObject("rates") }
            .flatMap {
                Single.just(parseRatesObjToList(it.entrySet()))
            }
    }

    private fun parseRatesObjToList(setRates: Set<Map.Entry<String, JsonElement>>): MutableList<RateCurrencyDomain>
    {
        val rateList: MutableList<RateCurrencyDomain> = arrayListOf()

        setRates.forEach {
            rateList.add(RateCurrencyDomain(CurrencyDomain.getInstance(it.key, currencyImageProvider), it.value.asDouble))
        }

        return rateList
    }
}