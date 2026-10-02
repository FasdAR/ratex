package ru.fasdev.ratex.domain.currency.boundaries.repo

import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain

interface CurrencyRateRepo {
    suspend fun getExchangeRates(): List<RateCurrencyDomain>
}
