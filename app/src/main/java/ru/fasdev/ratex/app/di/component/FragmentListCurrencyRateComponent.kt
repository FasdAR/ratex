package ru.fasdev.ratex.app.di.component

import dagger.Component
import ru.fasdev.ratex.app.di.module.currency.CurrencyModule
import ru.fasdev.ratex.app.di.scope.FragmentScope
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.ui.view.fragmentListCurrencyRate.ListCurrencyRateFragment

@FragmentScope
@Component(dependencies = [ActivityComponent::class], modules = [CurrencyModule::class])
interface FragmentListCurrencyRateComponent
{
    //Child dependencies
    fun currencyBaseInteractor(): CurrencyBaseInteractor

    fun inject(fragment: ListCurrencyRateFragment)
}