package ru.fasdev.ratex.data.currencyRate.repo

import java.util.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.extension.toCurrencyDomain
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo

class CurrencyBaseRepoImpl(
    val sharedPrefencesRepo: SharedPrefencesRepo,
    val currencyImageRepo: CurrencyImageRepo,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CurrencyBaseRepo {
    private val arrayActualCodeCurrency =
        arrayListOf(
            "CAD", "HKD", "ISK", "PHP", "DKK", "HUF", "CZK", "GBP", "RON", "SEK", "IDR",
            "INR", "BRL", "RUB", "HRK", "JPY", "THB", "CHF", "EUR", "MYR", "BGN", "TRY",
            "CNY", "NOK", "NZD", "ZAR", "USD", "MXN", "SGD", "AUD", "ILS", "KRW", "PLN"
        )
    private var listAvailableCurrencies: List<CurrencyDomain>? = null

    override suspend fun getBaseCurrency(): CurrencyDomain = withContext(ioDispatcher) {
        if (!sharedPrefencesRepo.getBaseCurrencyCode().isNullOrEmpty()) {
            val baseCurrency = Currency.getInstance(sharedPrefencesRepo.getBaseCurrencyCode())
            baseCurrency.toCurrencyDomain()
        } else {
            Currency.getInstance(Locale.getDefault()).toCurrencyDomain()
        }
    }

    override fun setBaseCurrency(baseCurrency: CurrencyDomain) {
        sharedPrefencesRepo.setBaseCurrencyCode(baseCurrency.currencyCode)
    }

    override suspend fun getAvailableCurrencies(): List<CurrencyDomain> = withContext(ioDispatcher) {
        val availableCurrencies = listAvailableCurrencies

        if (availableCurrencies.isNullOrEmpty()) {
            arrayActualCodeCurrency
                .map { it -> CurrencyDomain.getInstance(it, currencyImageRepo) }
                .also { listAvailableCurrencies = it }
        } else {
            availableCurrencies
        }
    }
}
