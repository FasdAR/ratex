package ru.fasdev.ratex.main.navigation

import androidx.fragment.app.Fragment

interface FragmentProvider {
    fun getCurrentFragment(): Fragment?
}
