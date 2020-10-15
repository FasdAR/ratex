package ru.fasdev.ratex

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Test
import ru.fasdev.ratex.ui.view.activityMain.MainActivity

@RunWith(AndroidJUnit4::class)
class TestTest
{
    @get:Rule public val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun checkFragmentContainerIsDisplayed() {
        onView(ViewMatchers.withId(R.id.main_container)).check(matches(isDisplayed()))
    }
}