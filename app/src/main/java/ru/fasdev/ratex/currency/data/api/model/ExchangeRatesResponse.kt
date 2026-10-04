package ru.fasdev.ratex.currency.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class ExchangeRatesResponse(val rates: Map<String, Double>, val base: String? = null, val date: String? = null)
