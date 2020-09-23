package ru.fasdev.ratex.ui.adapter.epoxy.listCurrencyRate

import android.app.Activity
import android.content.Context
import android.graphics.drawable.PictureDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import ru.fasdev.ratex.R
import ru.fasdev.ratex.app.util.dp
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain
import ru.fasdev.ratex.ui.view.activityMain.MainActivity
import ru.fasdev.ratex.ui.view.provider.FragmentProvider
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
            .with(holder.imageCurrency.context.applicationContext)
            .load(rateCurrency.currency.urlImage)
            .transition(withCrossFade())
            .transform(MultiTransformation(CenterCrop(), RoundedCorners(50.dp)))
            .into(holder.imageCurrency);

        holder.nameCurrency.setText(nameCurrency)
        holder.codeCurrency.setText(rateCurrency.currency.currencyCode)
        holder.rateCurrency.setText(DecimalFormat("###.##").format(rateCurrency.rate))
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)

        Glide
            .with(holder.imageCurrency.context.applicationContext)
            .clear(holder.imageCurrency)
    }
}