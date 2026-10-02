package ru.fasdev.ratex.currency.di.component

import dagger.Component
import ru.fasdev.ratex.currency.ui.bottomSheetSelectCurrency.SelectCurrencyBottomSheet
import ru.fasdev.ratex.main.di.scope.BottomSheetScope

@BottomSheetScope
@Component(dependencies = [FragmentListCurrencyRateComponent::class])
interface SelectCurrencyBottomSheetComponent {
    fun inject(selectCurrencyBottomSheet: SelectCurrencyBottomSheet)
}
