package ru.fasdev.ratex.data.currencyRate

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.fasdev.ratex.data.currencyRate.repo.FlagCdnRepoImpl
import java.net.URL

class FlagCdnRepoImplTest
{
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