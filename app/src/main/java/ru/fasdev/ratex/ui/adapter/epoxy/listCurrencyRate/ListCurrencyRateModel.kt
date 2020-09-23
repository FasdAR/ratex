package ru.fasdev.ratex.ui.adapter.epoxy.listCurrencyRate

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import ru.fasdev.ratex.R
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
        val nameCurrency = rateCurrency.currency.displayName.substring(0, 1).toUpperCase() + rateCurrency.currency.displayName.substring(
            1
        )

        Glide
            .with(holder.imageCurrency.context as Activity)
            .load(rateCurrency.currency.urlImage)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop()
            .circleCrop()
            .into(holder.imageCurrency);

        holder.nameCurrency.setText(nameCurrency)
        holder.codeCurrency.setText(rateCurrency.currency.currencyCode)
        holder.rateCurrency.setText(DecimalFormat("###.##").format(rateCurrency.rate))
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)

        Glide
            .with(holder.imageCurrency.context as Activity)
            .clear(holder.imageCurrency)
    }
}