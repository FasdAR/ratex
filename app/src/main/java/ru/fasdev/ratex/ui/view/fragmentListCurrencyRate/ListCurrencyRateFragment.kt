package ru.fasdev.ratex.ui.view.fragmentListCurrencyRate

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import dev.chrisbanes.insetter.applySystemWindowInsetsToPadding
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import ru.fasdev.ratex.R
import ru.fasdev.ratex.app.di.component.DaggerFragmentListCurrencyRateComponent
import ru.fasdev.ratex.databinding.ListCurrencyRateFragmentBinding
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain
import ru.fasdev.ratex.ui.adapter.epoxy.listCurrencyRate.ListCurrencyRateController
import ru.fasdev.ratex.ui.view.activityMain.MainActivity
import javax.inject.Inject
import javax.inject.Provider

class ListCurrencyRateFragment : MvpAppCompatFragment(), ListCurrencyRateView
{
    companion object
    {
        fun newInstance() = ListCurrencyRateFragment()
    }

    private lateinit var binding: ListCurrencyRateFragmentBinding

    @Inject
    lateinit var appCompactActivity: AppCompatActivity

    @Inject
    lateinit var presenterProvider: Provider<ListCurrencyRatePresenter>
    private val presenter by moxyPresenter {presenterProvider.get()}

    val fragmentListCurrencyComponent by lazy {
       return@lazy DaggerFragmentListCurrencyRateComponent
           .builder()
           .activityComponent((requireActivity() as MainActivity).activitySubComponent)
           .build()
    }

    private val listRateController by lazy {
        return@lazy ListCurrencyRateController()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        fragmentListCurrencyComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        binding = ListCurrencyRateFragmentBinding.inflate(inflater)

        binding.root.applySystemWindowInsetsToPadding(bottom = true, top = true)

        val toolbar = binding.root.findViewById<Toolbar>(R.id.toolbar)

        appCompactActivity.setSupportActionBar(toolbar)
        appCompactActivity.supportActionBar?.title = ""

        toolbar.setTitle(resources.getString(R.string.app_name))

        binding.swipeRefresh.setOnRefreshListener {
            presenter.loadExchangeRates()
        }

        binding.listCurrency.layoutManager = LinearLayoutManager(requireContext())
        binding.listCurrency.setController(listRateController)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)
    }

    override fun setBaseCurrency(currency: String)
    {
        binding.baseCurrency.setText(currency)
    }

    override fun setListExchangeRates(rateList: List<RateCurrencyDomain>)
    {
        listRateController.setData(rateList)
    }

    override fun setRefreshingState(isRefreshing: Boolean)
    {
        binding.swipeRefresh.isRefreshing = isRefreshing
    }

    override fun setNetworkError(message: String)
    {
        Toast.makeText(appCompactActivity, message, Toast.LENGTH_SHORT).show()
    }
}