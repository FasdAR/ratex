package ru.fasdev.ratex.domain.currencyRate.boundaries

import io.reactivex.Single
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain

interface CurrencyRateRepo
{
    fun getExchangeRates(baseCurrencyCode: String): Single<List<RateCurrencyDomain>>
}