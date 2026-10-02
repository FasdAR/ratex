package ru.fasdev.ratex.currency.domain.boundaries.interactor

import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain

interface CurrencyRateInteractor {
    suspend fun getExchangeRates(): List<RateCurrencyDomain>
}
