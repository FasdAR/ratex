package ru.fasdev.ratex.domain.currencyRate.entity

import ru.fasdev.ratex.domain.currencyRate.entity.extension.toCurrencyDomain
import java.util.*

data class CurrencyDomain(val currencyCode: String, val symbol: String, val displayName: String)
{
    companion object {
        fun getInstance(currencyCode: String): CurrencyDomain = Currency.getInstance(currencyCode).toCurrencyDomain()
    }
}