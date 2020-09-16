package ru.fasdev.ratex.domain.currencyRate

import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateRepo
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain
import ru.fasdev.ratex.domain.currencyRate.interactor.CurrencyRateInteractorImpl
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import java.util.*

public final class CurrencyRateInteractorTest
{
    private lateinit var currencyRateRepo: CurrencyRateRepo;
    private lateinit var sharedPrefencesRepo: SharedPrefencesRepo
    private lateinit var currencyRateInteractor: CurrencyRateInteractor

    @Before
    fun setUp() {
        currencyRateRepo = Mockito.mock(CurrencyRateRepo::class.java)
        sharedPrefencesRepo = Mockito.mock(SharedPrefencesRepo::class.java)
        currencyRateInteractor = CurrencyRateInteractorImpl(currencyRateRepo, sharedPrefencesRepo)
    }

    @Test
    fun getBaseCurrencyNotNull()
    {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(Currency.getInstance("USD").currencyCode)

        val result = currencyRateInteractor.getBaseCurrency()

        assertEquals(result.currencyCode, "USD")
    }

    @Test
    fun getBaseCurrencyNull()
    {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(null)
        Locale.setDefault(Locale.US)

        val result = currencyRateInteractor.getBaseCurrency()

        Mockito.verify(sharedPrefencesRepo).setBaseCurrencyCode("USD")
        assertEquals(result.currencyCode, "USD")
    }

    @Test
    fun getExchangeRates()
    {
        val testData: MutableList<RateCurrencyDomain> = arrayListOf()

        testData.add(RateCurrencyDomain("RUB", 0.534786))
        testData.add(RateCurrencyDomain("UKR", 0.563423))

        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode().toString()).thenReturn("USD")
        Mockito.`when`(currencyRateRepo.getExchangeRates("USD")).thenReturn(testData)

        val resultData = currencyRateInteractor.getExchangeRates()

        assertEquals(testData, resultData)
    }
}