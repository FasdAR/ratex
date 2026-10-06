package ru.fasdev.ratex.currency.data.source

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RateSourcePriorityTest {
    @Test
    fun testOrderIsPriority() {
        HttpClient(MockEngine { respond("") }).use { client ->
            val ids = RateSourcePriority.entries.map { it.create(client).id }

            assertThat(ids).containsExactly("ecb", "cbr", "fed", "treasury")
        }
    }
}
