package ru.fasdev.ratex.ui.view.fragmentListCurrencyRate

import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import moxy.MvpPresenter
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateInteractor
import javax.inject.Inject

class ListCurrencyRatePresenter @Inject constructor(val currencyRateInteractor: CurrencyRateInteractor)
    : MvpPresenter<ListCurrencyRateView>()
{
    var baseCurrencyDispose: Disposable? = null

    override fun onFirstViewAttach()
    {
        super.onFirstViewAttach()

        baseCurrencyDispose = currencyRateInteractor
            .getBaseCurrency()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    viewState.setBaseCurrency(it.currencyCode)
                },
                onError = {
                    Log.e("ERROR", it.toString())
                }
            )
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d("MOXY_ON_DESTROY", "DESTROY")

        baseCurrencyDispose?.dispose()
    }
}