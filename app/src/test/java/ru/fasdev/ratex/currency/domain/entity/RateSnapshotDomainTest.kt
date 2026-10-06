package ru.fasdev.ratex.currency.domain.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RateSnapshotDomainTest {
    private val snapshot = RateSnapshotDomain(
        baseCode = "EUR",
        date = "2026-10-05",
        rates = mapOf("USD" to 2.0, "JPY" to 200.0, "GBP" to 0.5)
    )

    @Test
    fun testAvailableCodesContainsBase() {
        assertThat(snapshot.availableCodes).containsExactlyInAnyOrder("EUR", "USD", "JPY", "GBP")
    }

    @Test
    fun testCrossRatesToSourceBase() {
        assertThat(snapshot.crossRates("EUR")).isEqualTo(mapOf("USD" to 2.0, "JPY" to 200.0, "GBP" to 0.5))
    }

    @Test
    fun testCrossRatesToAnotherCurrencyGoesThroughSourceBase() {
        val result = snapshot.crossRates("USD")

        assertThat(result).containsOnlyKeys("EUR", "JPY", "GBP")
        assertThat(result.getValue("EUR")).isEqualTo(0.5)
        assertThat(result.getValue("JPY")).isEqualTo(100.0)
        assertThat(result.getValue("GBP")).isEqualTo(0.25)
    }

    @Test
    fun testCrossRatesDoesNotContainTarget() {
        assertThat(snapshot.crossRates("USD")).doesNotContainKey("USD")
        assertThat(snapshot.crossRates("EUR")).doesNotContainKey("EUR")
    }

    @Test
    fun testCrossRatesUnknownTargetIsEmpty() {
        assertThat(snapshot.crossRates("RUB")).isEmpty()
    }
}
