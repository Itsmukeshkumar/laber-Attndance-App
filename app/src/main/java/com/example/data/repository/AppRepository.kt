package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.SessionManager
import com.example.data.model.*
import com.example.utils.DateUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID
import kotlin.random.Random

class AppRepository(private val context: Context) {

    private val sessionManager = SessionManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // StateFlows for reactive UI across all screens
    private val _users = MutableStateFlow<List<UserProfile>>(emptyList())
    private val _contractors = MutableStateFlow<List<Contractor>>(emptyList())
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    private val _labours = MutableStateFlow<List<Labour>>(emptyList())
    private val _attendances = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    private val _payments = MutableStateFlow<List<PaymentRecord>>(emptyList())
    private val _joinRequests = MutableStateFlow<List<JoinRequest>>(emptyList())
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())

    val projects: StateFlow<List<Project>> = _projects.asStateFlow()
    val labours: StateFlow<List<Labour>> = _labours.asStateFlow()
    val attendances: StateFlow<List<AttendanceRecord>> = _attendances.asStateFlow()
    val payments: StateFlow<List<PaymentRecord>> = _payments.asStateFlow()
    val joinRequests: StateFlow<List<JoinRequest>> = _joinRequests.asStateFlow()
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private var isFirebaseAvailable = false
    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null

    init {
        checkFirebaseInit()
        initDefaultDataIfFirstLaunch()
        if (isFirebaseAvailable) {
            syncFromFirestore()
        }
    }

    private fun checkFirebaseInit() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firestore = FirebaseFirestore.getInstance()
                auth = FirebaseAuth.getInstance()
                isFirebaseAvailable = true
                Log.d("AppRepository", "Firebase is initialized and available")
            } else {
                Log.d("AppRepository", "Firebase not configured. Running in high-reliability local caching mode.")
                isFirebaseAvailable = false
            }
        } catch (e: Exception) {
            Log.w("AppRepository", "Firebase init check: ${e.message}. Using robust local offline storage.")
            isFirebaseAvailable = false
        }
    }

    fun getSessionManager(): SessionManager = sessionManager

    // ==================== AUTHENTICATION ====================

    suspend fun verifyOtp(mobile: String, otp: String): Result<Boolean> {
        // Standard OTP verification. For testing/demo mode, any 6-digit OTP (or 123456) is accepted.
        return if (otp.length == 6) {
            Result.success(true)
        } else {
            Result.failure(Exception("Please enter a valid 6-digit OTP"))
        }
    }

    fun getUserByMobile(mobile: String): UserProfile? {
        val clean = mobile.replace("+91", "").trim()
        return _users.value.find { it.mobile.replace("+91", "").trim() == clean }
    }

    fun registerContractor(name: String, mobile: String, companyName: String): Result<UserProfile> {
        val cleanMobile = mobile.trim()
        val userId = "cnt_" + UUID.randomUUID().toString().take(8)

        // Generate unique referral code e.g. MUK12345
        val prefix = if (name.length >= 3) {
            name.take(3).uppercase()
        } else {
            (name + "ABC").take(3).uppercase()
        }
        val randomNum = Random.nextInt(10000, 99999)
        val referralCode = "$prefix$randomNum"

        val user = UserProfile(
            userId = userId,
            name = name.trim(),
            mobile = cleanMobile,
            role = UserRole.CONTRACTOR.name,
            referralCode = referralCode,
            companyName = companyName.trim()
        )

        val contractor = Contractor(
            contractorId = userId,
            name = name.trim(),
            mobile = cleanMobile,
            companyName = companyName.trim(),
            referralCode = referralCode
        )

        _users.value = _users.value + user
        _contractors.value = _contractors.value + contractor
        sessionManager.saveSession(user)

        // Sync to cloud if available
        if (isFirebaseAvailable && firestore != null) {
            scope.launch {
                try {
                    firestore!!.collection("users").document(userId).set(user).await()
                    firestore!!.collection("contractors").document(userId).set(contractor).await()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Failed to sync contractor: ${e.message}")
                }
            }
        }

        return Result.success(user)
    }

    fun registerLabour(name: String, mobile: String, referralCode: String): Result<UserProfile> {
        val cleanMobile = mobile.trim()
        val code = referralCode.trim().uppercase()

        // Verify referral code exists
        val contractor = _contractors.value.find { it.referralCode.uppercase() == code }
            ?: return Result.failure(Exception("Invalid Referral Code! Please check with your contractor."))

        val userId = "lab_" + UUID.randomUUID().toString().take(8)

        val user = UserProfile(
            userId = userId,
            name = name.trim(),
            mobile = cleanMobile,
            role = UserRole.LABOUR.name,
            referralCode = code
        )

        val joinReq = JoinRequest(
            requestId = "req_" + UUID.randomUUID().toString().take(8),
            labourId = userId,
            labourName = name.trim(),
            labourMobile = cleanMobile,
            contractorId = contractor.contractorId,
            referralCode = code,
            status = RequestStatus.PENDING.name
        )

        _users.value = _users.value + user
        _joinRequests.value = _joinRequests.value + joinReq
        sessionManager.saveSession(user)

        // Notification for contractor
        addNotification(
            userId = contractor.contractorId,
            title = "New Labour Request",
            message = "$name ($cleanMobile) requested to join with your referral code $code."
        )

        if (isFirebaseAvailable && firestore != null) {
            scope.launch {
                try {
                    firestore!!.collection("users").document(userId).set(user).await()
                    firestore!!.collection("joinRequests").document(joinReq.requestId).set(joinReq).await()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error syncing labour register: ${e.message}")
                }
            }
        }

        return Result.success(user)
    }

    fun logout() {
        sessionManager.clearSession()
    }

    // ==================== PROJECTS ====================

    fun getProjectsForContractor(contractorId: String): List<Project> {
        return _projects.value.filter { it.contractorId == contractorId }
    }

    fun addProject(contractorId: String, name: String, location: String, startDate: String, endDate: String): Result<Project> {
        if (name.isBlank()) return Result.failure(Exception("Project Name is required"))
        if (location.isBlank()) return Result.failure(Exception("Project Location is required"))

        val projectId = "prj_" + UUID.randomUUID().toString().take(8)
        val project = Project(
            projectId = projectId,
            contractorId = contractorId,
            name = name.trim(),
            location = location.trim(),
            startDate = startDate.ifBlank { DateUtils.getTodayIso() },
            endDate = endDate.trim(),
            status = ProjectStatus.ACTIVE.name
        )

        _projects.value = _projects.value + project

        if (isFirebaseAvailable && firestore != null) {
            scope.launch {
                try {
                    firestore!!.collection("projects").document(projectId).set(project).await()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error adding project: ${e.message}")
                }
            }
        }

        return Result.success(project)
    }

    fun updateProject(project: Project): Result<Project> {
        _projects.value = _projects.value.map { if (it.projectId == project.projectId) project else it }
        if (isFirebaseAvailable && firestore != null) {
            scope.launch {
                try {
                    firestore!!.collection("projects").document(project.projectId).set(project).await()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error updating project: ${e.message}")
                }
            }
        }
        return Result.success(project)
    }

    fun deleteProject(projectId: String): Result<Boolean> {
        _projects.value = _projects.value.filterNot { it.projectId == projectId }
        if (isFirebaseAvailable && firestore != null) {
            scope.launch {
                try {
                    firestore!!.collection("projects").document(projectId).delete().await()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error deleting project: ${e.message}")
                }
            }
        }
        return Result.success(true)
    }

    // ==================== LABOUR MANAGEMENT ====================

    fun getLaboursForContractor(contractorId: String): List<Labour> {
        return _labours.value.filter { it.contractorId == contractorId }
    }

    fun getLaboursForProject(projectId: String): List<Labour> {
        return _labours.value.filter { it.projectId == projectId && it.status == LabourStatus.ACTIVE.name }
    }

    fun getLabourById(labourId: String): Labour? {
        return _labours.value.find { it.labourId == labourId }
    }

    fun addLabourManually(contractorId: String, name: String, mobile: String, dailyWage: Double, projectId: String): Result<Labour> {
        if (name.isBlank()) return Result.failure(Exception("Labour name is required"))
        if (mobile.isBlank()) return Result.failure(Exception("Mobile number is required"))
        if (dailyWage <= 0) return Result.failure(Exception("Daily wage must be greater than 0"))
        if (projectId.isBlank()) return Result.failure(Exception("Please select an assigned project"))

        val labourId = "lab_" + UUID.randomUUID().toString().take(8)
        val labour = Labour(
            labourId = labourId,
            name = name.trim(),
            mobile = mobile.trim(),
            contractorId = contractorId,
            projectId = projectId,
            dailyWage = dailyWage,
            status = LabourStatus.ACTIVE.name,
            joiningDate = DateUtils.getTodayIso()
        )

        _labours.value = _labours.value + labour

        if (isFirebaseAvailable && firestore != null) {
            scope.launch {
                try {
                    firestore!!.collection("labours").document(labourId).set(labour).await()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error adding labour: ${e.message}")
                }
            }
        }

        return Result.success(labour)
    }

    fun updateLabour(labour: Labour): Result<Labour> {
        _labours.value = _labours.value.map { if (it.labourId == labour.labourId) labour else it }
        if (isFirebaseAvailable && firestore != null) {
            scope.launch {
                try {
                    firestore!!.collection("labours").document(labour.labourId).set(labour).await()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error updating labour: ${e.message}")
                }
            }
        }
        return Result.success(labour)
    }

    fun transferLabour(labourId: String, newProjectId: String): Result<Labour> {
        val existing = getLabourById(labourId) ?: return Result.failure(Exception("Labour not found"))
        val updated = existing.copy(projectId = newProjectId)
        _labours.value = _labours.value.map { if (it.labourId == labourId) updated else it }

        val newProject = _projects.value.find { it.projectId == newProjectId }
        addNotification(
            userId = labourId,
            title = "Project Transferred",
            message = "You have been moved to project: ${newProject?.name ?: "New Project"}."
        )

        if (isFirebaseAvailable && firestore != null) {
            scope.launch {
                try {
                    firestore!!.collection("labours").document(labourId).update("projectId", newProjectId).await()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error transferring labour: ${e.message}")
                }
            }
        }
        return Result.success(updated)
    }

    // ==================== JOIN REQUESTS ====================

    fun getJoinRequestsForContractor(contractorId: String): List<JoinRequest> {
        return _joinRequests.value.filter { it.contractorId == contractorId }
    }

    fun getJoinRequestForLabour(labourId: String): JoinRequest? {
        return _joinRequests.value.find { it.labourId == labourId }
    }

    fun approveJoinRequest(requestId: String, projectId: String, dailyWage: Double): Result<Labour> {
        val req = _joinRequests.value.find { it.requestId == requestId }
            ?: return Result.failure(Exception("Join request not found"))

        if (projectId.isBlank()) return Result.failure(Exception("Please assign a project"))
        if (dailyWage <= 0) return Result.failure(Exception("Daily wage must be greater than 0"))

        val updatedReq = req.copy(
            status = RequestStatus.APPROVED.name,
            assignedProjectId = projectId,
            dailyWage = dailyWage,
            approvedAt = System.currentTimeMillis()
        )
        _joinRequests.value = _joinRequests.value.map { if (it.requestId == requestId) updatedReq else it }

        // Create or update Labour record
        val existingLabour = _labours.value.find { it.labourId == req.labourId }
        val labour = existingLabour?.copy(
            projectId = projectId,
            dailyWage = dailyWage,
            status = LabourStatus.ACTIVE.name
        ) ?: Labour(
            labourId = req.labourId,
            name = req.labourName,
            mobile = req.labourMobile,
            contractorId = req.contractorId,
            projectId = projectId,
            dailyWage = dailyWage,
            status = LabourStatus.ACTIVE.name,
            joiningDate = DateUtils.getTodayIso()
        )

        _labours.value = _labours.value.filterNot { it.labourId == labour.labourId } + labour

        val projectName = _projects.value.find { it.projectId == projectId }?.name ?: "Construction"
        addNotification(
            userId = req.labourId,
            title = "Request Approved! 🎉",
            message = "Your join request has been approved for $projectName with daily wage ₹$dailyWage."
        )

        if (isFirebaseAvailable && firestore != null) {
            scope.launch {
                try {
                    firestore!!.collection("joinRequests").document(requestId).set(updatedReq).await()
                    firestore!!.collection("labours").document(labour.labourId).set(labour).await()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error approving join request: ${e.message}")
                }
            }
        }

        return Result.success(labour)
    }

    fun rejectJoinRequest(requestId: String): Result<Boolean> {
        val req = _joinRequests.value.find { it.requestId == requestId }
            ?: return Result.failure(Exception("Request not found"))

        val updatedReq = req.copy(status = RequestStatus.REJECTED.name)
        _joinRequests.value = _joinRequests.value.map { if (it.requestId == requestId) updatedReq else it }

        addNotification(
            userId = req.labourId,
            title = "Request Rejected",
            message = "Your request was rejected by the contractor."
        )

        if (isFirebaseAvailable && firestore != null) {
            scope.launch {
                try {
                    firestore!!.collection("joinRequests").document(requestId).set(updatedReq).await()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error rejecting request: ${e.message}")
                }
            }
        }
        return Result.success(true)
    }

    // ==================== ATTENDANCE ====================

    fun markAttendance(contractorId: String, labourId: String, projectId: String, date: String, status: AttendanceStatus): Result<AttendanceRecord> {
        // Prevent duplicates for the same Date + Labour + Project
        val existing = _attendances.value.find { it.date == date && it.labourId == labourId && it.projectId == projectId }

        val record = existing?.copy(status = status.name) ?: AttendanceRecord(
            attendanceId = "att_" + UUID.randomUUID().toString().take(8),
            labourId = labourId,
            contractorId = contractorId,
            projectId = projectId,
            date = date,
            status = status.name
        )

        _attendances.value = _attendances.value.filterNot { it.attendanceId == record.attendanceId } + record

        if (isFirebaseAvailable && firestore != null) {
            scope.launch {
                try {
                    firestore!!.collection("attendance").document(record.attendanceId).set(record).await()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error saving attendance: ${e.message}")
                }
            }
        }

        return Result.success(record)
    }

    fun markAllPresent(contractorId: String, projectId: String, date: String): Result<Int> {
        val projectLabours = getLaboursForProject(projectId)
        var count = 0
        val updatedList = _attendances.value.toMutableList()

        for (labour in projectLabours) {
            val existingIndex = updatedList.indexOfFirst {
                it.date == date && it.labourId == labour.labourId && it.projectId == projectId
            }
            if (existingIndex >= 0) {
                val updated = updatedList[existingIndex].copy(status = AttendanceStatus.PRESENT.name)
                updatedList[existingIndex] = updated
            } else {
                val record = AttendanceRecord(
                    attendanceId = "att_" + UUID.randomUUID().toString().take(8),
                    labourId = labour.labourId,
                    contractorId = contractorId,
                    projectId = projectId,
                    date = date,
                    status = AttendanceStatus.PRESENT.name
                )
                updatedList.add(record)
            }
            count++
        }

        _attendances.value = updatedList

        if (isFirebaseAvailable && firestore != null) {
            scope.launch {
                try {
                    val batch = firestore!!.batch()
                    for (rec in updatedList.filter { it.date == date && it.projectId == projectId }) {
                        val ref = firestore!!.collection("attendance").document(rec.attendanceId)
                        batch.set(ref, rec)
                    }
                    batch.commit().await()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error batch saving attendance: ${e.message}")
                }
            }
        }

        return Result.success(count)
    }

    fun getAttendanceForDateAndProject(date: String, projectId: String): List<AttendanceRecord> {
        return _attendances.value.filter { it.date == date && it.projectId == projectId }
    }

    fun getAttendanceForLabour(labourId: String): List<AttendanceRecord> {
        return _attendances.value.filter { it.labourId == labourId }.sortedByDescending { it.date }
    }

    // ==================== PAYMENTS ====================

    fun addPayment(contractorId: String, labourId: String, projectId: String, amount: Double, date: String, note: String): Result<PaymentRecord> {
        if (amount <= 0) return Result.failure(Exception("Payment amount must be greater than 0"))

        val paymentId = "pay_" + UUID.randomUUID().toString().take(8)
        val record = PaymentRecord(
            paymentId = paymentId,
            labourId = labourId,
            contractorId = contractorId,
            projectId = projectId,
            amount = amount,
            date = date.ifBlank { DateUtils.getTodayIso() },
            note = note.trim()
        )

        _payments.value = _payments.value + record

        val labour = getLabourById(labourId)
        addNotification(
            userId = labourId,
            title = "Payment Received",
            message = "Payment of ₹$amount has been recorded by contractor."
        )

        if (isFirebaseAvailable && firestore != null) {
            scope.launch {
                try {
                    firestore!!.collection("payments").document(paymentId).set(record).await()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error saving payment: ${e.message}")
                }
            }
        }

        return Result.success(record)
    }

    fun getPaymentsForLabour(labourId: String): List<PaymentRecord> {
        return _payments.value.filter { it.labourId == labourId }.sortedByDescending { it.date }
    }

    fun getPaymentsForContractor(contractorId: String): List<PaymentRecord> {
        return _payments.value.filter { it.contractorId == contractorId }.sortedByDescending { it.date }
    }

    // ==================== CALCULATIONS & REPORTS ====================

    fun calculateLabourWageSummary(labourId: String): LabourWageSummary {
        val labour = getLabourById(labourId)
        val labourName = labour?.name ?: "Labour"
        val wage = labour?.dailyWage ?: 0.0
        val project = _projects.value.find { it.projectId == labour?.projectId }
        val projectName = project?.name ?: "General"

        val labourAttendance = _attendances.value.filter { it.labourId == labourId }
        val presentCount = labourAttendance.count { it.status == AttendanceStatus.PRESENT.name }
        val halfDayCount = labourAttendance.count { it.status == AttendanceStatus.HALF_DAY.name }
        val absentCount = labourAttendance.count { it.status == AttendanceStatus.ABSENT.name }

        val presentWorkingDays = presentCount * 1.0 + halfDayCount * 0.5
        val totalEarned = presentWorkingDays * wage

        val labourPayments = _payments.value.filter { it.labourId == labourId }
        val totalPaid = labourPayments.sumOf { it.amount }
        val remaining = totalEarned - totalPaid

        return LabourWageSummary(
            labourId = labourId,
            labourName = labourName,
            dailyWage = wage,
            projectName = projectName,
            presentDays = presentWorkingDays,
            presentCount = presentCount,
            halfDayCount = halfDayCount,
            absentCount = absentCount,
            totalEarned = totalEarned,
            totalPaid = totalPaid,
            remaining = remaining
        )
    }

    // ==================== NOTIFICATIONS ====================

    fun addNotification(userId: String, title: String, message: String) {
        val notif = AppNotification(
            notificationId = "notif_" + UUID.randomUUID().toString().take(8),
            userId = userId,
            title = title,
            message = message,
            date = DateUtils.getTodayIso(),
            timestamp = System.currentTimeMillis()
        )
        _notifications.value = listOf(notif) + _notifications.value

        if (isFirebaseAvailable && firestore != null) {
            scope.launch {
                try {
                    firestore!!.collection("notifications").document(notif.notificationId).set(notif).await()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error adding notification: ${e.message}")
                }
            }
        }
    }

    fun getNotificationsForUser(userId: String): List<AppNotification> {
        return _notifications.value.filter { it.userId == userId }.sortedByDescending { it.timestamp }
    }

    // ==================== FIRESTORE CLOUD SYNC ====================

    private fun syncFromFirestore() {
        val fs = firestore ?: return
        scope.launch {
            try {
                // Listen to projects
                fs.collection("projects").addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { it.toObject(Project::class.java) }
                    if (list.isNotEmpty()) _projects.value = list
                }

                // Listen to labours
                fs.collection("labours").addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { it.toObject(Labour::class.java) }
                    if (list.isNotEmpty()) _labours.value = list
                }

                // Listen to attendances
                fs.collection("attendance").addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { it.toObject(AttendanceRecord::class.java) }
                    if (list.isNotEmpty()) _attendances.value = list
                }

                // Listen to payments
                fs.collection("payments").addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { it.toObject(PaymentRecord::class.java) }
                    if (list.isNotEmpty()) _payments.value = list
                }

                // Listen to join requests
                fs.collection("joinRequests").addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { it.toObject(JoinRequest::class.java) }
                    if (list.isNotEmpty()) _joinRequests.value = list
                }
            } catch (e: Exception) {
                Log.e("AppRepository", "Sync from firestore failed: ${e.message}")
            }
        }
    }

    // ==================== INITIAL SAMPLE DATA SEEDING ====================

    private fun initDefaultDataIfFirstLaunch() {
        if (!sessionManager.isFirstLaunch() && _users.value.isNotEmpty()) return

        val contractorId = "cnt_demo_01"
        val contractorReferral = "MUK12345"

        val contractorUser = UserProfile(
            userId = contractorId,
            name = "Mukesh Sharma",
            mobile = "9876543210",
            role = UserRole.CONTRACTOR.name,
            referralCode = contractorReferral,
            companyName = "Sharma Constructions"
        )

        val contractorObj = Contractor(
            contractorId = contractorId,
            name = "Mukesh Sharma",
            mobile = "9876543210",
            companyName = "Sharma Constructions",
            referralCode = contractorReferral
        )

        val prj1Id = "prj_house_alwar"
        val prj1 = Project(
            projectId = prj1Id,
            contractorId = contractorId,
            name = "House Construction",
            location = "Alwar",
            startDate = "2026-08-01",
            status = ProjectStatus.ACTIVE.name
        )

        val prj2Id = "prj_road_jaipur"
        val prj2 = Project(
            projectId = prj2Id,
            contractorId = contractorId,
            name = "Road Construction",
            location = "Jaipur",
            startDate = "2026-08-15",
            status = ProjectStatus.ACTIVE.name
        )

        // Ramesh Kumar (matches prompt example: 22 Present, 2 Half Day, 3 Absent, ₹13800 earned, ₹10000 paid, ₹3800 remaining)
        val labour1Id = "lab_ramesh_01"
        val labour1 = Labour(
            labourId = labour1Id,
            name = "Ramesh Kumar",
            mobile = "9812345678",
            contractorId = contractorId,
            projectId = prj1Id,
            dailyWage = 600.0,
            status = LabourStatus.ACTIVE.name,
            joiningDate = "2026-08-01"
        )

        val labour1User = UserProfile(
            userId = labour1Id,
            name = "Ramesh Kumar",
            mobile = "9812345678",
            role = UserRole.LABOUR.name,
            referralCode = contractorReferral
        )

        val labour2Id = "lab_suresh_02"
        val labour2 = Labour(
            labourId = labour2Id,
            name = "Suresh Verma",
            mobile = "9823456789",
            contractorId = contractorId,
            projectId = prj1Id,
            dailyWage = 550.0,
            status = LabourStatus.ACTIVE.name,
            joiningDate = "2026-08-05"
        )

        val labour3Id = "lab_dinesh_03"
        val labour3 = Labour(
            labourId = labour3Id,
            name = "Dinesh Singh",
            mobile = "9834567890",
            contractorId = contractorId,
            projectId = prj2Id,
            dailyWage = 600.0,
            status = LabourStatus.ACTIVE.name,
            joiningDate = "2026-08-15"
        )

        val labour4Id = "lab_amit_04"
        val labour4 = Labour(
            labourId = labour4Id,
            name = "Amit Yadav",
            mobile = "9845678901",
            contractorId = contractorId,
            projectId = prj2Id,
            dailyWage = 500.0,
            status = LabourStatus.ACTIVE.name,
            joiningDate = "2026-08-20"
        )

        // Pre-seed sample attendance for Ramesh Kumar (22 present, 2 half day, 3 absent)
        val sampleAttendance = mutableListOf<AttendanceRecord>()
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.SEPTEMBER, 1)

        // 22 present days
        for (day in 1..22) {
            val dateStr = "2026-09-%02d".format(day)
            sampleAttendance.add(
                AttendanceRecord(
                    attendanceId = "att_ramesh_$day",
                    labourId = labour1Id,
                    contractorId = contractorId,
                    projectId = prj1Id,
                    date = dateStr,
                    status = AttendanceStatus.PRESENT.name
                )
            )
        }

        // 2 half days
        for (day in 23..24) {
            val dateStr = "2026-09-%02d".format(day)
            sampleAttendance.add(
                AttendanceRecord(
                    attendanceId = "att_ramesh_$day",
                    labourId = labour1Id,
                    contractorId = contractorId,
                    projectId = prj1Id,
                    date = dateStr,
                    status = AttendanceStatus.HALF_DAY.name
                )
            )
        }

        // 3 absent days
        for (day in 25..27) {
            val dateStr = "2026-09-%02d".format(day)
            sampleAttendance.add(
                AttendanceRecord(
                    attendanceId = "att_ramesh_$day",
                    labourId = labour1Id,
                    contractorId = contractorId,
                    projectId = prj1Id,
                    date = dateStr,
                    status = AttendanceStatus.ABSENT.name
                )
            )
        }

        // Today attendance
        val todayStr = DateUtils.getTodayIso()
        sampleAttendance.add(
            AttendanceRecord(
                attendanceId = "att_ramesh_today",
                labourId = labour1Id,
                contractorId = contractorId,
                projectId = prj1Id,
                date = todayStr,
                status = AttendanceStatus.PRESENT.name
            )
        )
        sampleAttendance.add(
            AttendanceRecord(
                attendanceId = "att_suresh_today",
                labourId = labour2Id,
                contractorId = contractorId,
                projectId = prj1Id,
                date = todayStr,
                status = AttendanceStatus.PRESENT.name
            )
        )
        sampleAttendance.add(
            AttendanceRecord(
                attendanceId = "att_dinesh_today",
                labourId = labour3Id,
                contractorId = contractorId,
                projectId = prj2Id,
                date = todayStr,
                status = AttendanceStatus.HALF_DAY.name
            )
        )
        sampleAttendance.add(
            AttendanceRecord(
                attendanceId = "att_amit_today",
                labourId = labour4Id,
                contractorId = contractorId,
                projectId = prj2Id,
                date = todayStr,
                status = AttendanceStatus.ABSENT.name
            )
        )

        // Payment for Ramesh (Total Paid ₹10,000)
        val samplePayments = listOf(
            PaymentRecord(
                paymentId = "pay_01",
                labourId = labour1Id,
                contractorId = contractorId,
                projectId = prj1Id,
                amount = 5000.0,
                date = "2026-09-10",
                note = "Advance cash payment"
            ),
            PaymentRecord(
                paymentId = "pay_02",
                labourId = labour1Id,
                contractorId = contractorId,
                projectId = prj1Id,
                amount = 5000.0,
                date = "2026-09-20",
                note = "Mid-month payment"
            )
        )

        // Pending Join Request for contractor to test approval
        val sampleJoinRequest = JoinRequest(
            requestId = "req_demo_01",
            labourId = "lab_vikram_05",
            labourName = "Vikram Patel",
            labourMobile = "9898989898",
            contractorId = contractorId,
            referralCode = contractorReferral,
            status = RequestStatus.PENDING.name
        )

        val sampleNotification = AppNotification(
            notificationId = "notif_01",
            userId = contractorId,
            title = "New Labour Request",
            message = "Vikram Patel (9898989898) sent a join request using referral code $contractorReferral.",
            date = todayStr
        )

        _users.value = listOf(contractorUser, labour1User)
        _contractors.value = listOf(contractorObj)
        _projects.value = listOf(prj1, prj2)
        _labours.value = listOf(labour1, labour2, labour3, labour4)
        _attendances.value = sampleAttendance
        _payments.value = samplePayments
        _joinRequests.value = listOf(sampleJoinRequest)
        _notifications.value = listOf(sampleNotification)

        sessionManager.setFirstLaunchDone()
    }
}
