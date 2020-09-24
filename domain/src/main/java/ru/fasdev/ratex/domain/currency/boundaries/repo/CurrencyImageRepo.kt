package ru.fasdev.ratex.domain.currency.boundaries.repo

interface CurrencyImageRepo
{
    fun getImageUrl(currencyCode: String): String?
}