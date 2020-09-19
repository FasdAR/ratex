package ru.fasdev.ratex.ui.adapter.epoxy.listCurrencyRate

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import ru.fasdev.ratex.R
import ru.fasdev.ratex.app.di.module.currencyRate.CurrencyRateModule
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain
import java.text.DecimalFormat

@EpoxyModelClass(layout = R.layout.item_currency_rate)
abstract class ListCurrencyRateModel : EpoxyModelWithHolder<ListCurrencyRateModel.Holder>()
{
    class Holder: EpoxyHolder()
    {
        lateinit var imageCurrency: ImageView
        lateinit var nameCurrency: TextView
        lateinit var codeCurrency: TextView
        lateinit var rateCurrency: TextView

        override fun bindView(itemView: View)
        {
            imageCurrency = itemView.findViewById(R.id.image_currency)
            nameCurrency = itemView.findViewById(R.id.name_currency)
            codeCurrency = itemView.findViewById(R.id.code_currency)
            rateCurrency = itemView.findViewById(R.id.rate_currency)
        }
    }

    @EpoxyAttribute
    lateinit var rateCurrency: RateCurrencyDomain

    override fun bind(holder: Holder)
    {
        holder.nameCurrency.setText(rateCurrency.currency.displayName)
        holder.codeCurrency.setText(rateCurrency.currency.currencyCode)
        holder.rateCurrency.setText(DecimalFormat("###.##").format(rateCurrency.rate))
    }
}