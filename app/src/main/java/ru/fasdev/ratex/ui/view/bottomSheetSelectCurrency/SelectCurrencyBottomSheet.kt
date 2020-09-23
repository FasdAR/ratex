package ru.fasdev.ratex.ui.view.bottomSheetSelectCurrency

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.fasdev.ratex.R
import ru.fasdev.ratex.databinding.SelectCurrencyBottomSheetBinding

class SelectCurrencyBottomSheet : BottomSheetDialogFragment()
{
    private lateinit var binding: SelectCurrencyBottomSheetBinding

    companion object
    {
        const val TAG = "SHEET_CURRENCY_BOTTOM_SHEET"

        fun newInstance() = SelectCurrencyBottomSheet()

        fun show(fragmentManager: FragmentManager) {
            val fragment = newInstance()
            fragment.show(fragmentManager, TAG)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        binding = SelectCurrencyBottomSheetBinding.inflate(inflater)
        return binding.root
    }

    override fun getTheme(): Int = R.style.BaseBottomSheet
}