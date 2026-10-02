package ru.fasdev.ratex.currency.ui.adapter.listSelectCurrency

import com.airbnb.epoxy.Typed2EpoxyController
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

class ListSelectCurrencyController(val selectListener: ListSelectCurrencyModel.Listener) :
    Typed2EpoxyController<List<CurrencyDomain>, CurrencyDomain>() {
    override fun buildModels(data: List<CurrencyDomain>?, baseCurrency: CurrencyDomain) {
        data?.forEach {
            val isSelected = it.currencyCode == baseCurrency.currencyCode

            val model = ListSelectCurrencyModel_().apply {
                id(it.currencyCode)
                listener = this@ListSelectCurrencyController.selectListener
                currency = it
                selectedState = isSelected
            }

            add(model)
        }
    }
}
