package ru.fasdev.ratex.currency.ui.adapter.listCurrencyRate

import com.airbnb.epoxy.TypedEpoxyController
import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain

class ListCurrencyRateController : TypedEpoxyController<List<RateCurrencyDomain>>() {
    override fun buildModels(data: List<RateCurrencyDomain>?) {
        data?.forEach {
            val model = ListCurrencyRateModel_().apply {
                id(it.currency.currencyCode)
                rateCurrency = it
            }

            add(model)
        }
    }
}
