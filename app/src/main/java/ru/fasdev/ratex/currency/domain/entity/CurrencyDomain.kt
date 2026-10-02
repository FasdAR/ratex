package ru.fasdev.ratex.currency.domain.entity

import java.util.*
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.entity.extension.toCurrencyDomain

class CurrencyDomain(val currencyCode: String, val symbol: String, displayName: String, val urlImage: String?) {
    val displayName: String

    init {
        this.displayName = displayName.substring(0, 1).uppercase() + displayName.substring(1)
    }

    companion object {
        fun getInstance(currencyCode: String): CurrencyDomain = Currency.getInstance(currencyCode).toCurrencyDomain()

        fun getInstance(currencyCode: String, imageProvider: CurrencyImageRepo): CurrencyDomain = Currency
            .getInstance(currencyCode)
            .toCurrencyDomain(
                imageProvider.getImageUrl(currencyCode)
            )
    }

    override fun toString(): String = "$currencyCode, $symbol, $displayName, $urlImage"

    override fun hashCode(): Int = currencyCode.length + symbol.length + displayName.length + (urlImage?.length ?: 0)

    override fun equals(other: Any?): Boolean = other.hashCode() == hashCode()
}
