package ru.fasdev.ratex.domain.currency.interactor

import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain

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
