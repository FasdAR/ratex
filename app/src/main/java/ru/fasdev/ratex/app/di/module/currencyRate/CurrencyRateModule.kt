package ru.fasdev.ratex.app.di.module.currencyRate

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import ru.fasdev.ratex.app.di.scope.FragmentScope
import ru.fasdev.ratex.data.retrofit.exchangeRates.ExchangeRateApi
import ru.fasdev.ratex.data.retrofit.exchangeRates.ExchangeRateRepoImpl
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateRepo
import ru.fasdev.ratex.domain.currencyRate.interactor.CurrencyRateInteractorImpl
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo

@Module
class CurrencyRateModule
{
    @Provides
    @FragmentScope
    fun provideExchangeRateApi(retrofit: Retrofit): ExchangeRateApi = retrofit.create(ExchangeRateApi::class.java)

    @Provides
    @FragmentScope
    fun provideRateRepo(exchangeRateApi: ExchangeRateApi): CurrencyRateRepo = ExchangeRateRepoImpl(exchangeRateApi)

    @Provides
    @FragmentScope
    fun provideCurrencyRateInteractor(currencyRateRepo: CurrencyRateRepo, sharedPrefencesRepo: SharedPrefencesRepo) : CurrencyRateInteractor = CurrencyRateInteractorImpl(currencyRateRepo, sharedPrefencesRepo)
}