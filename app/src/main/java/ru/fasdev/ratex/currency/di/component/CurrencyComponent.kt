package ru.fasdev.ratex.currency.di.component

import androidx.lifecycle.ViewModelProvider
import dagger.Component
import ru.fasdev.ratex.currency.di.module.CurrencyModule
import ru.fasdev.ratex.currency.di.scope.CurrencyScope
import ru.fasdev.ratex.main.di.component.AppComponent

@CurrencyScope
@Component(dependencies = [AppComponent::class], modules = [CurrencyModule::class])
interface CurrencyComponent {
    fun viewModelFactory(): ViewModelProvider.Factory
}
