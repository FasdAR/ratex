package ru.fasdev.ratex.ui.view.bottomSheetSelectCurrency

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import moxy.MvpBottomSheetDialogFragment
import moxy.ktx.moxyPresenter
import ru.fasdev.ratex.R
import ru.fasdev.ratex.app.di.component.DaggerSelectCurrencyBottomSheetComponent
import ru.fasdev.ratex.databinding.SelectCurrencyBottomSheetBinding
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.ui.adapter.epoxy.listSelectCurrency.ListSelectCurrencyController
import ru.fasdev.ratex.ui.view.fragmentListCurrencyRate.ListCurrencyRateFragment
import javax.inject.Inject
import javax.inject.Provider

class SelectCurrencyBottomSheet : MvpBottomSheetDialogFragment(), SelectCurrencyView
{
    private lateinit var binding: SelectCurrencyBottomSheetBinding

    @Inject
    lateinit var presenterProvider: Provider<SelectCurrencyPresenter>
    private val presenter by moxyPresenter { presenterProvider.get() }

    val selectCurrencyComponent by lazy {
        return@lazy DaggerSelectCurrencyBottomSheetComponent
            .builder()
            .fragmentListCurrencyRateComponent((requireParentFragment() as ListCurrencyRateFragment).fragmentListCurrencyComponent)
            .build()
    }

    val listSelectCurrenyController: ListSelectCurrencyController = ListSelectCurrencyController()

    companion object
    {
        const val TAG = "SHEET_CURRENCY_BOTTOM_SHEET"

        fun newInstance() = SelectCurrencyBottomSheet()

        fun show(fragmentManager: FragmentManager) {
            val fragment = newInstance()
            fragment.show(fragmentManager, TAG)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        selectCurrencyComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        binding = SelectCurrencyBottomSheetBinding.inflate(inflater)

        binding.currencyList.layoutManager = LinearLayoutManager(context)
        binding.currencyList.setController(listSelectCurrenyController)

        binding.searchCurrency.doOnTextChanged { text, start, before, count ->
            presenter.searchCurrency(text.toString())
        }

        return binding.root
    }

    override fun getTheme(): Int = R.style.BaseBottomSheet

    override fun setListCurrency(list: List<CurrencyDomain>)
    {
        listSelectCurrenyController.setData(list)
    }
}