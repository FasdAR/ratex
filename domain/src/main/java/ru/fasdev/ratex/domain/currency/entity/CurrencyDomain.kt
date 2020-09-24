package ru.fasdev.ratex.domain.currency.entity

import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.domain.currency.entity.extension.toCurrencyDomain
import java.util.*

class CurrencyDomain(val currencyCode: String, val symbol: String, displayName: String, val urlImage: String?)
{
    val displayName: String

    init {
        this.displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1)
    }

    companion object {
        fun getInstance(currencyCode: String): CurrencyDomain = Currency.getInstance(currencyCode).toCurrencyDomain()

        fun getInstance(currencyCode: String, imageProvider: CurrencyImageRepo): CurrencyDomain
        {
            return Currency
                .getInstance(currencyCode)
                .toCurrencyDomain(
                    imageProvider.getImageUrl(currencyCode)
                )
        }
    }

    override fun toString(): String {
        return "${currencyCode}, ${symbol}, ${displayName}, ${urlImage}"
    }
}