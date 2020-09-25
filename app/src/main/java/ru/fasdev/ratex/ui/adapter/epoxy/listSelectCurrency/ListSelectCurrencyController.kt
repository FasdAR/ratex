package ru.fasdev.ratex.ui.adapter.epoxy.listSelectCurrency

import com.airbnb.epoxy.Typed2EpoxyController
import com.airbnb.epoxy.TypedEpoxyController
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain

class ListSelectCurrencyController : Typed2EpoxyController<List<CurrencyDomain>, CurrencyDomain>()
{
    override fun buildModels(data: List<CurrencyDomain>?, baseCurrency: CurrencyDomain)
    {
        data?.forEach {
            val model = ListSelectCurrencyModel_().apply {
                id(it.currencyCode)
                currency = it
                selectedState = it.currencyCode == baseCurrency.currencyCode
            }

            add(model)
        }
    }
}