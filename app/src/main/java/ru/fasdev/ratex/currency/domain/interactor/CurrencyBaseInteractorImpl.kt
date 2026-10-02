package ru.fasdev.ratex.currency.domain.interactor

import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

class CurrencyBaseInteractorImpl(val currencyBaseRepo: CurrencyBaseRepo) : CurrencyBaseInteractor {
    override suspend fun getBaseCurrency(): CurrencyDomain = currencyBaseRepo.getBaseCurrency()

    override fun setBaseCurrency(baseCurrency: CurrencyDomain) {
        currencyBaseRepo.setBaseCurrency(baseCurrency)
    }

    override suspend fun getAvailableCurrencies(): List<CurrencyDomain> = currencyBaseRepo.getAvailableCurrencies()
        .sortedBy {
            it.displayName
        }

    override fun filterSearchAvailbaleCurrenciesNameCode(list: List<CurrencyDomain>, nameCode: String?): List<CurrencyDomain> {
        if (nameCode.isNullOrEmpty()) {
            return list
        } else {
            val name = nameCode.lowercase()

            if (name.length > 2) {
                return list.filter { it -> it.displayName.lowercase().contains(name) || it.currencyCode.lowercase().contains(name) }
            }
            return emptyList()
        }
    }
}
