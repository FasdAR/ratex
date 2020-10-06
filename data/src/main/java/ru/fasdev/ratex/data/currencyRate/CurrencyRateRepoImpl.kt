package ru.fasdev.ratex.data.currencyRate

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.reactivex.Observable
import io.reactivex.Single
import ru.fasdev.ratex.data.retrofit.exchangeRates.ExchangeRateApi
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain

class CurrencyRateRepoImpl (val exchangeRateApi: ExchangeRateApi, val currencyBaseRepo: CurrencyBaseRepo, val currencyImageRepo: CurrencyImageRepo):
    CurrencyRateRepo
{
    override fun getExchangeRates(): Single<List<RateCurrencyDomain>>
    {
        return currencyBaseRepo.getBaseCurrency()
            .flatMap { baseCurrency ->
                exchangeRateApi
                    .getProducts(baseCurrency.currencyCode)
                    .map { it -> it.string() }
                    .map { it -> JsonParser().parse(it) }
                    .map { it -> it.asJsonObject.getAsJsonObject("rates") }
                    .flatMapObservable {
                        Observable.fromIterable(it.entrySet())
                    }
                    .map {
                        RateCurrencyDomain(CurrencyDomain.getInstance(it.key, currencyImageRepo), it.value.asDouble)
                    }
                    .filter {
                        it.currency.currencyCode != baseCurrency.currencyCode
                    }
                    .toList()
            }
    }
}