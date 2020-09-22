package ru.fasdev.ratex.domain.currencyRate.entity

import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyImageProvider
import ru.fasdev.ratex.domain.currencyRate.entity.extension.toCurrencyDomain
import java.util.*

data class CurrencyDomain(val currencyCode: String, val symbol: String, val displayName: String, val urlImage: String?)
{
    companion object {
        fun getInstance(currencyCode: String): CurrencyDomain = Currency.getInstance(currencyCode).toCurrencyDomain()

        fun getInstance(currencyCode: String, imageProvider: CurrencyImageProvider): CurrencyDomain
        {
            return Currency
                .getInstance(currencyCode)
                .toCurrencyDomain(
                    imageProvider.getImageUrl(currencyCode)
                )
        }
    }
}