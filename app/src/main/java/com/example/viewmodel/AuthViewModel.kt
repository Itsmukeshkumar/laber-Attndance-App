package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val mobileNumber: String = "",
    val otp: String = "",
    val isOtpSent: Boolean = false,
    val resendTimer: Int = 30,
    val canResend: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedRole: UserRole? = null,
    val contractorName: String = "",
    val companyName: String = "",
    val referralCodeInput: String = "",
    val labourName: String = "",
    val generatedReferralCode: String = "",
    val currentUser: UserProfile? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application.applicationContext)
    private val sessionManager = repository.getSessionManager()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun getRepository(): AppRepository = repository

    fun onMobileChange(number: String) {
        // Only allow digits up to 10 chars
        val filtered = number.filter { it.isDigit() }.take(10)
        _uiState.value = _uiState.value.copy(mobileNumber = filtered, errorMessage = null)
    }

    fun onOtpChange(otp: String) {
        val filtered = otp.filter { it.isDigit() }.take(6)
        _uiState.value = _uiState.value.copy(otp = filtered, errorMessage = null)
    }

    fun sendOtp() {
        val mobile = _uiState.value.mobileNumber
        if (mobile.length != 10) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid 10-digit mobile number")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            delay(600) // Realistic network sensation
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isOtpSent = true,
                resendTimer = 30,
                canResend = false,
                otp = "123456" // Auto-fill demo OTP for effortless user testing
            )
            startResendTimer()
        }
    }

    fun resendOtp() {
        if (!_uiState.value.canResend) return
        startResendTimer()
    }

    private fun startResendTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(resendTimer = 30, canResend = false)
            for (sec in 29 downTo 0) {
                delay(1000)
                _uiState.value = _uiState.value.copy(resendTimer = sec)
            }
            _uiState.value = _uiState.value.copy(canResend = true)
        }
    }

    fun changeMobileNumber() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isOtpSent = false,
            otp = "",
            errorMessage = null
        )
    }

    fun verifyOtp(onExistingUser: (UserProfile) -> Unit, onNewUser: () -> Unit) {
        val otp = _uiState.value.otp
        if (otp.length != 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter the 6-digit OTP")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            delay(400)
            val result = repository.verifyOtp(_uiState.value.mobileNumber, otp)
            if (result.isSuccess) {
                val existingUser = repository.getUserByMobile(_uiState.value.mobileNumber)
                _uiState.value = _uiState.value.copy(isLoading = false, currentUser = existingUser)
                if (existingUser != null && existingUser.role.isNotBlank()) {
                    sessionManager.saveSession(existingUser)
                    onExistingUser(existingUser)
                } else {
                    onNewUser()
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Invalid OTP"
                )
            }
        }
    }

    fun selectRole(role: UserRole) {
        _uiState.value = _uiState.value.copy(selectedRole = role, errorMessage = null)
    }

    fun onContractorNameChange(name: String) {
        _uiState.value = _uiState.value.copy(contractorName = name, errorMessage = null)
    }

    fun onCompanyNameChange(name: String) {
        _uiState.value = _uiState.value.copy(companyName = name)
    }

    fun onLabourNameChange(name: String) {
        _uiState.value = _uiState.value.copy(labourName = name, errorMessage = null)
    }

    fun onReferralCodeChange(code: String) {
        _uiState.value = _uiState.value.copy(referralCodeInput = code.uppercase(), errorMessage = null)
    }

    fun registerContractor(onSuccess: (UserProfile) -> Unit) {
        val name = _uiState.value.contractorName.trim()
        val company = _uiState.value.companyName.trim()
        val mobile = _uiState.value.mobileNumber.trim()

        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Contractor Name is required")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.registerContractor(name, mobile, company)
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    generatedReferralCode = user.referralCode
                )
                onSuccess(user)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun registerLabour(onSuccess: (UserProfile) -> Unit) {
        val name = _uiState.value.labourName.trim()
        val code = _uiState.value.referralCodeInput.trim()
        val mobile = _uiState.value.mobileNumber.trim()

        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Labour Name is required")
            return
        }
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Contractor Referral Code is required")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.registerLabour(name, mobile, code)
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                _uiState.value = _uiState.value.copy(currentUser = user)
                onSuccess(user)
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to submit join request"
                )
            }
        }
    }

    fun checkAutoLogin(): UserProfile? {
        if (sessionManager.isLoggedIn()) {
            val id = sessionManager.getUserId()
            val role = sessionManager.getUserRole()
            val name = sessionManager.getUserName()
            val mobile = sessionManager.getUserMobile()
            val referralCode = sessionManager.getReferralCode()
            val companyName = sessionManager.getCompanyName()

            if (id.isNotBlank() && role.isNotBlank()) {
                val user = UserProfile(
                    userId = id,
                    name = name,
                    mobile = mobile,
                    role = role,
                    referralCode = referralCode,
                    companyName = companyName
                )
                _uiState.value = _uiState.value.copy(currentUser = user)
                return user
            }
        }
        return null
    }

    fun logout(onLoggedOut: () -> Unit) {
        repository.logout()
        _uiState.value = AuthUiState()
        onLoggedOut()
    }
}
