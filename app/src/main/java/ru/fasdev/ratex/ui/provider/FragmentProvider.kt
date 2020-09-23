package ru.fasdev.ratex.ui.view.provider

import androidx.fragment.app.Fragment

interface FragmentProvider
{
    fun getCurrentFragment(): Fragment?
}