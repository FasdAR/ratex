package ru.fasdev.ratex.domain.currencyRate.boundaries

import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain
import java.util.*

interface CurrencyRateInteractor
{
    fun getBaseCurrency(): CurrencyDomain
    fun setBaseCurrency(baseCurrency: CurrencyDomain)
    fun getExchangeRates(): List<RateCurrencyDomain>
}