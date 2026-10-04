package ru.fasdev.ratex.main.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import ru.fasdev.ratex.currency.ui.listCurrencyRate.TAG_BASE_CURRENCY
import ru.fasdev.ratex.currency.ui.listCurrencyRate.TAG_LIST_CURRENCY_RATE
import ru.fasdev.ratex.currency.ui.selectCurrency.TAG_SELECT_CURRENCY_SHEET

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testLaunchRootScreen() {
        composeRule.onNodeWithTag(TAG_LIST_CURRENCY_RATE).assertIsDisplayed()
    }

    @Test
    fun testOpenSelectCurrencySheet() {
        composeRule.onNodeWithTag(TAG_BASE_CURRENCY).performClick()

        composeRule.onNodeWithTag(TAG_SELECT_CURRENCY_SHEET).assertIsDisplayed()
    }
}
