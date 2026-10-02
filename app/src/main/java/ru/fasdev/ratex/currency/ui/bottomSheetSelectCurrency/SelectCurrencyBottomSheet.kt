package ru.fasdev.ratex.currency.ui.bottomSheetSelectCurrency

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import javax.inject.Inject
import javax.inject.Provider
import moxy.MvpBottomSheetDialogFragment
import moxy.ktx.moxyPresenter
import ru.fasdev.ratex.R
import ru.fasdev.ratex.currency.di.component.DaggerSelectCurrencyBottomSheetComponent
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.ui.adapter.listSelectCurrency.ListSelectCurrencyController
import ru.fasdev.ratex.currency.ui.adapter.listSelectCurrency.ListSelectCurrencyModel
import ru.fasdev.ratex.currency.ui.fragmentListCurrencyRate.ListCurrencyRateFragment
import ru.fasdev.ratex.databinding.SelectCurrencyBottomSheetBinding

class SelectCurrencyBottomSheet :
    MvpBottomSheetDialogFragment(),
    SelectCurrencyView,
    ListSelectCurrencyModel.Listener {
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

    val listSelectCurrenyController: ListSelectCurrencyController = ListSelectCurrencyController(this)

    companion object {
        const val TAG = "SHEET_CURRENCY_BOTTOM_SHEET"

        fun newInstance() = SelectCurrencyBottomSheet()

        fun show(fragmentManager: FragmentManager): Fragment {
            val fragment = newInstance()
            fragment.show(fragmentManager, TAG)

            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        selectCurrencyComponent.inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener { dialog ->
            val wm = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay

            val d = dialog as BottomSheetDialog
            dialog.behavior.peekHeight = (display.height / 1.5f).toInt()
        }

        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = SelectCurrencyBottomSheetBinding.inflate(inflater)

        binding.currencyList.layoutManager = LinearLayoutManager(context)
        binding.currencyList.setController(listSelectCurrenyController)

        binding.searchCurrency.doOnTextChanged { text, start, before, count ->
            presenter.searchCurrency(text.toString())
        }

        return binding.root
    }

    override fun getTheme(): Int = R.style.BaseBottomSheet

    override fun setListCurrency(list: List<CurrencyDomain>, baseCurrency: CurrencyDomain) {
        listSelectCurrenyController.setData(list, baseCurrency)
    }

    override fun selectedCurrency(isChecked: Boolean, currencyDomain: CurrencyDomain) {
        presenter.selectedCurrency(isChecked, currencyDomain)
    }
}
