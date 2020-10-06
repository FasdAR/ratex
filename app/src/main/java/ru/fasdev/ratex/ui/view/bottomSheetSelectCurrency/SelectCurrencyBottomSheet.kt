package ru.fasdev.ratex.ui.view.bottomSheetSelectCurrency

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import moxy.MvpBottomSheetDialogFragment
import moxy.ktx.moxyPresenter
import ru.fasdev.ratex.R
import ru.fasdev.ratex.app.di.component.DaggerSelectCurrencyBottomSheetComponent
import ru.fasdev.ratex.app.util.dp
import ru.fasdev.ratex.databinding.SelectCurrencyBottomSheetBinding
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.ui.adapter.epoxy.listSelectCurrency.ListSelectCurrencyController
import ru.fasdev.ratex.ui.adapter.epoxy.listSelectCurrency.ListSelectCurrencyModel
import ru.fasdev.ratex.ui.view.fragmentListCurrencyRate.ListCurrencyRateFragment
import javax.inject.Inject
import javax.inject.Provider


class SelectCurrencyBottomSheet : MvpBottomSheetDialogFragment(), SelectCurrencyView, ListSelectCurrencyModel.Listener
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

    val listSelectCurrenyController: ListSelectCurrencyController = ListSelectCurrencyController(this)

    companion object
    {
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
    {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener { dialog ->
            val wm = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay

            val d = dialog as BottomSheetDialog
            dialog.behavior.peekHeight = (display.height / 1.5f).toInt()
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?
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

    override fun setListCurrency(list: List<CurrencyDomain>, baseCurrency: CurrencyDomain)
    {
        listSelectCurrenyController.setData(list, baseCurrency)
    }

    override fun selectedCurrency(isChecked: Boolean, currencyDomain: CurrencyDomain)
    {
        presenter.selectedCurrency(isChecked, currencyDomain)
    }
}