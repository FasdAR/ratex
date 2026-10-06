package ru.fasdev.ratex.currency.data.source.fed

/** Урезанный ответ DDP H.10 (пакет валют): настоящие строки заголовка и значения за 2026-09-30 … 2026-10-02. */
object FedTestData {
    private const val HEADER = """
"Series Description","Australian Dollar","Euro-Area Euro","Japanese Yen","Venezuelan Bolivar"
"Unit:","Currency","Currency","Currency","Currency"
"Multiplier:","1","1","1","1"
"Currency:","AUD","EUR","JPY","VEB"
"Unique Identifier:","H10/H10/RXI${'$'}US_N.B.AL","H10/H10/RXI${'$'}US_N.B.EU","H10/H10/RXI_N.B.JA","H10/H10/RXI_N.B.VES"
"Time Period","RXI${'$'}US_N.B.AL","RXI${'$'}US_N.B.EU","RXI_N.B.JA","RXI_N.B.VES"
"""

    val CSV_DAILY: String = HEADER.trimStart() + """
2026-09-30,0.6952,1.1345,157.2500,856.9152
2026-10-01,0.6911,1.1232,157.6300,858.0249
2026-10-02,0.6953,1.1259,157.8100,864.3948
"""

    /** Последний день — праздник: везде `ND`. */
    val CSV_LAST_ROW_ND: String = HEADER.trimStart() + """
2026-09-30,0.6952,1.1345,157.2500,856.9152
2026-10-01,0.6911,1.1232,157.6300,858.0249
2026-10-02,ND,ND,ND,ND
"""

    /** По иене на последнюю дату пропуск, по остальным курс есть. */
    val CSV_PARTIAL_ND: String = HEADER.trimStart() + """
2026-10-01,0.6911,1.1232,157.6300,858.0249
2026-10-02,0.6953,1.1259,ND,864.3948
"""

    val CSV_WITH_BAD_VALUES: String = HEADER.trimStart() + """
2026-10-02,0,abc,157.8100,-1
"""

    val CSV_ONLY_ND: String = HEADER.trimStart() + """
2026-10-02,ND,ND,ND,ND
"""

    /** В длинной выгрузке (`lastobs=500`) подпись строки записана с пробелом внутри кавычек: `"Unique Identifier: "`. */
    val CSV_LABEL_WITH_SPACE: String = CSV_DAILY.replace("\"Unique Identifier:\"", "\"Unique Identifier: \"")

    val CSV_NO_CURRENCY_ROW: String = """
"Series Description","Nominal Broad Dollar Index"
"Unit:","Index:_1997_Jan_100"
2026-10-02,120.3300
"""

    const val HTML_ERROR_PAGE: String = "<html><body><h1>503 Service Unavailable</h1></body></html>"
}
