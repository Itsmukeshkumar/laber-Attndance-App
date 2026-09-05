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
import kotlinx.coroutines.launch

data class ContractorDashboardStats(
    val totalLabour: Int = 0,
    val totalProjects: Int = 0,
    val todayPresent: Int = 0,
    val todayAbsent: Int = 0
)

data class ProjectWithLabourCount(
    val project: Project,
    val labourCount: Int
)

class ContractorViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val sessionManager = repository.getSessionManager()
    val contractorId: String = sessionManager.getUserId()

    // Current navigation tab inside Contractor Screen
    // 0: Home, 1: Projects, 2: Labour, 3: Reports, 4: Profile
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Screen-level navigation inside Contractor section
    // null: standard tabs; "ATTENDANCE": attendance screen; "PAYMENTS": payments screen; "REQUESTS": join requests screen
    private val _currentSubScreen = MutableStateFlow<String?>(null)
    val currentSubScreen: StateFlow<String?> = _currentSubScreen.asStateFlow()

    // Attendance selection states
    private val _selectedAttendanceDate = MutableStateFlow(DateUtils.getTodayIso())
    val selectedAttendanceDate: StateFlow<String> = _selectedAttendanceDate.asStateFlow()

    private val _selectedProjectId = MutableStateFlow<String>("")
    val selectedProjectId: StateFlow<String> = _selectedProjectId.asStateFlow()

    // Message feedback
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Reactive streams from repository
    val projects = repository.projects
    val labours = repository.labours
    val attendances = repository.attendances
    val payments = repository.payments
    val joinRequests = repository.joinRequests
    val notifications = repository.notifications

    // Auto-select first project if none selected
    init {
        viewModelScope.launch {
            projects.collect { prjList ->
                val myProjects = prjList.filter { it.contractorId == contractorId }
                if (_selectedProjectId.value.isBlank() && myProjects.isNotEmpty()) {
                    _selectedProjectId.value = myProjects.first().projectId
                }
            }
        }
    }

    // Projects with labour counts
    val projectsWithCounts: StateFlow<List<ProjectWithLabourCount>> = combine(projects, labours) { prjList, labList ->
        val myProjects = prjList.filter { it.contractorId == contractorId }
        myProjects.map { prj ->
            val count = labList.count { it.projectId == prj.projectId && it.status == LabourStatus.ACTIVE.name }
            ProjectWithLabourCount(prj, count)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard statistics
    val dashboardStats: StateFlow<ContractorDashboardStats> = combine(
        projects,
        labours,
        attendances
    ) { prjList, labList, attList ->
        val myProjects = prjList.filter { it.contractorId == contractorId }.map { it.projectId }
        val myLabours = labList.filter { it.contractorId == contractorId && it.status == LabourStatus.ACTIVE.name }

        val todayStr = DateUtils.getTodayIso()
        val todayAttendances = attList.filter { it.date == todayStr && it.projectId in myProjects }

        val presentCount = todayAttendances.count { it.status == AttendanceStatus.PRESENT.name || it.status == AttendanceStatus.HALF_DAY.name }
        val absentCount = todayAttendances.count { it.status == AttendanceStatus.ABSENT.name }

        ContractorDashboardStats(
            totalLabour = myLabours.size,
            totalProjects = myProjects.size,
            todayPresent = presentCount,
            todayAbsent = absentCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContractorDashboardStats())

    // Pending requests for this contractor
    val pendingRequests: StateFlow<List<JoinRequest>> = joinRequests.combine(MutableStateFlow(contractorId)) { reqs, cid ->
        reqs.filter { it.contractorId == cid && it.status == RequestStatus.PENDING.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
        _currentSubScreen.value = null
    }

    fun navigateToSubScreen(screen: String?) {
        _currentSubScreen.value = screen
    }

    fun setAttendanceDate(dateIso: String) {
        _selectedAttendanceDate.value = dateIso
    }

    fun selectProject(projectId: String) {
        _selectedProjectId.value = projectId
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // ==================== PROJECT ACTIONS ====================

    fun addProject(name: String, location: String, startDate: String, endDate: String) {
        val res = repository.addProject(contractorId, name, location, startDate, endDate)
        if (res.isSuccess) {
            _toastMessage.value = "Project added successfully"
            if (_selectedProjectId.value.isBlank()) {
                _selectedProjectId.value = res.getOrNull()?.projectId ?: ""
            }
        } else {
            _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to add project"
        }
    }

    fun updateProject(project: Project) {
        val res = repository.updateProject(project)
        if (res.isSuccess) {
            _toastMessage.value = "Project updated"
        } else {
            _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to update project"
        }
    }

    fun deleteProject(projectId: String) {
        val res = repository.deleteProject(projectId)
        if (res.isSuccess) {
            _toastMessage.value = "Project deleted"
            if (_selectedProjectId.value == projectId) {
                val remaining = projects.value.filter { it.contractorId == contractorId && it.projectId != projectId }
                _selectedProjectId.value = remaining.firstOrNull()?.projectId ?: ""
            }
        } else {
            _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to delete project"
        }
    }

    // ==================== LABOUR ACTIONS ====================

    fun addLabourManually(name: String, mobile: String, dailyWage: Double, projectId: String) {
        val res = repository.addLabourManually(contractorId, name, mobile, dailyWage, projectId)
        if (res.isSuccess) {
            _toastMessage.value = "Labour added successfully"
        } else {
            _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to add labour"
        }
    }

    fun updateLabour(labour: Labour) {
        val res = repository.updateLabour(labour)
        if (res.isSuccess) {
            _toastMessage.value = "Labour details updated"
        } else {
            _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to update labour"
        }
    }

    fun transferLabour(labourId: String, newProjectId: String) {
        val res = repository.transferLabour(labourId, newProjectId)
        if (res.isSuccess) {
            _toastMessage.value = "Labour transferred to new project"
        } else {
            _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to transfer labour"
        }
    }

    // ==================== REQUEST ACTIONS ====================

    fun approveRequest(requestId: String, projectId: String, dailyWage: Double) {
        val res = repository.approveJoinRequest(requestId, projectId, dailyWage)
        if (res.isSuccess) {
            _toastMessage.value = "Labour approved and added to project"
        } else {
            _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to approve request"
        }
    }

    fun rejectRequest(requestId: String) {
        val res = repository.rejectJoinRequest(requestId)
        if (res.isSuccess) {
            _toastMessage.value = "Request rejected"
        } else {
            _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to reject request"
        }
    }

    // ==================== ATTENDANCE ACTIONS ====================

    fun markAttendance(labourId: String, status: AttendanceStatus) {
        val projectId = _selectedProjectId.value
        if (projectId.isBlank()) {
            _toastMessage.value = "Please select a project first"
            return
        }
        val date = _selectedAttendanceDate.value
        val res = repository.markAttendance(contractorId, labourId, projectId, date, status)
        if (res.isSuccess) {
            _toastMessage.value = "Attendance marked: ${status.name.replace("_", " ")}"
        } else {
            _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to mark attendance"
        }
    }

    fun markAllPresent() {
        val projectId = _selectedProjectId.value
        if (projectId.isBlank()) {
            _toastMessage.value = "Please select a project first"
            return
        }
        val date = _selectedAttendanceDate.value
        val res = repository.markAllPresent(contractorId, projectId, date)
        if (res.isSuccess) {
            _toastMessage.value = "Marked ${res.getOrNull()} labours as Present"
        } else {
            _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to mark all present"
        }
    }

    // ==================== PAYMENT ACTIONS ====================

    fun addPayment(labourId: String, projectId: String, amount: Double, date: String, note: String) {
        val res = repository.addPayment(contractorId, labourId, projectId, amount, date, note)
        if (res.isSuccess) {
            _toastMessage.value = "Payment of ₹$amount recorded"
        } else {
            _toastMessage.value = res.exceptionOrNull()?.message ?: "Failed to record payment"
        }
    }

    fun getLabourWageSummary(labourId: String): LabourWageSummary {
        return repository.calculateLabourWageSummary(labourId)
    }

    fun updateProfile(name: String, companyName: String) {
        sessionManager.updateProfile(name, companyName)
        _toastMessage.value = "Profile updated"
    }

    fun logout() {
        repository.logout()
    }
}
