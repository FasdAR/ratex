package ru.fasdev.ratex.app.di.module.currency

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import ru.fasdev.ratex.app.di.scope.FragmentScope
import ru.fasdev.ratex.data.currencyRate.CurrencyBaseRepoImpl
import ru.fasdev.ratex.data.currencyRate.CurrencyRateRepoImpl
import ru.fasdev.ratex.data.currencyRate.FlagCdnRepoImpl
import ru.fasdev.ratex.data.retrofit.exchangeRates.ExchangeRateApi
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.interactor.CurrencyBaseInteractorImpl
import ru.fasdev.ratex.domain.currency.interactor.CurrencyRateInteractorImpl
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo

@Module
class CurrencyModule
{
    @Provides
    @FragmentScope
    fun provideExchangeRateApi(retrofit: Retrofit): ExchangeRateApi = retrofit.create(ExchangeRateApi::class.java)

    @Provides
    @FragmentScope
    fun provideCurrencyImageRepo(): CurrencyImageRepo = FlagCdnRepoImpl()

    @Provides
    @FragmentScope
    fun provideCurrencyBaseRepo(sharedPrefencesRepo: SharedPrefencesRepo, currencyImageRepo: CurrencyImageRepo): CurrencyBaseRepo = CurrencyBaseRepoImpl(sharedPrefencesRepo, currencyImageRepo)

    @Provides
    @FragmentScope
    fun provideCurrencyRateRepo(exchangeRateApi: ExchangeRateApi, currencyBaseRepo: CurrencyBaseRepo, currencyImageRepo: CurrencyImageRepo): CurrencyRateRepo = CurrencyRateRepoImpl(exchangeRateApi, currencyBaseRepo, currencyImageRepo)

    @Provides
    @FragmentScope
    fun provideCurrencyRateInteractor(currencyRateRepo: CurrencyRateRepo): CurrencyRateInteractor = CurrencyRateInteractorImpl(currencyRateRepo)

    @Provides
    @FragmentScope
    fun currencyBaseInteractor(currencyBaseRepo: CurrencyBaseRepo): CurrencyBaseInteractor = CurrencyBaseInteractorImpl(currencyBaseRepo)
}