package ru.fasdev.ratex.ui.provider

import androidx.fragment.app.Fragment

interface FragmentProvider
{
    fun getCurrentFragment(): Fragment?
}