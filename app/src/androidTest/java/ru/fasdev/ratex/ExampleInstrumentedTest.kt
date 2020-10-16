package ru.fasdev.ratex

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule
import ru.fasdev.ratex.ui.view.activityMain.MainActivity

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testLaunchListCurrencyRateFragment() {
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testClickSelectBaseCurrency() {
        onView(withId(R.id.layout_base_currency))
            .perform(click())

        onView(withId(R.id.currency_bottom_sheet))
            .check(matches(isDisplayed()))
    }
}