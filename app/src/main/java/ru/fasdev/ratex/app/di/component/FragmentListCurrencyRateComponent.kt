package ru.fasdev.ratex.app.di.component

import dagger.Component
import ru.fasdev.ratex.app.di.module.currencyRate.CurrencyRateModule
import ru.fasdev.ratex.app.di.scope.FragmentScope
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.ui.view.fragmentListCurrencyRate.ListCurrencyRateFragment

@FragmentScope
@Component(dependencies = [ActivityComponent::class], modules = [CurrencyRateModule::class])
interface FragmentListCurrencyRateComponent
{
    //Child dependencies
    fun sharedPrefencesRepo(): SharedPrefencesRepo

    fun inject(fragment: ListCurrencyRateFragment)
}