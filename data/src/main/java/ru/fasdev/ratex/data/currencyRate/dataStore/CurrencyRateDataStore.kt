package ru.fasdev.ratex.data.currencyRate.dataStore

import io.reactivex.Single
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain

interface CurrencyRateDataStore
{
    fun getExchangeRates(baseCurrency: CurrencyDomain): Single<List<RateCurrencyDomain>>
}