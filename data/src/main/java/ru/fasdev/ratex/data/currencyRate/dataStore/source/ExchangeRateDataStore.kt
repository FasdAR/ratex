package ru.fasdev.ratex.data.currencyRate.dataStore.source

import com.google.gson.JsonParser
import io.reactivex.Observable
import io.reactivex.Single
import ru.fasdev.ratex.data.currencyRate.dataStore.CurrencyRateDataStore
import ru.fasdev.ratex.data.source.retrofit.exchangeRates.ExchangeRateApi
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain

class ExchangeRateDataStore (val exchangeRateApi: ExchangeRateApi, val imageRepo: CurrencyImageRepo): CurrencyRateDataStore
{
    override fun getExchangeRates(baseCurrency: CurrencyDomain): Single<List<RateCurrencyDomain>>
    {
        return exchangeRateApi
            .getProducts(baseCurrency.currencyCode)
            .map { it -> it.string() }
            .map { it -> JsonParser().parse(it) }
            .map { it -> it.asJsonObject.getAsJsonObject("rates") }
            .flatMapObservable {
                Observable.fromIterable(it.entrySet())
            }
            .map {
                RateCurrencyDomain(CurrencyDomain.getInstance(it.key, imageRepo), it.value.asDouble)
            }
            .filter {
                it.currency.currencyCode != baseCurrency.currencyCode
            }
            .toList()
    }
}