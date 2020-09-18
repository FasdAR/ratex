package ru.fasdev.ratex.app.di.component

import dagger.Component
import ru.fasdev.ratex.app.di.scope.FragmentScope
import ru.fasdev.ratex.ui.view.fragmentListCurrencyRate.ListCurrencyRateFragment

@FragmentScope
@Component(dependencies = [ActivityComponent::class])
interface FragmentListCurrencyRateComponent
{
    fun inject(fragment: ListCurrencyRateFragment)
}