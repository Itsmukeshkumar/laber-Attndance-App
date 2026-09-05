package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

enum class UserRole {
    CONTRACTOR,
    LABOUR
}

enum class AttendanceStatus {
    PRESENT,
    HALF_DAY,
    ABSENT
}

enum class ProjectStatus {
    ACTIVE,
    COMPLETED
}

enum class LabourStatus {
    ACTIVE,
    INACTIVE
}

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val mobile: String = "",
    val role: String = "", // CONTRACTOR or LABOUR
    val referralCode: String = "",
    val companyName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class Contractor(
    val contractorId: String = "",
    val name: String = "",
    val mobile: String = "",
    val companyName: String = "",
    val referralCode: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class Project(
    val projectId: String = "",
    val contractorId: String = "",
    val name: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val status: String = ProjectStatus.ACTIVE.name,
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class Labour(
    val labourId: String = "",
    val name: String = "",
    val mobile: String = "",
    val contractorId: String = "",
    val projectId: String = "",
    val dailyWage: Double = 0.0,
    val status: String = LabourStatus.ACTIVE.name,
    val joiningDate: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class AttendanceRecord(
    val attendanceId: String = "",
    val labourId: String = "",
    val contractorId: String = "",
    val projectId: String = "",
    val date: String = "", // Format: YYYY-MM-DD
    val status: String = AttendanceStatus.PRESENT.name,
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class PaymentRecord(
    val paymentId: String = "",
    val labourId: String = "",
    val contractorId: String = "",
    val projectId: String = "",
    val amount: Double = 0.0,
    val date: String = "", // Format: YYYY-MM-DD
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class JoinRequest(
    val requestId: String = "",
    val labourId: String = "",
    val labourName: String = "",
    val labourMobile: String = "",
    val contractorId: String = "",
    val referralCode: String = "",
    val status: String = RequestStatus.PENDING.name,
    val assignedProjectId: String = "",
    val dailyWage: Double = 0.0,
    val requestedAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null
)

@IgnoreExtraProperties
data class AppNotification(
    val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val date: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class LabourWageSummary(
    val labourId: String,
    val labourName: String,
    val dailyWage: Double,
    val projectName: String,
    val presentDays: Double, // 1 for present, 0.5 for half day
    val presentCount: Int,
    val halfDayCount: Int,
    val absentCount: Int,
    val totalEarned: Double,
    val totalPaid: Double,
    val remaining: Double
)
