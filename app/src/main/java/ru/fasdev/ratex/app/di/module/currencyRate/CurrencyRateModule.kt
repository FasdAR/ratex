package ru.fasdev.ratex.app.di.module.currencyRate

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import ru.fasdev.ratex.app.di.scope.FragmentScope
import ru.fasdev.ratex.data.currencyRate.FlagCdnRepoImpl
import ru.fasdev.ratex.data.retrofit.exchangeRates.ExchangeRateApi
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.interactor.CurrencyRateInteractorImpl
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo

@Module
class CurrencyRateModule
{
    @Provides
    @FragmentScope
    fun provideExchangeRateApi(retrofit: Retrofit): ExchangeRateApi = retrofit.create(ExchangeRateApi::class.java)

    @Provides
    @FragmentScope
    fun provideCurrencyImageProvider(): CurrencyImageRepo = FlagCdnRepoImpl()

    @Provides
    @FragmentScope
    fun provideRateRepo(exchangeRateApi: ExchangeRateApi, currencyImageProvider: CurrencyImageRepo): CurrencyRateRepo = ExchangeRateRepoImpl(exchangeRateApi, currencyImageProvider)

    @Provides
    @FragmentScope
    fun provideCurrencyRateInteractor(currencyRateRepo: CurrencyRateRepo, sharedPrefencesRepo: SharedPrefencesRepo, currencyImageProvider: CurrencyImageRepo) : CurrencyRateInteractor = CurrencyRateInteractorImpl(currencyRateRepo, sharedPrefencesRepo, currencyImageProvider)
}