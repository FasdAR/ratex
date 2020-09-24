package ru.fasdev.ratex.ui.adapter.epoxy.listSelectCurrency

import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import ru.fasdev.ratex.R
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain

@EpoxyModelClass(layout = R.layout.item_currency_select)
abstract class ListSelectCurrencyModel : EpoxyModelWithHolder<ListSelectCurrencyModel.Holder>()
{
    class Holder: EpoxyHolder()
    {
        lateinit var nameCurrency: TextView
        lateinit var checkBox: CheckBox

        override fun bindView(itemView: View) {
            nameCurrency = itemView.findViewById(R.id.name_currency)
            checkBox = itemView.findViewById(R.id.selected_checkbox)
        }
    }

    @EpoxyAttribute
    lateinit var currency: CurrencyDomain

    override fun bind(holder: Holder)
    {
        holder.nameCurrency.setText(currency.displayName)
    }
}