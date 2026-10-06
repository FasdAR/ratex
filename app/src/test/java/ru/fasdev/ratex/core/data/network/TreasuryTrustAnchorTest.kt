package ru.fasdev.ratex.core.data.network

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.fasdev.ratex.R

/**
 * Промежуточный сертификат Entrust для api.fiscaldata.treasury.gov лежит в res/raw и подключён через network_security_config.
 * Сертификат действует до 2027-12-10: после этой даты тест упадёт, и файл нужно обновить (см. комментарий в network_security_config.xml).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class TreasuryTrustAnchorTest {
    private fun loadCertificate(): X509Certificate {
        val context: Context = ApplicationProvider.getApplicationContext()
        return context.resources.openRawResource(R.raw.entrust_ov_rsa_ca2).use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }
    }

    @Test
    fun testTrustAnchorIsEntrustIntermediateCa() {
        val certificate = loadCertificate()

        assertThat(certificate.subjectX500Principal.name).contains("Entrust OV TLS Issuing RSA CA 2")
        assertThat(certificate.basicConstraints).isGreaterThanOrEqualTo(0)
    }

    @Test
    fun testTrustAnchorIsNotExpired() {
        loadCertificate().checkValidity()
    }
}
