package ru.fasdev.ratex.currency.di.module

import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient
import ru.fasdev.ratex.core.domain.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.currency.data.api.ExchangeRateApi
import ru.fasdev.ratex.currency.data.api.ExchangeRateApiImpl
import ru.fasdev.ratex.currency.data.dataStore.CurrencyRateDataStore
import ru.fasdev.ratex.currency.data.dataStore.source.ExchangeRateDataStore
import ru.fasdev.ratex.currency.data.repo.CurrencyBaseRepoImpl
import ru.fasdev.ratex.currency.data.repo.CurrencyRateRepoImpl
import ru.fasdev.ratex.currency.data.repo.FlagCdnRepoImpl
import ru.fasdev.ratex.currency.di.CurrencyViewModelFactory
import ru.fasdev.ratex.currency.di.scope.CurrencyScope
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.interactor.CurrencyBaseInteractorImpl
import ru.fasdev.ratex.currency.domain.interactor.CurrencyRateInteractorImpl

@Module
class CurrencyModule {
    @Provides
    @CurrencyScope
    fun provideExchangeRateApi(httpClient: HttpClient): ExchangeRateApi = ExchangeRateApiImpl(httpClient)

    @Provides
    @CurrencyScope
    fun provideCurrencyImageRepo(): CurrencyImageRepo = FlagCdnRepoImpl()

    @Provides
    @CurrencyScope
    fun provideCurrencyBaseRepo(sharedPrefencesRepo: SharedPrefencesRepo, currencyImageRepo: CurrencyImageRepo): CurrencyBaseRepo =
        CurrencyBaseRepoImpl(sharedPrefencesRepo, currencyImageRepo)

    @Provides
    @CurrencyScope
    fun currencyRateDataStore(exchangeRateApi: ExchangeRateApi, currencyImageRepo: CurrencyImageRepo): CurrencyRateDataStore =
        ExchangeRateDataStore(exchangeRateApi, currencyImageRepo)

    @Provides
    @CurrencyScope
    fun provideCurrencyRateRepo(currencyRateDataStore: CurrencyRateDataStore, currencyBaseRepo: CurrencyBaseRepo): CurrencyRateRepo =
        CurrencyRateRepoImpl(currencyRateDataStore, currencyBaseRepo)

    @Provides
    @CurrencyScope
    fun provideCurrencyRateInteractor(currencyRateRepo: CurrencyRateRepo): CurrencyRateInteractor =
        CurrencyRateInteractorImpl(currencyRateRepo)

    @Provides
    @CurrencyScope
    fun currencyBaseInteractor(currencyBaseRepo: CurrencyBaseRepo): CurrencyBaseInteractor = CurrencyBaseInteractorImpl(currencyBaseRepo)

    @Provides
    @CurrencyScope
    fun provideViewModelFactory(factory: CurrencyViewModelFactory): ViewModelProvider.Factory = factory
}
