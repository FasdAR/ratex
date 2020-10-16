package ru.fasdev.ratex.ui.view.activityMain

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import ru.fasdev.ratex.R

class MainActivityTest {
    @get:Rule
    var activeRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testLaunchMainActivity()
    {
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLaunchRootFragment()
    {
        onView(withId(R.id.fragment_list_currency_rate))
            .check(matches(isDisplayed()))
    }
}