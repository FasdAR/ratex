package ru.fasdev.ratex.ui.view.fragmentListCurrencyRate

import android.content.Context
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import dev.chrisbanes.insetter.applySystemWindowInsetsToPadding
import ru.fasdev.ratex.R
import ru.fasdev.ratex.app.di.component.DaggerFragmentListCurrencyRateComponent
import ru.fasdev.ratex.databinding.ListCurrencyRateFragmentBinding
import ru.fasdev.ratex.ui.view.activityMain.MainActivity
import javax.inject.Inject

class ListCurrencyRateFragment : Fragment()
{
    companion object
    {
        fun newInstance() = ListCurrencyRateFragment()
    }

    private lateinit var binding: ListCurrencyRateFragmentBinding

    @Inject
    lateinit var appCompactActivity: AppCompatActivity

    val fragmentListCurrencyComponent by lazy {
       return@lazy DaggerFragmentListCurrencyRateComponent
           .builder()
           .activityComponent((requireActivity() as MainActivity).activitySubComponent)
           .build()
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

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)
    }
}