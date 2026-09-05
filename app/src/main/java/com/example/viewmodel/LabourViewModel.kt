package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AppRepository
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LabourDashboardUiState(
    val labourName: String = "",
    val mobileNumber: String = "",
    val projectName: String = "Not Assigned",
    val contractorName: String = "Contractor",
    val dailyWage: Double = 0.0,
    val joiningDate: String = "",
    val todayStatus: String = "Not Marked",
    val presentCount: Int = 0,
    val halfDayCount: Int = 0,
    val absentCount: Int = 0,
    val totalEarned: Double = 0.0,
    val totalPaid: Double = 0.0,
    val remaining: Double = 0.0,
    val isApproved: Boolean = false,
    val isPending: Boolean = false,
    val isRejected: Boolean = false
)

class LabourViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val sessionManager = repository.getSessionManager()
    val labourId: String = sessionManager.getUserId()

    // 0: Home, 1: Attendance, 2: Payment, 3: Profile
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val attendances = repository.attendances
    val payments = repository.payments
    val labours = repository.labours
    val projects = repository.projects
    val joinRequests = repository.joinRequests
    val notifications = repository.notifications

    // Labour dashboard state
    val dashboardState: StateFlow<LabourDashboardUiState> = combine(
        labours,
        projects,
        attendances,
        payments,
        joinRequests
    ) { labList, prjList, attList, payList, reqList ->
        val currentLabour = labList.find { it.labourId == labourId }
        val joinReq = reqList.find { it.labourId == labourId }

        val isApproved = currentLabour != null && currentLabour.status == LabourStatus.ACTIVE.name
        val isPending = !isApproved && (joinReq?.status == RequestStatus.PENDING.name || joinReq == null)
        val isRejected = !isApproved && joinReq?.status == RequestStatus.REJECTED.name

        val name = currentLabour?.name ?: sessionManager.getUserName().ifBlank { "Labour" }
        val mobile = currentLabour?.mobile ?: sessionManager.getUserMobile()
        val wage = currentLabour?.dailyWage ?: 0.0
        val joiningDate = currentLabour?.joiningDate ?: ""

        val assignedProject = prjList.find { it.projectId == currentLabour?.projectId }
        val projectName = assignedProject?.name ?: "Assigning Project..."
        val contractorName = "Contractor"

        // Today status
        val todayIso = DateUtils.getTodayIso()
        val todayRecord = attList.find { it.labourId == labourId && it.date == todayIso }
        val todayStatus = when (todayRecord?.status) {
            AttendanceStatus.PRESENT.name -> "Present"
            AttendanceStatus.HALF_DAY.name -> "Half Day"
            AttendanceStatus.ABSENT.name -> "Absent"
            else -> "Not Marked"
        }

        // Attendance stats for this labour
        val labourAtts = attList.filter { it.labourId == labourId }
        val presentCount = labourAtts.count { it.status == AttendanceStatus.PRESENT.name }
        val halfDayCount = labourAtts.count { it.status == AttendanceStatus.HALF_DAY.name }
        val absentCount = labourAtts.count { it.status == AttendanceStatus.ABSENT.name }

        val workingDays = presentCount * 1.0 + halfDayCount * 0.5
        val totalEarned = workingDays * wage

        val labourPays = payList.filter { it.labourId == labourId }
        val totalPaid = labourPays.sumOf { it.amount }
        val remaining = totalEarned - totalPaid

        LabourDashboardUiState(
            labourName = name,
            mobileNumber = mobile,
            projectName = projectName,
            contractorName = contractorName,
            dailyWage = wage,
            joiningDate = joiningDate,
            todayStatus = todayStatus,
            presentCount = presentCount,
            halfDayCount = halfDayCount,
            absentCount = absentCount,
            totalEarned = totalEarned,
            totalPaid = totalPaid,
            remaining = remaining,
            isApproved = isApproved,
            isPending = isPending,
            isRejected = isRejected
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LabourDashboardUiState())

    // Labour attendance history sorted descending
    val myAttendanceHistory: StateFlow<List<AttendanceRecord>> = combine(attendances, MutableStateFlow(labourId)) { atts, lid ->
        atts.filter { it.labourId == lid }.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Labour payment history
    val myPaymentHistory: StateFlow<List<PaymentRecord>> = combine(payments, MutableStateFlow(labourId)) { pays, lid ->
        pays.filter { it.labourId == lid }.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications for this labour
    val myNotifications: StateFlow<List<AppNotification>> = combine(notifications, MutableStateFlow(labourId)) { notifs, lid ->
        notifs.filter { it.userId == lid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun logout() {
        repository.logout()
    }
}
