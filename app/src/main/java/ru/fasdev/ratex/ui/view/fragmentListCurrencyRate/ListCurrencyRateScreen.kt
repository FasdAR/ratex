package ru.fasdev.ratex.ui.view.fragmentListCurrencyRate

import androidx.fragment.app.Fragment
import ru.terrakok.cicerone.android.support.SupportAppScreen

class ListCurrencyRateScreen : SupportAppScreen()
{
    override fun getFragment(): Fragment? {
        return ListCurrencyRateFragment.newInstance()
    }
}