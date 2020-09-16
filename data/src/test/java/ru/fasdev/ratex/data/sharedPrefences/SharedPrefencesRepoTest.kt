package ru.fasdev.ratex.data.sharedPrefences

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo

@RunWith(RobolectricTestRunner::class)
class SharedPrefencesRepoTest
{
    lateinit var context: Context
    lateinit var sharedPrefencesRepo: SharedPrefencesRepo

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        val sPrefences = context.getSharedPreferences(SPrefences.NAME_SETTINGS, Context.MODE_PRIVATE)
        sharedPrefencesRepo = SharedPrefencesRepoImpl(SPrefences(sPrefences));
    }

    @Test
    fun testBaseCurrencyCode()
    {
        val value = "USD"

        sharedPrefencesRepo.setBaseCurrencyCode(value)
        val result = sharedPrefencesRepo.getBaseCurrencyCode()

        assertEquals(value, result)
    }
}