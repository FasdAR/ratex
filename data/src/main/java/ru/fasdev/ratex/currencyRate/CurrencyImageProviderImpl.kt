package ru.fasdev.ratex.currencyRate

import android.util.Log
import ru.fasdev.ratex.currencyRate.entity.CurrencyFlag
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyImageProvider

class CurrencyImageProviderImpl : CurrencyImageProvider
{
    override fun getImageUrl(currencyCode: String): String
    {
        val url = "https://flagcdn.com/w160/${currencyCode.substring(0,2).toLowerCase()}.png"
        return url
    }
}