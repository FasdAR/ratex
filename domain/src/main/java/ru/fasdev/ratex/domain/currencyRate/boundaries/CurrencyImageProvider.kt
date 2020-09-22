package ru.fasdev.ratex.domain.currencyRate.boundaries

import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain

interface CurrencyImageProvider
{
    fun getImageUrl(currencyCode: String): String?
}