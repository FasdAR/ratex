package ru.fasdev.ratex.data.currencyRate.repo

import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo

class FlagCdnRepoImpl : CurrencyImageRepo
{
    override fun getImageUrl(currencyCode: String): String
    {
        val url = "https://flagcdn.com/w160/${currencyCode.substring(0,2).toLowerCase()}.jpg"
        return url
    }
}