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
        assertEquals("Nexus Tunnel", appName)
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
}
