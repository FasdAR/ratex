package ru.fasdev.ratex.currency.di.component

import dagger.Component
import ru.fasdev.ratex.currency.di.module.CurrencyModule
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.currency.ui.fragmentListCurrencyRate.ListCurrencyRateFragment
import ru.fasdev.ratex.main.di.component.ActivityComponent
import ru.fasdev.ratex.main.di.scope.FragmentScope

@FragmentScope
@Component(dependencies = [ActivityComponent::class], modules = [CurrencyModule::class])
interface FragmentListCurrencyRateComponent {
    // Child dependencies
    fun currencyBaseInteractor(): CurrencyBaseInteractor

    fun inject(fragment: ListCurrencyRateFragment)
}
