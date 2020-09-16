package ru.fasdev.ratex.domain.currencyRate.boundaries

import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain

interface CurrencyRateRepo
{
    fun getExchangeRates(baseCurrencyCode: String): List<RateCurrencyDomain>
}