package ru.fasdev.ratex.currency.data.repo

import java.util.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import ru.fasdev.ratex.core.domain.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

class CurrencyBaseRepoTest {
    @get:Rule val mockitoJUnit = MockitoJUnit.rule()

    @Mock lateinit var sharedPrefencesRepo: SharedPrefencesRepo

    @Mock lateinit var currencyImageRepo: CurrencyImageRepo

    @Mock lateinit var currencyRateRepo: CurrencyRateRepo

    lateinit var currencyBaseRepo: CurrencyBaseRepo

    private val defaultLocale = Locale.getDefault()
    private val snapshot = RateSnapshotDomain("EUR", "2026-10-05", mapOf("USD" to 1.1, "JPY" to 160.0, "ZZZ" to 3.0))

    @Before
    fun setUp() = runTest {
        Mockito.`when`(currencyRateRepo.getSnapshot()).thenReturn(snapshot)
        currencyBaseRepo = CurrencyBaseRepoImpl(sharedPrefencesRepo, currencyImageRepo, currencyRateRepo, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun testGetBaseCurrencyNullPreferencesUsesLocale() = runTest {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(null)
        Locale.setDefault(Locale.US)

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo("USD")
    }

    @Test
    fun testGetBaseCurrencyFromPreferences() = runTest {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn("JPY")

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo("JPY")
    }

    @Test
    fun testGetBaseCurrencySourceBaseIsAvailable() = runTest {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn("EUR")

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo("EUR")
    }

    @Test
    fun testGetBaseCurrencyFromPreferencesMissingInSourceFallsBackToSourceBase() = runTest {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn("RUB")

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo("EUR")
        Mockito.verify(sharedPrefencesRepo, Mockito.never()).setBaseCurrencyCode(Mockito.anyString())
    }

    @Test
    fun testGetBaseCurrencyLocaleMissingInSourceFallsBackToSourceBase() = runTest {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(null)
        Locale.setDefault(Locale("ru", "RU"))

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo("EUR")
    }

    @Test
    fun testGetBaseCurrencyLocaleWithoutCurrencyFallsBackToSourceBase() = runTest {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(null)
        Locale.setDefault(Locale.ROOT)

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo("EUR")
    }

    @Test
    fun testSetBaseCurrency() {
        val testCurrencyCode = "JPY"
        currencyBaseRepo.setBaseCurrency(CurrencyDomain.getInstance(testCurrencyCode))

        Mockito.verify(sharedPrefencesRepo).setBaseCurrencyCode(testCurrencyCode)
    }

    @Test
    fun testGetAvailableCurrenciesComeFromSnapshotWithoutUnknownCodes() = runTest {
        val result = currencyBaseRepo.getAvailableCurrencies()

        assertThat(result.map { it.currencyCode }).containsExactlyInAnyOrder("EUR", "USD", "JPY")
    }
}
