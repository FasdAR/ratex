package ru.fasdev.ratex.currency.domain.entity

/** Снимок курсов одного источника: «1 [baseCode] = rates[X] единиц X». Самой базы в [rates] нет. */
data class RateSnapshotDomain(val baseCode: String, val date: String, val rates: Map<String, Double>) {
    val availableCodes: Set<String>
        get() = rates.keys + baseCode

    /**
     * Курсы всех валют снимка к [targetCode]: «1 [targetCode] = N единиц X».
     * Считается через базу источника: rate(target→X) = rates[X] / rates[target], где rates[base] = 1.
     * Если [targetCode] нет в снимке — пустая карта.
     */
    fun crossRates(targetCode: String): Map<String, Double> {
        val withBase = rates + (baseCode to 1.0)
        val targetRate = withBase[targetCode] ?: return emptyMap()

        return withBase
            .filterKeys { it != targetCode }
            .mapValues { it.value / targetRate }
    }
}
