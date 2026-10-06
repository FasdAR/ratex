package ru.fasdev.ratex.currency.data.repo

import java.util.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fasdev.ratex.core.domain.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.extension.isKnownCurrencyCode
import ru.fasdev.ratex.currency.domain.entity.extension.toCurrencyDomain

class CurrencyBaseRepoImpl(
    val sharedPrefencesRepo: SharedPrefencesRepo,
    val currencyImageRepo: CurrencyImageRepo,
    val currencyRateRepo: CurrencyRateRepo,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CurrencyBaseRepo {
    /**
     * Сохранённая база, а если её нет — валюта локали. Если источник такой валюты не знает (RUB, BGN, HRK, локаль без валюты),
     * берём базу источника. Сохранённое значение при этом не перезаписывается.
     */
    override suspend fun getBaseCurrency(): CurrencyDomain {
        val snapshot = currencyRateRepo.getSnapshot()

        return withContext(ioDispatcher) {
            val preferredCode = sharedPrefencesRepo.getBaseCurrencyCode().takeUnless { it.isNullOrEmpty() }
                ?: runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull()

            val code = if (preferredCode != null && preferredCode in snapshot.availableCodes) preferredCode else snapshot.baseCode
            Currency.getInstance(code).toCurrencyDomain()
        }
    }

    override fun setBaseCurrency(baseCurrency: CurrencyDomain) {
        sharedPrefencesRepo.setBaseCurrencyCode(baseCurrency.currencyCode)
    }

    override suspend fun getAvailableCurrencies(): List<CurrencyDomain> {
        val snapshot = currencyRateRepo.getSnapshot()

        return withContext(ioDispatcher) {
            snapshot.availableCodes
                .filter { isKnownCurrencyCode(it) }
                .map { CurrencyDomain.getInstance(it, currencyImageRepo) }
        }
    }
}
