package ru.fasdev.ratex.main.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import ru.fasdev.ratex.currency.di.component.CurrencyComponent
import ru.fasdev.ratex.currency.di.component.DaggerCurrencyComponent
import ru.fasdev.ratex.main.RatexApp
import ru.fasdev.ratex.main.navigation.MainNavigation

class MainActivity : ComponentActivity() {
    private val currencyComponent: CurrencyComponent by lazy {
        DaggerCurrencyComponent
            .builder()
            .appComponent(RatexApp.DI.appComponent)
            .build()
    }

    private val viewModelFactory: ViewModelProvider.Factory by lazy { currencyComponent.viewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RatexTheme {
                MainNavigation(viewModelFactory)
            }
        }
    }
}
