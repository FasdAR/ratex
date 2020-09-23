package ru.fasdev.ratex.ui.view.bottomSheetSelectCurrency

import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import moxy.MvpPresenter
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateInteractor
import javax.inject.Inject

class SelectCurrencyPresenter @Inject constructor(val currencyRateInteractor: CurrencyRateInteractor): MvpPresenter<SelectCurrencyView>()
{
    var disposables: CompositeDisposable = CompositeDisposable()

    override fun onFirstViewAttach()
    {
        super.onFirstViewAttach()

        disposables.add(
            currencyRateInteractor
                .getAvailableCurrencies()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        viewState.setListCurrency(it)
                    },
                    onError = {
                        //TODO: SET NORMAL ERRRO to VIEW
                        Log.d("ERROR", it.toString())
                    }
                )
        )
    }

    fun searchCurrency(text: String)
    {

    }
}