package ru.fasdev.ratex.currencyRate

import ru.fasdev.ratex.currencyRate.entity.CurrencyFlag
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyImageProvider

class CurrencyImageProviderImpl : CurrencyImageProvider
{
    override fun getImageUrl(currencyCode: String): String
    {
        val flag: CurrencyFlag = CurrencyFlag.valueOf(currencyCode.toUpperCase())
        return flag.urlFlag
    }
}