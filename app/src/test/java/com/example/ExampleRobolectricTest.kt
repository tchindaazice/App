package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AfricanCountries
import com.example.data.model.ApprovalStatus
import com.example.data.model.CommunicationChannel
import com.example.data.model.RegistrationRequest
import com.example.data.repository.NexusRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertEquals("🇨🇲", cameroon?.flagEmoji)
    }

    @Test
    fun `nexus repository device info and approval simulation work`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = NexusRepository(context)

        val devInfo = repo.getDeviceInfo()
        assertNotNull(devInfo.deviceId)
        assertTrue(devInfo.deviceId.startsWith("NX-"))

        val session = repo.simulateAdminApprovalForTesting()
        assertEquals(ApprovalStatus.APPROVED, session.approvalStatus)
        assertNotNull(session.uid)
        assertNotNull(session.token)
        assertTrue(session.quotaTotalBytes > 0)
    }
}
