package ru.fasdev.ratex.currency.data.source.cbr

object CbrTestData {
    val XML_DAILY: String = """
        <?xml version="1.0" encoding="windows-1251"?>
        <ValCurs Date="05.10.2026" name="Foreign Currency Market">
            <Valute ID="R01235">
                <NumCode>840</NumCode>
                <CharCode>USD</CharCode>
                <Nominal>1</Nominal>
                <Name>Доллар США</Name>
                <Value>92,5000</Value>
                <VunitRate>92,5</VunitRate>
            </Valute>
            <Valute ID="R01820">
                <NumCode>392</NumCode>
                <CharCode>JPY</CharCode>
                <Nominal>100</Nominal>
                <Name>Японских иен</Name>
                <Value>61,2000</Value>
                <VunitRate>0,612</VunitRate>
            </Valute>
            <Valute ID="R01335">
                <NumCode>398</NumCode>
                <CharCode>KZT</CharCode>
                <Nominal>100</Nominal>
                <Name>Казахстанских тенге</Name>
                <Value>18,9000</Value>
                <VunitRate>0,189</VunitRate>
            </Valute>
            <Valute ID="R01100">
                <NumCode>975</NumCode>
                <CharCode>BGN</CharCode>
                <Nominal>1</Nominal>
                <Name>Болгарский лев</Name>
                <Value>50,0000</Value>
                <VunitRate>50</VunitRate>
            </Valute>
        </ValCurs>
    """.trimIndent()

    val XML_WITH_BAD_VALUES: String = """
        <?xml version="1.0" encoding="windows-1251"?>
        <ValCurs Date="05.10.2026" name="Foreign Currency Market">
            <Valute><CharCode>USD</CharCode><Nominal>1</Nominal><Value>92,5000</Value></Valute>
            <Valute><CharCode>JPY</CharCode><Nominal>0</Nominal><Value>61,2000</Value></Valute>
            <Valute><CharCode>KZT</CharCode><Nominal>100</Nominal><Value>0</Value></Valute>
            <Valute><CharCode>BGN</CharCode><Nominal>1</Nominal><Value>abc</Value></Valute>
            <Valute><CharCode>CNY</CharCode><Nominal>x</Nominal><Value>12,5</Value></Valute>
            <Valute><Nominal>1</Nominal><Value>10,0</Value></Valute>
        </ValCurs>
    """.trimIndent()

    val XML_NO_RATES: String = """
        <?xml version="1.0" encoding="windows-1251"?>
        <ValCurs Date="05.10.2026" name="Foreign Currency Market"></ValCurs>
    """.trimIndent()

    val XML_BAD_DATE: String = """
        <?xml version="1.0" encoding="windows-1251"?>
        <ValCurs Date="yesterday" name="Foreign Currency Market">
            <Valute><CharCode>USD</CharCode><Nominal>1</Nominal><Value>92,5000</Value></Valute>
        </ValCurs>
    """.trimIndent()

    const val HTML_ERROR_PAGE: String = "<html><body><h1>503 Service Unavailable</h1></body></html>"
}
