package ru.fasdev.ratex.ui.adapter.epoxy.listSelectCurrency

import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
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
        lateinit var radioButton: RadioButton

        override fun bindView(itemView: View) {
            nameCurrency = itemView.findViewById(R.id.name_currency)
            radioButton = itemView.findViewById(R.id.selected_checkbox)
        }
    }

    interface Listener {
        fun selectedCurrency(isChecked: Boolean, currencyDomain: CurrencyDomain)
    }

    @EpoxyAttribute
    lateinit var listener: Listener

    @EpoxyAttribute
    lateinit var currency: CurrencyDomain

    @EpoxyAttribute
    var selectedState: Boolean = false

    override fun bind(holder: Holder)
    {
        holder.nameCurrency.setText(currency.displayName)

        holder.radioButton.isChecked = selectedState

        holder.radioButton.setOnClickListener {
            listener.selectedCurrency(holder.radioButton.isChecked, currency)
        }
    }
}