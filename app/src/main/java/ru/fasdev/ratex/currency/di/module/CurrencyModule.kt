package ru.fasdev.ratex.currency.di.module

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import ru.fasdev.ratex.core.domain.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.currency.data.api.ExchangeRateApi
import ru.fasdev.ratex.currency.data.dataStore.CurrencyRateDataStore
import ru.fasdev.ratex.currency.data.dataStore.source.ExchangeRateDataStore
import ru.fasdev.ratex.currency.data.repo.CurrencyBaseRepoImpl
import ru.fasdev.ratex.currency.data.repo.CurrencyRateRepoImpl
import ru.fasdev.ratex.currency.data.repo.FlagCdnRepoImpl
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.interactor.CurrencyBaseInteractorImpl
import ru.fasdev.ratex.currency.domain.interactor.CurrencyRateInteractorImpl
import ru.fasdev.ratex.main.di.scope.FragmentScope

@Module
class CurrencyModule {
    @Provides
    @FragmentScope
    fun provideExchangeRateApi(retrofit: Retrofit): ExchangeRateApi = retrofit.create(ExchangeRateApi::class.java)

    @Provides
    @FragmentScope
    fun provideCurrencyImageRepo(): CurrencyImageRepo = FlagCdnRepoImpl()

    @Provides
    @FragmentScope
    fun provideCurrencyBaseRepo(sharedPrefencesRepo: SharedPrefencesRepo, currencyImageRepo: CurrencyImageRepo): CurrencyBaseRepo =
        CurrencyBaseRepoImpl(sharedPrefencesRepo, currencyImageRepo)

    @Provides
    @FragmentScope
    fun currencyRateDataStore(exchangeRateApi: ExchangeRateApi, currencyImageRepo: CurrencyImageRepo): CurrencyRateDataStore =
        ExchangeRateDataStore(exchangeRateApi, currencyImageRepo)

    @Provides
    @FragmentScope
    fun provideCurrencyRateRepo(currencyRateDataStore: CurrencyRateDataStore, currencyBaseRepo: CurrencyBaseRepo): CurrencyRateRepo =
        CurrencyRateRepoImpl(currencyRateDataStore, currencyBaseRepo)

    @Provides
    @FragmentScope
    fun provideCurrencyRateInteractor(currencyRateRepo: CurrencyRateRepo): CurrencyRateInteractor =
        CurrencyRateInteractorImpl(currencyRateRepo)

    @Provides
    @FragmentScope
    fun currencyBaseInteractor(currencyBaseRepo: CurrencyBaseRepo): CurrencyBaseInteractor = CurrencyBaseInteractorImpl(currencyBaseRepo)
}
