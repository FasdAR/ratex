package ru.fasdev.ratex.currency.data.repo

import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo

class FlagCdnRepoImpl : CurrencyImageRepo {
    override fun getImageUrl(currencyCode: String): String {
        val url = "https://flagcdn.com/w160/${currencyCode.substring(0,2).lowercase()}.jpg"
        return url
    }
}
