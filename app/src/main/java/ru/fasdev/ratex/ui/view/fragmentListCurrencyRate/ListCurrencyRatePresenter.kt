package ru.fasdev.ratex.ui.view.fragmentListCurrencyRate

import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import moxy.MvpPresenter
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import javax.inject.Inject

class ListCurrencyRatePresenter @Inject constructor(val currencyBaseInteractor: CurrencyBaseInteractor, val currencyRateInteractor: CurrencyRateInteractor)
    : MvpPresenter<ListCurrencyRateView>()
{
    var disposables: CompositeDisposable = CompositeDisposable()

    override fun onFirstViewAttach()
    {
        super.onFirstViewAttach()

        getBaseCurrency()
        loadExchangeRates()
    }

    override fun onDestroy() {
        super.onDestroy()

        disposables.dispose()
    }

    fun getBaseCurrency()
    {
        disposables.add(
            currencyBaseInteractor
                .getBaseCurrency()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        viewState.setBaseCurrency(it.currencyCode)
                    },
                    onError = {
                        //TOOD: SEt NORMAL ERROR TO VIEW
                        Log.e("ERROR", it.toString())
                    }
                )
        )
    }

    fun loadExchangeRates()
    {
        disposables.add(
            currencyRateInteractor
                .getExchangeRates()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    viewState.setRefreshingState(true)
                }
                .doFinally {
                    viewState.setRefreshingState(false)
                }
                .subscribeBy (
                    onSuccess = {
                        viewState.setListExchangeRates(it)
                    },
                    onError = {
                        //TODO: CHANGE TO NORMAL MESSAGE IN CODE ... 400, 404 ....
                        viewState.setNetworkError(it.message.toString())
                    }
                )
        )
    }
}