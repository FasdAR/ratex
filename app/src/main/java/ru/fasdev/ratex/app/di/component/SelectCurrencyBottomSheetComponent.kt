package ru.fasdev.ratex.app.di.component

import dagger.Component
import ru.fasdev.ratex.app.di.module.app.AppModule
import ru.fasdev.ratex.app.di.module.app.SettingsModule
import ru.fasdev.ratex.app.di.module.network.RetrofitModule
import ru.fasdev.ratex.app.di.scope.AppScope
import ru.fasdev.ratex.app.di.scope.BottomSheetScope
import ru.fasdev.ratex.app.di.scope.FragmentScope
import ru.fasdev.ratex.ui.view.bottomSheetSelectCurrency.SelectCurrencyBottomSheet

@BottomSheetScope
@Component(dependencies = [FragmentListCurrencyRateComponent::class])
interface SelectCurrencyBottomSheetComponent
{
    fun inject(selectCurrencyBottomSheet: SelectCurrencyBottomSheet)
}