package ru.fasdev.ratex.currency.data.dataStore

import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain

interface CurrencyRateDataStore {
    suspend fun getExchangeRates(baseCurrency: CurrencyDomain): List<RateCurrencyDomain>
}
