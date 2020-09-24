package ru.fasdev.ratex.ui.adapter.epoxy.listSelectCurrency

import com.airbnb.epoxy.TypedEpoxyController
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain

class ListSelectCurrencyController : TypedEpoxyController<List<CurrencyDomain>>()
{
    override fun buildModels(data: List<CurrencyDomain>?)
    {
        data?.forEach {
            val model = ListSelectCurrencyModel_().apply {
                id(it.currencyCode)
                currency = it
            }

            add(model)
        }
    }
}