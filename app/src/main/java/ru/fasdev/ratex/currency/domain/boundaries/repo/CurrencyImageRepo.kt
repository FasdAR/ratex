package ru.fasdev.ratex.currency.domain.boundaries.repo

interface CurrencyImageRepo {
    fun getImageUrl(currencyCode: String): String?
}
