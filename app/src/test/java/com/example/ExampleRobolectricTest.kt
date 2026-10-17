package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AfricanCountries
import com.example.data.model.ApprovalStatus
import com.example.data.repository.NexusRepository
import com.example.data.security.SecurityStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nexus Tunnel VPN", appName)
    }

    @Test
    fun `african countries list is populated and valid`() {
        assertTrue(AfricanCountries.list.isNotEmpty())
        val cameroon = AfricanCountries.list.find { it.code == "CM" }
        assertNotNull(cameroon)
        assertEquals("Cameroun", cameroon?.name)
        assertEquals("CM", cameroon?.code)
    }

    @Test
    fun `security storage encrypts and decrypts correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val secStorage = SecurityStorage(context)

        val secretKey = "vless_uuid"
        val secretValue = "e7492c3a-810a-4a25-9c59-bf75a409f582"

        secStorage.putEncrypted(secretKey, secretValue)
        val decrypted = secStorage.getDecrypted(secretKey)
        assertEquals(secretValue, decrypted)

        secStorage.remove(secretKey)
        assertNull(secStorage.getDecrypted(secretKey))
    }

    @Test
    fun `nexus repository activate token and device info work`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = NexusRepository(context)

        val devInfo = repo.getDeviceInfo()
        assertNotNull(devInfo.deviceId)
        assertTrue(devInfo.deviceId.startsWith("NX-"))

        val token = "NEXUS-VLESS-SECURE-TEST"
        val result = repo.activateToken(token)
        assertTrue(result.isSuccess)

        val session = result.getOrNull()
        assertNotNull(session)
        assertEquals(ApprovalStatus.APPROVED, session?.approvalStatus)
        assertEquals(token, session?.token)
        assertTrue((session?.quotaTotalBytes ?: 0) > 0)
    }

    @Test
    fun `generateXrayJsonConfig produces valid Xray core json`() {
        val config = com.example.data.model.DynamicServerConfig(
            protocol = "vless",
            serverAddress = "192.179.18.100",
            serverPort = 443,
            uuid = "83d5a4b2-a45b-48c1-9012-3456789abcde",
            wsPath = "/nexustunnel",
            sni = "crashlyticsreports-pa.googleapis.com",
            headerHost = "nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio",
            enableTls = true,
            enableMux = true
        )
        val jsonString = com.example.vpn.VlessConfigParser.generateXrayJsonConfig(config)
        assertNotNull(jsonString)
        val json = org.json.JSONObject(jsonString)

        assertTrue(json.has("inbounds"))
        assertTrue(json.has("outbounds"))

        val inbounds = json.getJSONArray("inbounds")
        assertEquals(2, inbounds.length())
        assertEquals("socks", inbounds.getJSONObject(0).getString("protocol"))
        assertEquals(10808, inbounds.getJSONObject(0).getInt("port"))
        assertEquals("http", inbounds.getJSONObject(1).getString("protocol"))
        assertEquals(10809, inbounds.getJSONObject(1).getInt("port"))

        val outbounds = json.getJSONArray("outbounds")
        val vlessOut = outbounds.getJSONObject(0)
        assertEquals("vless", vlessOut.getString("protocol"))
        val stream = vlessOut.getJSONObject("streamSettings")
        assertEquals("ws", stream.getString("network"))
        assertEquals("tls", stream.getString("security"))
        assertEquals("/nexustunnel", stream.getJSONObject("wsSettings").getString("path"))
        assertEquals(
            "nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio",
            stream.getJSONObject("wsSettings").getJSONObject("headers").getString("Host")
        )
        assertEquals(
            "crashlyticsreports-pa.googleapis.com",
            stream.getJSONObject("tlsSettings").getString("serverName")
        )
        assertTrue(vlessOut.getJSONObject("mux").getBoolean("enabled"))
    }
}

