package ru.fasdev.ratex.data.currencyRate

import org.assertj.core.api.Assertions.assertThat
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.URL
import java.util.concurrent.locks.Condition

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