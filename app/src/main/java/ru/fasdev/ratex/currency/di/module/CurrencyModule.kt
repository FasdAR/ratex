package ru.fasdev.ratex.currency.di.module

import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient
import ru.fasdev.ratex.core.domain.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.currency.data.repo.CurrencyBaseRepoImpl
import ru.fasdev.ratex.currency.data.repo.CurrencyRateRepoImpl
import ru.fasdev.ratex.currency.data.repo.FlagCdnRepoImpl
import ru.fasdev.ratex.currency.data.source.CurrencyRateSource
import ru.fasdev.ratex.currency.data.source.ecb.EcbRateSource
import ru.fasdev.ratex.currency.data.storage.CurrencyDatabase
import ru.fasdev.ratex.currency.data.storage.RateSnapshotStorage
import ru.fasdev.ratex.currency.data.storage.RoomRateSnapshotStorage
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
    fun provideCurrencyRateSource(httpClient: HttpClient): CurrencyRateSource = EcbRateSource(httpClient)

    @Provides
    @CurrencyScope
    fun provideRateSnapshotStorage(database: CurrencyDatabase): RateSnapshotStorage = RoomRateSnapshotStorage(database.rateSnapshotDao())

    @Provides
    @CurrencyScope
    fun provideCurrencyImageRepo(): CurrencyImageRepo = FlagCdnRepoImpl()

    @Provides
    @CurrencyScope
    fun provideCurrencyRateRepo(source: CurrencyRateSource, storage: RateSnapshotStorage): CurrencyRateRepo =
        CurrencyRateRepoImpl(source, storage)

    @Provides
    @CurrencyScope
    fun provideCurrencyBaseRepo(
        sharedPrefencesRepo: SharedPrefencesRepo,
        currencyImageRepo: CurrencyImageRepo,
        currencyRateRepo: CurrencyRateRepo
    ): CurrencyBaseRepo = CurrencyBaseRepoImpl(sharedPrefencesRepo, currencyImageRepo, currencyRateRepo)

    @Provides
    @CurrencyScope
    fun provideCurrencyRateInteractor(
        currencyRateRepo: CurrencyRateRepo,
        currencyBaseRepo: CurrencyBaseRepo,
        currencyImageRepo: CurrencyImageRepo
    ): CurrencyRateInteractor = CurrencyRateInteractorImpl(currencyRateRepo, currencyBaseRepo, currencyImageRepo)

    @Provides
    @CurrencyScope
    fun currencyBaseInteractor(currencyBaseRepo: CurrencyBaseRepo): CurrencyBaseInteractor = CurrencyBaseInteractorImpl(currencyBaseRepo)

    @Provides
    @CurrencyScope
    fun provideViewModelFactory(factory: CurrencyViewModelFactory): ViewModelProvider.Factory = factory
}
