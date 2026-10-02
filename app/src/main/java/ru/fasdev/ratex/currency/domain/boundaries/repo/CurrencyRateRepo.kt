package ru.fasdev.ratex.currency.domain.boundaries.repo

import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain

interface CurrencyRateRepo {
    suspend fun getExchangeRates(): List<RateCurrencyDomain>
}
