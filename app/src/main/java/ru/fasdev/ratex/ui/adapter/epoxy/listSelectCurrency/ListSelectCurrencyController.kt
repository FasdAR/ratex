package ru.fasdev.ratex.ui.adapter.epoxy.listSelectCurrency

import android.util.Log
import com.airbnb.epoxy.Typed2EpoxyController
import com.airbnb.epoxy.TypedEpoxyController
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain

class ListSelectCurrencyController(val selectListener: ListSelectCurrencyModel.Listener) : Typed2EpoxyController<List<CurrencyDomain>, CurrencyDomain>()
{
    override fun buildModels(data: List<CurrencyDomain>?, baseCurrency: CurrencyDomain)
    {
        data?.forEach {
            val isSelected = it.currencyCode == baseCurrency.currencyCode

            val model = ListSelectCurrencyModel_().apply {
                id(it.currencyCode)
                listener = selectListener
                currency = it
                selectedState = isSelected
            }

            add(model)
        }
    }
}