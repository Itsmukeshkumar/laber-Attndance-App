package com.example.ui.contractor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.AppTopBar
import com.example.ui.components.AttendanceBadge
import com.example.ui.components.AttendanceSelectorButtons
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import com.example.viewmodel.ContractorViewModel

// ==================== ATTENDANCE SCREEN ====================

@Composable
fun ContractorAttendanceScreen(
    viewModel: ContractorViewModel,
    onBack: () -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val labours by viewModel.labours.collectAsState()
    val attendances by viewModel.attendances.collectAsState()
    val selectedDate by viewModel.selectedAttendanceDate.collectAsState()
    val selectedProjectId by viewModel.selectedProjectId.collectAsState()

    val myProjects = projects.filter { it.contractorId == viewModel.contractorId }
    val currentProject = myProjects.find { it.projectId == selectedProjectId } ?: myProjects.firstOrNull()

    val assignedLabours = labours.filter {
        it.contractorId == viewModel.contractorId &&
        it.projectId == currentProject?.projectId &&
        it.status == LabourStatus.ACTIVE.name
    }

    val todayStr = DateUtils.getTodayIso()
    val yesterdayStr = DateUtils.getYesterdayIso()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Mark Attendance",
                subtitle = "Date: ${DateUtils.formatToDisplay(selectedDate)}",
                onBackClick = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Project Selector Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Select Project:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (myProjects.isEmpty()) {
                    Text("No projects found. Please add a project first.", color = MaterialTheme.colorScheme.error)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(myProjects) { prj ->
                            FilterChip(
                                selected = prj.projectId == currentProject?.projectId,
                                onClick = { viewModel.selectProject(prj.projectId) },
                                label = { Text(prj.name) },
                                leadingIcon = {
                                    if (prj.projectId == currentProject?.projectId) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date Selector Chips
                Text(
                    text = "Select Date:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedDate == todayStr,
                        onClick = { viewModel.setAttendanceDate(todayStr) },
                        label = { Text("Today") }
                    )
                    FilterChip(
                        selected = selectedDate == yesterdayStr,
                        onClick = { viewModel.setAttendanceDate(yesterdayStr) },
                        label = { Text("Yesterday") }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Header with "Mark All Present" button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${assignedLabours.size} Labour${if (assignedLabours.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (assignedLabours.isNotEmpty()) {
                    Button(
                        onClick = { viewModel.markAllPresent() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("mark_all_present_button")
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark All Present", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Labours Attendance List
            if (assignedLabours.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.PeopleOutline,
                    title = "No Labours in this Project",
                    message = "Assign labour to ${currentProject?.name ?: "this project"} in the Labour tab to mark attendance."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(assignedLabours) { labour ->
                        val record = attendances.find {
                            it.labourId == labour.labourId &&
                            it.date == selectedDate &&
                            it.projectId == currentProject?.projectId
                        }
                        val currentStatus = record?.status ?: "NOT_MARKED"

                        LabourAttendanceCard(
                            labour = labour,
                            status = currentStatus,
                            onStatusSelected = { status ->
                                viewModel.markAttendance(labour.labourId, status)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun LabourAttendanceCard(
    labour: Labour,
    status: String,
    onStatusSelected: (AttendanceStatus) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = labour.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Wage: ${CurrencyUtils.formatInrClean(labour.dailyWage)}/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AttendanceBadge(status = status)
            }

            Spacer(modifier = Modifier.height(14.dp))

            AttendanceSelectorButtons(
                currentStatus = status,
                onStatusSelected = onStatusSelected
            )
        }
    }
}

// ==================== PAYMENTS SCREEN ====================

@Composable
fun ContractorPaymentsScreen(
    viewModel: ContractorViewModel,
    onBack: () -> Unit
) {
    val labours by viewModel.labours.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val payments by viewModel.payments.collectAsState()

    val contractorLabours = labours.filter {
        it.contractorId == viewModel.contractorId && it.status == LabourStatus.ACTIVE.name
    }

    var selectedLabourForPayment by remember { mutableStateOf<Labour?>(null) }
    var selectedLabourForHistory by remember { mutableStateOf<Labour?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Labour Payments",
                subtitle = "Manage wages, advances & balances",
                onBackClick = onBack
            )
        }
    ) { padding ->
        if (contractorLabours.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Payments,
                title = "No Active Labour",
                message = "Add labour to calculate earned wages and record payments.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(contractorLabours) { labour ->
                    val summary = viewModel.getLabourWageSummary(labour.labourId)
                    val project = projects.find { it.projectId == labour.projectId }

                    LabourPaymentCard(
                        summary = summary,
                        projectName = project?.name ?: "Construction",
                        onAddPayment = { selectedLabourForPayment = labour },
                        onViewHistory = { selectedLabourForHistory = labour }
                    )
                }
            }
        }
    }

    selectedLabourForPayment?.let { lab ->
        AddPaymentDialog(
            labour = lab,
            onDismiss = { selectedLabourForPayment = null },
            onConfirm = { amount, date, note ->
                viewModel.addPayment(lab.labourId, lab.projectId, amount, date, note)
                selectedLabourForPayment = null
            }
        )
    }

    selectedLabourForHistory?.let { lab ->
        val labourPayments = payments.filter { it.labourId == lab.labourId }.sortedByDescending { it.date }
        PaymentHistoryDialog(
            labourName = lab.name,
            payments = labourPayments,
            onDismiss = { selectedLabourForHistory = null }
        )
    }
}

@Composable
fun LabourPaymentCard(
    summary: LabourWageSummary,
    projectName: String,
    onAddPayment: () -> Unit,
    onViewHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = summary.labourName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$projectName • Daily Wage: ${CurrencyUtils.formatInrClean(summary.dailyWage)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Attendance Breakdown Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = StatusPresentBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Present", style = MaterialTheme.typography.labelSmall, color = StatusPresent)
                        Text("${summary.presentCount}", fontWeight = FontWeight.Bold, color = StatusPresent)
                    }
                }
                Surface(
                    color = StatusHalfDayBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Half Day", style = MaterialTheme.typography.labelSmall, color = StatusHalfDay)
                        Text("${summary.halfDayCount}", fontWeight = FontWeight.Bold, color = StatusHalfDay)
                    }
                }
                Surface(
                    color = StatusAbsentBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Absent", style = MaterialTheme.typography.labelSmall, color = StatusAbsent)
                        Text("${summary.absentCount}", fontWeight = FontWeight.Bold, color = StatusAbsent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Financial Breakdown: Earned | Paid | Remaining
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Earned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = CurrencyUtils.formatInrClean(summary.totalEarned),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text("Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = CurrencyUtils.formatInrClean(summary.totalPaid),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = StatusPresent
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = CurrencyUtils.formatInrClean(summary.remaining),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (summary.remaining > 0) MaterialTheme.colorScheme.error else StatusPresent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onViewHistory,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("History", fontSize = 13.sp)
                }

                Button(
                    onClick = onAddPayment,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(42.dp).testTag("add_payment_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Payment", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun AddPaymentDialog(
    labour: Labour,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, date: String, note: String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(DateUtils.getTodayIso()) }
    var noteStr by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Payment") },
        text = {
            Column {
                Text("Labour: ${labour.name}")
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                    label = { Text("Amount (₹) *") },
                    placeholder = { Text("e.g. 5000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = noteStr,
                    onValueChange = { noteStr = it },
                    label = { Text("Note / Remark (Optional)") },
                    placeholder = { Text("e.g. Advance, Cash payment") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        error = "Please enter a valid amount greater than 0"
                    } else {
                        onConfirm(amount, dateStr, noteStr)
                    }
                }
            ) {
                Text("Record Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PaymentHistoryDialog(
    labourName: String,
    payments: List<PaymentRecord>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$labourName - Payment History") },
        text = {
            if (payments.isEmpty()) {
                Text("No payment records found for this labour yet.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(payments) { pay ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = DateUtils.formatToDisplay(pay.date),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    if (pay.note.isNotBlank()) {
                                        Text(
                                            text = pay.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = CurrencyUtils.formatInrClean(pay.amount),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StatusPresent
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// ==================== REPORTS TAB ====================

@Composable
fun ContractorReportsTab(viewModel: ContractorViewModel) {
    val projects by viewModel.projects.collectAsState()
    val labours by viewModel.labours.collectAsState()
    val attendances by viewModel.attendances.collectAsState()
    val payments by viewModel.payments.collectAsState()

    val myProjects = projects.filter { it.contractorId == viewModel.contractorId }
    val myLabours = labours.filter { it.contractorId == viewModel.contractorId }

    var selectedReportType by remember { mutableStateOf(0) }
    // 0: Labour-wise, 1: Project-wise, 2: Monthly Summary, 3: Pending Payments

    var selectedProjectFilter by remember { mutableStateOf<String>("ALL") }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Reports & Analytics",
                subtitle = "Attendance & payment summaries"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Report type chips
            ScrollableTabRow(
                selectedTabIndex = selectedReportType,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedReportType == 0,
                    onClick = { selectedReportType = 0 },
                    text = { Text("Labour-wise") }
                )
                Tab(
                    selected = selectedReportType == 1,
                    onClick = { selectedReportType = 1 },
                    text = { Text("Project-wise") }
                )
                Tab(
                    selected = selectedReportType == 2,
                    onClick = { selectedReportType = 2 },
                    text = { Text("Monthly") }
                )
                Tab(
                    selected = selectedReportType == 3,
                    onClick = { selectedReportType = 3 },
                    text = { Text("Pending Payments") }
                )
            }

            // Project filter
            if (selectedReportType != 1 && myProjects.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Project:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedProjectFilter == "ALL",
                                onClick = { selectedProjectFilter = "ALL" },
                                label = { Text("All") }
                            )
                        }
                        items(myProjects) { prj ->
                            FilterChip(
                                selected = selectedProjectFilter == prj.projectId,
                                onClick = { selectedProjectFilter = prj.projectId },
                                label = { Text(prj.name) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Report Content
            val filteredLabours = if (selectedProjectFilter == "ALL") {
                myLabours
            } else {
                myLabours.filter { it.projectId == selectedProjectFilter }
            }

            when (selectedReportType) {
                0 -> {
                    // Labour-wise report
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredLabours) { lab ->
                            val summary = viewModel.getLabourWageSummary(lab.labourId)
                            val prj = myProjects.find { it.projectId == lab.projectId }
                            ReportLabourCard(summary, prj?.name ?: "Construction")
                        }
                    }
                }
                1 -> {
                    // Project-wise report
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(myProjects) { prj ->
                            val prjLabours = myLabours.filter { it.projectId == prj.projectId }
                            val prjAttendances = attendances.filter { it.projectId == prj.projectId }
                            val prjPayments = payments.filter { it.projectId == prj.projectId }

                            val totalPresent = prjAttendances.count { it.status == AttendanceStatus.PRESENT.name }
                            val totalHalfDay = prjAttendances.count { it.status == AttendanceStatus.HALF_DAY.name }
                            val totalAbsent = prjAttendances.count { it.status == AttendanceStatus.ABSENT.name }
                            val totalPaid = prjPayments.sumOf { it.amount }

                            ReportProjectCard(
                                project = prj,
                                labourCount = prjLabours.size,
                                presentCount = totalPresent,
                                halfDayCount = totalHalfDay,
                                absentCount = totalAbsent,
                                totalPaid = totalPaid
                            )
                        }
                    }
                }
                2 -> {
                    // Monthly attendance summary
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Month: September 2026",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Total Active Labours: ${filteredLabours.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        items(filteredLabours) { lab ->
                            val summary = viewModel.getLabourWageSummary(lab.labourId)
                            ReportLabourCard(summary, summary.projectName)
                        }
                    }
                }
                3 -> {
                    // Pending Payments Report
                    val pendingLabours = filteredLabours.map { viewModel.getLabourWageSummary(it.labourId) }
                        .filter { it.remaining > 0 }

                    if (pendingLabours.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.CheckCircle,
                            title = "No Pending Payments",
                            message = "All labours have been settled up!"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pendingLabours) { summary ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = summary.labourName,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Earned: ${CurrencyUtils.formatInrClean(summary.totalEarned)} | Paid: ${CurrencyUtils.formatInrClean(summary.totalPaid)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Due",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = CurrencyUtils.formatInrClean(summary.remaining),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportLabourCard(summary: LabourWageSummary, projectName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = summary.labourName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "$projectName • Wage: ${CurrencyUtils.formatInrClean(summary.dailyWage)}/day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Present: ${summary.presentCount}  |  Half Day: ${summary.halfDayCount}  |  Absent: ${summary.absentCount}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Earned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyUtils.formatInrClean(summary.totalEarned), fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyUtils.formatInrClean(summary.totalPaid), fontWeight = FontWeight.Bold, color = StatusPresent)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        CurrencyUtils.formatInrClean(summary.remaining),
                        fontWeight = FontWeight.Bold,
                        color = if (summary.remaining > 0) MaterialTheme.colorScheme.error else StatusPresent
                    )
                }
            }
        }
    }
}

@Composable
fun ReportProjectCard(
    project: Project,
    labourCount: Int,
    presentCount: Int,
    halfDayCount: Int,
    absentCount: Int,
    totalPaid: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Location: ${project.location} • $labourCount Labours Assigned",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Present Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$presentCount", fontWeight = FontWeight.Bold, color = StatusPresent)
                }
                Column {
                    Text("Half Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$halfDayCount", fontWeight = FontWeight.Bold, color = StatusHalfDay)
                }
                Column {
                    Text("Absent Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$absentCount", fontWeight = FontWeight.Bold, color = StatusAbsent)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyUtils.formatInrClean(totalPaid), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ==================== REQUESTS SCREEN ====================

@Composable
fun ContractorRequestsScreen(
    viewModel: ContractorViewModel,
    onBack: () -> Unit
) {
    val pendingReqs by viewModel.pendingRequests.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val myProjects = projects.filter { it.contractorId == viewModel.contractorId }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Labour Join Requests",
                subtitle = "${pendingReqs.size} pending review",
                onBackClick = onBack
            )
        }
    ) { padding ->
        if (pendingReqs.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.CheckCircle,
                title = "All Caught Up!",
                message = "There are no pending join requests right now.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingReqs) { req ->
                    JoinRequestCard(
                        request = req,
                        projects = myProjects,
                        onApprove = { prjId, wage ->
                            viewModel.approveRequest(req.requestId, prjId, wage)
                        },
                        onReject = {
                            viewModel.rejectRequest(req.requestId)
                        }
                    )
                }
            }
        }
    }
}
