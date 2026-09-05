package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserProfile
import com.example.data.model.UserRole

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("labour_mgr_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_MOBILE = "user_mobile"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_REFERRAL_CODE = "referral_code"
        private const val KEY_COMPANY_NAME = "company_name"
        private const val KEY_FIRST_LAUNCH = "first_launch_done"
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUserId(): String = prefs.getString(KEY_USER_ID, "") ?: ""
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun getUserMobile(): String = prefs.getString(KEY_USER_MOBILE, "") ?: ""
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "") ?: ""
    fun getReferralCode(): String = prefs.getString(KEY_REFERRAL_CODE, "") ?: ""
    fun getCompanyName(): String = prefs.getString(KEY_COMPANY_NAME, "") ?: ""

    fun isFirstLaunch(): Boolean = !prefs.getBoolean(KEY_FIRST_LAUNCH, false)

    fun setFirstLaunchDone() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
    }

    fun saveSession(user: UserProfile) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, user.userId)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_MOBILE, user.mobile)
            .putString(KEY_USER_ROLE, user.role)
            .putString(KEY_REFERRAL_CODE, user.referralCode)
            .putString(KEY_COMPANY_NAME, user.companyName)
            .apply()
    }

    fun updateProfile(name: String, companyName: String) {
        prefs.edit()
            .putString(KEY_USER_NAME, name)
            .putString(KEY_COMPANY_NAME, companyName)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .putString(KEY_USER_ID, "")
            .putString(KEY_USER_NAME, "")
            .putString(KEY_USER_MOBILE, "")
            .putString(KEY_USER_ROLE, "")
            .putString(KEY_REFERRAL_CODE, "")
            .putString(KEY_COMPANY_NAME, "")
            .apply()
    }
}
