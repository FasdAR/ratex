package ru.fasdev.ratex.currencyRate

import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyImageProvider

class FlagCdnProviderImpl : CurrencyImageProvider
{
    override fun getImageUrl(currencyCode: String): String
    {
        val url = "https://flagcdn.com/w160/${currencyCode.substring(0,2).toLowerCase()}.jpg"
        return url
    }
}