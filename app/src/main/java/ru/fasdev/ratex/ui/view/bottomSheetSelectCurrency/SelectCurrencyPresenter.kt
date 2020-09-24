package ru.fasdev.ratex.ui.view.bottomSheetSelectCurrency

import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import moxy.MvpPresenter
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyRateInteractor
import javax.inject.Inject

class SelectCurrencyPresenter @Inject constructor(val currencyBaseInteractor: CurrencyBaseInteractor): MvpPresenter<SelectCurrencyView>()
{
    var filterSearchCurrency: String? = null

    var disposables: CompositeDisposable = CompositeDisposable()

    override fun onFirstViewAttach()
    {
        super.onFirstViewAttach()

        loadAvailableCurrencies()
    }

    fun searchCurrency(text: String)
    {
        filterSearchCurrency = text
        loadAvailableCurrencies()
    }

    private fun loadAvailableCurrencies()
    {
        disposables.add(
            currencyBaseInteractor
                .getAvailableCurrencies()
                .map{it-> currencyBaseInteractor.filterSearchAvailbaleCurrenciesNameCode(it, filterSearchCurrency)}
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
}