package ru.fasdev.ratex.currency.data.repo

import java.net.URL
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.fasdev.ratex.currency.data.repo.FlagCdnRepoImpl

class FlagCdnRepoTest {
    private lateinit var flagCdnRepoImpl: FlagCdnRepoImpl

    @Before
    fun setUp() {
        flagCdnRepoImpl = FlagCdnRepoImpl()
    }

    @Test
    fun testGetImageUrl() {
        val url = flagCdnRepoImpl.getImageUrl("RUB")

        assertThat(url)
            .endsWith("ru.jpg")

        assertThat(URL(url).toURI())
            .isNotNull()
    }
}
