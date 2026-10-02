package ru.fasdev.ratex.domain.currency.boundaries.interactor

import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain

interface CurrencyRateInteractor {
    suspend fun getExchangeRates(): List<RateCurrencyDomain>
}
