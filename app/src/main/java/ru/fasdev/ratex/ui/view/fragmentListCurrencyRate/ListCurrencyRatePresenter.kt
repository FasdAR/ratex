package ru.fasdev.ratex.ui.view.fragmentListCurrencyRate

import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import moxy.MvpPresenter
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateInteractor
import javax.inject.Inject

class ListCurrencyRatePresenter @Inject constructor(val currencyRateInteractor: CurrencyRateInteractor)
    : MvpPresenter<ListCurrencyRateView>()
{
    var disposables: CompositeDisposable = CompositeDisposable()

    override fun onFirstViewAttach()
    {
        super.onFirstViewAttach()

        disposables.add(
            currencyRateInteractor
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
        )

        loadExchangeRates()
    }

    override fun onDestroy() {
        super.onDestroy()

        disposables.dispose()
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
                    Log.d("RX", "FINNALY")
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