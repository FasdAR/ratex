package ru.fasdev.ratex.ui.view.bottomSheetSelectCurrency

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import moxy.MvpPresenter
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
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
            Single.zip(currencyBaseInteractor.getAvailableCurrencies(), currencyBaseInteractor.getBaseCurrency(),
                object : BiFunction<List<CurrencyDomain>, CurrencyDomain, Pair<CurrencyDomain, List<CurrencyDomain>>>
            {
                override fun apply(t1: List<CurrencyDomain>, t2: CurrencyDomain): Pair<CurrencyDomain, List<CurrencyDomain>>
                {
                    return Pair(t2, t1)
                }
            })
            .flatMap {
                Single.just(Pair(it.first, currencyBaseInteractor.filterSearchAvailbaleCurrenciesNameCode(it.second, filterSearchCurrency)))
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    viewState.setListCurrency(it.second, it.first)
                },
                onError = {
                    Log.d("ERROR", it.toString())
                }
            )
        )
    }

    fun selectedCurrency(isChecked: Boolean, currencyDomain: CurrencyDomain)
    {
        if (isChecked) {
            currencyBaseInteractor.setBaseCurrency(currencyDomain)

            loadAvailableCurrencies()
        }
    }
}