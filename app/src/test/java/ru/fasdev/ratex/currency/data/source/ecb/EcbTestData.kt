package ru.fasdev.ratex.currency.data.source.ecb

object EcbTestData {
    val XML_DAILY: String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gesmes:Envelope xmlns:gesmes="http://www.gesmes.org/xml/2002-08-01" xmlns="http://www.ecb.int/vocabulary/2002-08-01/eurofxref">
            <gesmes:subject>Reference rates</gesmes:subject>
            <gesmes:Sender>
                <gesmes:name>European Central Bank</gesmes:name>
            </gesmes:Sender>
            <Cube>
                <Cube time='2026-10-05'>
                    <Cube currency='USD' rate='1.0850'/>
                    <Cube currency='JPY' rate='162.40'/>
                    <Cube currency='GBP' rate='0.8412'/>
                    <Cube currency='PLN' rate='4.2791'/>
                </Cube>
            </Cube>
        </gesmes:Envelope>
    """.trimIndent()

    val XML_WITH_BAD_RATES: String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gesmes:Envelope xmlns:gesmes="http://www.gesmes.org/xml/2002-08-01" xmlns="http://www.ecb.int/vocabulary/2002-08-01/eurofxref">
            <Cube>
                <Cube time='2026-10-05'>
                    <Cube currency='USD' rate='1.0850'/>
                    <Cube currency='JPY' rate='0'/>
                    <Cube currency='GBP' rate='-0.84'/>
                    <Cube currency='PLN' rate='abc'/>
                    <Cube currency='CHF' rate='NaN'/>
                </Cube>
            </Cube>
        </gesmes:Envelope>
    """.trimIndent()

    val XML_NO_RATES: String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gesmes:Envelope xmlns:gesmes="http://www.gesmes.org/xml/2002-08-01" xmlns="http://www.ecb.int/vocabulary/2002-08-01/eurofxref">
            <Cube>
                <Cube time='2026-10-05'/>
            </Cube>
        </gesmes:Envelope>
    """.trimIndent()

    const val HTML_ERROR_PAGE: String = "<html><body><h1>503 Service Unavailable</h1></body></html>"
    const val NOT_XML: String = "this is not xml at all <<<"
}
