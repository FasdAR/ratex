package ru.fasdev.ratex.data.currencyRate

import android.webkit.URLUtil
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
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

        assertTrue(url.endsWith("ru.jpg"))
        assertTrue(URL(url).toURI() != null)
    }
}