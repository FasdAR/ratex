package ru.fasdev.ratex.domain.currency.boundaries

import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain
import java.util.*

interface CurrencyInteractor {
    fun getDomainCurrencyByCode(code: String): CurrencyDomain
    fun mapCurrencyToCurrencyDomain(currency: Currency): CurrencyDomain
}