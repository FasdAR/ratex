package ru.fasdev.ratex.domain.currency.boundaries.repo

import io.reactivex.Single
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain

interface CurrencyRateRepo
{
    fun getExchangeRates(): Single<List<RateCurrencyDomain>>
}