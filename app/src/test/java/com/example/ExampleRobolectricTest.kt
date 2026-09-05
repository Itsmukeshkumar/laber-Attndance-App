package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify app name resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Labour Attendance Manager", appName)
    }

    @Test
    fun `verify session manager storage and clear`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SessionManager(context)

        sessionManager.clearSession()
        assertFalse(sessionManager.isLoggedIn())

        val testUser = UserProfile(
            userId = "user_123",
            mobile = "9876543210",
            name = "Mukesh Singh",
            role = UserRole.CONTRACTOR.name,
            referralCode = "REF-ABC123",
            companyName = "Singh Construction"
        )
        sessionManager.saveSession(testUser)

        assertTrue(sessionManager.isLoggedIn())
        assertEquals("user_123", sessionManager.getUserId())
        assertEquals("9876543210", sessionManager.getUserMobile())
        assertEquals(UserRole.CONTRACTOR.name, sessionManager.getUserRole())
        assertEquals("Mukesh Singh", sessionManager.getUserName())
        assertEquals("REF-ABC123", sessionManager.getReferralCode())
        assertEquals("Singh Construction", sessionManager.getCompanyName())

        sessionManager.clearSession()
        assertFalse(sessionManager.isLoggedIn())
        assertEquals("", sessionManager.getUserId())
    }
}
