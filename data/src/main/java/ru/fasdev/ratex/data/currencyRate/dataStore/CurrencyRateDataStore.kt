package ru.fasdev.ratex.data.currencyRate.dataStore

import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain

interface CurrencyRateDataStore {
    suspend fun getExchangeRates(baseCurrency: CurrencyDomain): List<RateCurrencyDomain>
}
