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

/** Промежуточный сертификат Sectigo для ecb.europa.eu лежит в res/raw и подключён через network_security_config. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class EcbTrustAnchorTest {
    private fun loadCertificate(): X509Certificate {
        val context: Context = ApplicationProvider.getApplicationContext()
        return context.resources.openRawResource(R.raw.sectigo_ov_e36).use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }
    }

    @Test
    fun testTrustAnchorIsSectigoIntermediateCa() {
        val certificate = loadCertificate()

        assertThat(certificate.subjectX500Principal.name).contains("Sectigo Public Server Authentication CA OV E36")
        assertThat(certificate.basicConstraints).isGreaterThanOrEqualTo(0)
    }

    @Test
    fun testTrustAnchorIsNotExpired() {
        loadCertificate().checkValidity()
    }
}
