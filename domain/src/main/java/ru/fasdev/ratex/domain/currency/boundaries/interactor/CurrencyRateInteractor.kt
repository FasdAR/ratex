package ru.fasdev.ratex.domain.currency.boundaries.interactor

import io.reactivex.Single
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain

interface CurrencyRateInteractor
{
    fun getExchangeRates(): Single<List<RateCurrencyDomain>>
}