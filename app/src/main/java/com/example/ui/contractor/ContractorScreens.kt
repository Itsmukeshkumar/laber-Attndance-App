package com.example.ui.contractor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.AppTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatCard
import com.example.ui.theme.*
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import com.example.viewmodel.ContractorViewModel

@Composable
fun ContractorMainScreen(
    viewModel: ContractorViewModel,
    onLogout: () -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val subScreen by viewModel.currentSubScreen.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        bottomBar = {
            if (subScreen == null) {
                Column {
                    HorizontalDivider(color = GeoBorder, thickness = 1.dp)
                    NavigationBar(
                        containerColor = GeoSurface,
                        tonalElevation = 0.dp
                    ) {
                        val navItemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GeoOnBlueContainer,
                            selectedTextColor = GeoOnBlueContainer,
                            indicatorColor = GeoBlueContainer,
                            unselectedIconColor = GeoTextSecondary.copy(alpha = 0.7f),
                            unselectedTextColor = GeoTextSecondary.copy(alpha = 0.7f)
                        )
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { viewModel.setSelectedTab(0) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_contractor_home")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { viewModel.setSelectedTab(1) },
                            icon = { Icon(Icons.Default.Business, contentDescription = "Projects") },
                            label = { Text("Projects", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_contractor_projects")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { viewModel.setSelectedTab(2) },
                            icon = { Icon(Icons.Default.People, contentDescription = "Labour") },
                            label = { Text("Labour", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_contractor_labour")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { viewModel.setSelectedTab(3) },
                            icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                            label = { Text("Reports", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_contractor_reports")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 4,
                            onClick = { viewModel.setSelectedTab(4) },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile", fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_contractor_profile")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (subScreen) {
                "ATTENDANCE" -> {
                    ContractorAttendanceScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateToSubScreen(null) }
                    )
                }
                "PAYMENTS" -> {
                    ContractorPaymentsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateToSubScreen(null) }
                    )
                }
                "REQUESTS" -> {
                    ContractorRequestsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateToSubScreen(null) }
                    )
                }
                else -> {
                    when (selectedTab) {
                        0 -> ContractorDashboardTab(viewModel = viewModel)
                        1 -> ContractorProjectsTab(viewModel = viewModel)
                        2 -> ContractorLabourTab(viewModel = viewModel)
                        3 -> ContractorReportsTab(viewModel = viewModel)
                        4 -> ContractorProfileTab(viewModel = viewModel, onLogout = onLogout)
                    }
                }
            }
        }
    }
}

@Composable
fun ContractorDashboardTab(viewModel: ContractorViewModel) {
    val stats by viewModel.dashboardStats.collectAsState()
    val pendingReqs by viewModel.pendingRequests.collectAsState()
    val projectsWithCounts by viewModel.projectsWithCounts.collectAsState()
    val sessionManager = remember { viewModel.getApplication<android.app.Application>() }
    val repoSession = viewModel.contractorId
    val contractorName = viewModel.projects.collectAsState().value.firstOrNull { it.contractorId == repoSession }?.let {
        "Contractor"
    } ?: "Mukesh Singh"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GeoBackground)
    ) {
        // Geometric Balance Header
        Surface(
            color = GeoSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WELCOME BACK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 11.sp
                        ),
                        color = GeoTextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = contractorName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = GeoTextPrimary
                    )
                }

                val initials = contractorName.split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .ifEmpty { "MS" }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GeoBluePrimary)
                        .border(BorderStroke(2.dp, GeoBlueContainer), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
        HorizontalDivider(color = GeoBorder, thickness = 1.dp)

        // Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Pending Request Alert Banner
            if (pendingReqs.isNotEmpty()) {
                Surface(
                    color = GeoBlueContainer,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GeoBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.navigateToSubScreen("REQUESTS") }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(GeoBluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${pendingReqs.size}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "New Labour Join Request",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = GeoOnBlueContainer
                            )
                            Text(
                                text = "${pendingReqs.first().labourName} requested to join",
                                style = MaterialTheme.typography.bodySmall,
                                color = GeoOnBlueContainer.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = "Review →",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = GeoBluePrimary
                        )
                    }
                }
            }

            // Summary Cards Grid (2x2) with Geometric Balance Palette
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Labour",
                        value = "${stats.totalLabour}",
                        containerColor = GeoBlueContainer,
                        contentColor = GeoOnBlueContainer,
                        onClick = { viewModel.setSelectedTab(2) }
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Projects",
                        value = "${stats.totalProjects}".padStart(2, '0'),
                        containerColor = GeoPurpleContainer,
                        contentColor = GeoOnPurpleContainer,
                        onClick = { viewModel.setSelectedTab(1) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Present Today",
                        value = "${stats.todayPresent}",
                        containerColor = GeoGreenContainer,
                        contentColor = GeoOnGreenContainer,
                        onClick = { viewModel.navigateToSubScreen("ATTENDANCE") }
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Absent",
                        value = "${stats.todayAbsent}".padStart(2, '0'),
                        containerColor = GeoRedContainer,
                        contentColor = GeoOnRedContainer,
                        onClick = { viewModel.navigateToSubScreen("ATTENDANCE") }
                    )
                }
            }

            // Quick Actions (3 Column Grid)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "QUICK ACTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontSize = 12.sp
                    ),
                    color = GeoTextSecondary,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GeometricActionButton(
                        modifier = Modifier.weight(1f),
                        title = "Attendance",
                        icon = Icons.Default.CalendarMonth,
                        onClick = { viewModel.navigateToSubScreen("ATTENDANCE") }
                    )
                    GeometricActionButton(
                        modifier = Modifier.weight(1f),
                        title = "Payments",
                        icon = Icons.Default.CreditCard,
                        onClick = { viewModel.navigateToSubScreen("PAYMENTS") }
                    )
                    GeometricActionButton(
                        modifier = Modifier.weight(1f),
                        title = "Requests",
                        icon = Icons.Default.MailOutline,
                        onClick = { viewModel.navigateToSubScreen("REQUESTS") }
                    )
                }
            }

            // Active Projects Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE PROJECTS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontSize = 12.sp
                        ),
                        color = GeoTextSecondary
                    )
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = GeoBluePrimary,
                        modifier = Modifier
                            .clickable { viewModel.setSelectedTab(1) }
                            .padding(4.dp)
                    )
                }

                val activeProject = projectsWithCounts.firstOrNull { it.project.status == "ACTIVE" }
                    ?: projectsWithCounts.firstOrNull()

                if (activeProject != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { viewModel.setSelectedTab(1) },
                        shape = RoundedCornerShape(24.dp),
                        color = GeoSurface,
                        border = BorderStroke(1.dp, GeoBorder),
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeProject.project.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = GeoTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = activeProject.project.location.ifBlank { "Site Location" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GeoTextSecondary
                                    )
                                }

                                Surface(
                                    color = if (activeProject.project.status == "ACTIVE") GeoGreenContainer else GeoSurfaceVariant,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = activeProject.project.status.uppercase(),
                                        color = if (activeProject.project.status == "ACTIVE") GeoOnGreenContainer else GeoTextSecondary,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = GeoBorderLight, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Overlapping Avatars Stack
                                Row(
                                    modifier = Modifier.padding(end = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy((-8).dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(GeoBluePrimary)
                                            .border(BorderStroke(2.dp, Color.White), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("RK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(GeoPurplePrimary)
                                            .border(BorderStroke(2.dp, Color.White), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("AS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(GeoBlueContainer)
                                            .border(BorderStroke(2.dp, Color.White), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val extra = (activeProject.labourCount - 2).coerceAtLeast(1)
                                        Text("+$extra", color = GeoBluePrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }

                                Text(
                                    text = "${activeProject.labourCount} Labours assigned",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = GeoTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Geometric Balance Action Button
 */
@Composable
fun GeometricActionButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(94.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        color = GeoSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, GeoOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GeoBluePrimary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = GeoBluePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = GeoTextBody,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== PROJECTS TAB ====================

@Composable
fun ContractorProjectsTab(viewModel: ContractorViewModel) {
    val projectsWithCounts by viewModel.projectsWithCounts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var projectToEdit by remember { mutableStateOf<Project?>(null) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Projects",
                subtitle = "Manage multiple construction sites"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_project_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Project")
            }
        }
    ) { padding ->
        if (projectsWithCounts.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Business,
                title = "No Projects Yet",
                message = "Create your first project to start tracking labour and attendance.",
                actionButtonText = "Add Project",
                onActionClick = { showAddDialog = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(projectsWithCounts) { item ->
                    ProjectCard(
                        project = item.project,
                        labourCount = item.labourCount,
                        onEdit = { projectToEdit = item.project },
                        onDelete = { projectToDelete = item.project }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditProjectDialog(
            project = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, loc, start, end, _ ->
                viewModel.addProject(name, loc, start, end)
                showAddDialog = false
            }
        )
    }

    projectToEdit?.let { prj ->
        AddEditProjectDialog(
            project = prj,
            onDismiss = { projectToEdit = null },
            onConfirm = { name, loc, start, end, status ->
                viewModel.updateProject(
                    prj.copy(name = name, location = loc, startDate = start, endDate = end, status = status)
                )
                projectToEdit = null
            }
        )
    }

    projectToDelete?.let { prj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project") },
            text = { Text("Are you sure you want to delete \"${prj.name}\"? Assigned labours will need to be reassigned.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(prj.projectId)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProjectCard(
    project: Project,
    labourCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GeoSurface,
        border = BorderStroke(1.dp, GeoBorder),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = GeoTextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = GeoBluePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = project.location.ifBlank { "Site Location" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = GeoTextSecondary
                        )
                    }
                }

                Surface(
                    color = if (project.status == ProjectStatus.ACTIVE.name) GeoGreenContainer else GeoSurfaceVariant,
                    shape = CircleShape
                ) {
                    Text(
                        text = project.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (project.status == ProjectStatus.ACTIVE.name) GeoOnGreenContainer else GeoTextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = GeoBorderLight, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GeoBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = GeoBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$labourCount Labours assigned",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = GeoTextSecondary
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = GeoBluePrimary)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = GeoRedPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditProjectDialog(
    project: Project?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, location: String, start: String, end: String, status: String) -> Unit
) {
    var name by remember { mutableStateOf(project?.name ?: "") }
    var location by remember { mutableStateOf(project?.location ?: "") }
    var startDate by remember { mutableStateOf(project?.startDate ?: DateUtils.getTodayIso()) }
    var endDate by remember { mutableStateOf(project?.endDate ?: "") }
    var status by remember { mutableStateOf(project?.status ?: ProjectStatus.ACTIVE.name) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (project == null) "Add New Project" else "Edit Project") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Project Name *") },
                    placeholder = { Text("e.g. House Construction") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it; error = null },
                    label = { Text("Location *") },
                    placeholder = { Text("e.g. Alwar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End Date (Optional)") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (project != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Status: ", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = status == ProjectStatus.ACTIVE.name,
                            onClick = { status = ProjectStatus.ACTIVE.name },
                            label = { Text("Active") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = status == ProjectStatus.COMPLETED.name,
                            onClick = { status = ProjectStatus.COMPLETED.name },
                            label = { Text("Completed") }
                        )
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || location.isBlank()) {
                        error = "Name and Location are required"
                    } else {
                        onConfirm(name, location, startDate, endDate, status)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==================== LABOUR TAB ====================

@Composable
fun ContractorLabourTab(viewModel: ContractorViewModel) {
    val labours by viewModel.labours.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val pendingReqs by viewModel.pendingRequests.collectAsState()

    val contractorLabours = labours.filter { it.contractorId == viewModel.contractorId }
    var selectedTab by remember { mutableStateOf(0) } // 0: Labours, 1: Requests
    var showAddDialog by remember { mutableStateOf(false) }
    var labourToEdit by remember { mutableStateOf<Labour?>(null) }
    var labourToTransfer by remember { mutableStateOf<Labour?>(null) }

    Scaffold(
        topBar = {
            Column {
                AppTopBar(
                    title = "Labour Management",
                    subtitle = "Manage wages, projects & join requests"
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Approved (${contractorLabours.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Requests")
                                if (pendingReqs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "${pendingReqs.size}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_labour_fab")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Labour")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (selectedTab == 0) {
                if (contractorLabours.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.People,
                        title = "No Labour Yet",
                        message = "Add labour manually or share your Referral Code for them to send join requests.",
                        actionButtonText = "Add Labour",
                        onActionClick = { showAddDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(contractorLabours) { labour ->
                            val assignedPrj = projects.find { it.projectId == labour.projectId }
                            LabourCard(
                                labour = labour,
                                projectName = assignedPrj?.name ?: "No Project",
                                onEdit = { labourToEdit = labour },
                                onTransfer = { labourToTransfer = labour }
                            )
                        }
                    }
                }
            } else {
                // Requests tab
                if (pendingReqs.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.CheckCircle,
                        title = "No Pending Requests",
                        message = "When a labour sends a join request using your Referral Code, it will appear here for approval."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pendingReqs) { req ->
                            JoinRequestCard(
                                request = req,
                                projects = projects.filter { it.contractorId == viewModel.contractorId },
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
    }

    if (showAddDialog) {
        AddEditLabourDialog(
            labour = null,
            projects = projects.filter { it.contractorId == viewModel.contractorId },
            onDismiss = { showAddDialog = false },
            onConfirm = { name, mobile, wage, prjId, _ ->
                viewModel.addLabourManually(name, mobile, wage, prjId)
                showAddDialog = false
            }
        )
    }

    labourToEdit?.let { lab ->
        AddEditLabourDialog(
            labour = lab,
            projects = projects.filter { it.contractorId == viewModel.contractorId },
            onDismiss = { labourToEdit = null },
            onConfirm = { name, mobile, wage, prjId, status ->
                viewModel.updateLabour(
                    lab.copy(name = name, mobile = mobile, dailyWage = wage, projectId = prjId, status = status)
                )
                labourToEdit = null
            }
        )
    }

    labourToTransfer?.let { lab ->
        TransferLabourDialog(
            labour = lab,
            projects = projects.filter { it.contractorId == viewModel.contractorId },
            onDismiss = { labourToTransfer = null },
            onConfirm = { newPrjId ->
                viewModel.transferLabour(lab.labourId, newPrjId)
                labourToTransfer = null
            }
        )
    }
}

@Composable
fun LabourCard(
    labour: Labour,
    projectName: String,
    onEdit: () -> Unit,
    onTransfer: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = GeoSurface,
        border = BorderStroke(1.dp, GeoBorder),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val initials = labour.name.split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .ifEmpty { "L" }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GeoBlueContainer)
                            .border(BorderStroke(1.5.dp, GeoOutline), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = GeoOnBlueContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = labour.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GeoTextPrimary
                        )
                        Text(
                            text = "+91 ${labour.mobile}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GeoTextSecondary
                        )
                    }
                }

                Surface(
                    color = if (labour.status == LabourStatus.ACTIVE.name) GeoGreenContainer else GeoSurfaceVariant,
                    shape = CircleShape
                ) {
                    Text(
                        text = labour.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (labour.status == LabourStatus.ACTIVE.name) GeoOnGreenContainer else GeoTextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = GeoBorderLight, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PROJECT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        ),
                        color = GeoTextSecondary
                    )
                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = GeoTextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "DAILY WAGE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        ),
                        color = GeoTextSecondary
                    )
                    Text(
                        text = CurrencyUtils.formatInrClean(labour.dailyWage),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = GeoBluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = GeoBorderLight, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onTransfer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GeoOutline),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp), tint = GeoBluePrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Transfer Project", fontSize = 12.sp, color = GeoBluePrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = GeoBluePrimary)
                }
            }
        }
    }
}

@Composable
fun JoinRequestCard(
    request: JoinRequest,
    projects: List<Project>,
    onApprove: (projectId: String, dailyWage: Double) -> Unit,
    onReject: () -> Unit
) {
    var showApproveDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = GeoSurface,
        border = BorderStroke(1.dp, GeoBorder),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "New Labour Request",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = request.labourName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Mobile: +91 ${request.labourMobile}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Referral Code Used: ${request.referralCode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reject")
                }
                Button(
                    onClick = { showApproveDialog = true },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Accept")
                }
            }
        }
    }

    if (showApproveDialog) {
        var selectedPrj by remember { mutableStateOf(projects.firstOrNull()?.projectId ?: "") }
        var dailyWageStr by remember { mutableStateOf("600") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            title = { Text("Approve Labour") },
            text = {
                Column {
                    Text("Select Project and set Daily Wage for ${request.labourName}:")
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Select Project:", style = MaterialTheme.typography.labelMedium)
                    projects.forEach { prj ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPrj = prj.projectId }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedPrj == prj.projectId,
                                onClick = { selectedPrj = prj.projectId }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(prj.name)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dailyWageStr,
                        onValueChange = { dailyWageStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Daily Wage (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        val wage = dailyWageStr.toDoubleOrNull() ?: 0.0
                        if (selectedPrj.isBlank()) {
                            error = "Please select a project"
                        } else if (wage <= 0) {
                            error = "Enter a valid wage greater than 0"
                        } else {
                            onApprove(selectedPrj, wage)
                            showApproveDialog = false
                        }
                    }
                ) {
                    Text("Approve Labour")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddEditLabourDialog(
    labour: Labour?,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, mobile: String, wage: Double, prjId: String, status: String) -> Unit
) {
    var name by remember { mutableStateOf(labour?.name ?: "") }
    var mobile by remember { mutableStateOf(labour?.mobile ?: "") }
    var wageStr by remember { mutableStateOf(if (labour != null) "${labour.dailyWage.toInt()}" else "600") }
    var selectedPrj by remember { mutableStateOf(labour?.projectId ?: projects.firstOrNull()?.projectId ?: "") }
    var status by remember { mutableStateOf(labour?.status ?: LabourStatus.ACTIVE.name) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (labour == null) "Add Labour" else "Edit Labour") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Labour Name *") },
                    placeholder = { Text("e.g. Ramesh Kumar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it.filter { c -> c.isDigit() }.take(10); error = null },
                    label = { Text("Mobile Number (10 digits) *") },
                    placeholder = { Text("98XXXXXXXX") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = wageStr,
                    onValueChange = { wageStr = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                    label = { Text("Daily Wage (₹) *") },
                    placeholder = { Text("600") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Assigned Project:", style = MaterialTheme.typography.labelMedium)
                if (projects.isEmpty()) {
                    Text("No projects available. Please add a project first.", color = MaterialTheme.colorScheme.error)
                } else {
                    projects.forEach { prj ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPrj = prj.projectId }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedPrj == prj.projectId,
                                onClick = { selectedPrj = prj.projectId }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(prj.name)
                        }
                    }
                }

                if (labour != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Status: ", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = status == LabourStatus.ACTIVE.name,
                            onClick = { status = LabourStatus.ACTIVE.name },
                            label = { Text("Active") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = status == LabourStatus.INACTIVE.name,
                            onClick = { status = LabourStatus.INACTIVE.name },
                            label = { Text("Inactive") }
                        )
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val wage = wageStr.toDoubleOrNull() ?: 0.0
                    if (name.isBlank()) {
                        error = "Labour Name is required"
                    } else if (mobile.length != 10) {
                        error = "10-digit mobile number is required"
                    } else if (wage <= 0) {
                        error = "Daily wage must be greater than 0"
                    } else if (selectedPrj.isBlank()) {
                        error = "Please select a project"
                    } else {
                        onConfirm(name, mobile, wage, selectedPrj, status)
                    }
                }
            ) {
                Text("Save")
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
fun TransferLabourDialog(
    labour: Labour,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onConfirm: (newProjectId: String) -> Unit
) {
    var selectedNewPrj by remember {
        mutableStateOf(projects.firstOrNull { it.projectId != labour.projectId }?.projectId ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Labour") },
        text = {
            Column {
                Text("Move ${labour.name} to another project.")
                Text(
                    "Note: Historical attendance and payment records will stay preserved under the previous project.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Text("Select New Project:", style = MaterialTheme.typography.labelMedium)
                projects.forEach { prj ->
                    val isCurrent = prj.projectId == labour.projectId
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isCurrent) { selectedNewPrj = prj.projectId }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedNewPrj == prj.projectId,
                            onClick = { if (!isCurrent) selectedNewPrj = prj.projectId },
                            enabled = !isCurrent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCurrent) "${prj.name} (Current)" else prj.name,
                            color = if (isCurrent) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedNewPrj.isNotBlank()) {
                        onConfirm(selectedNewPrj)
                    }
                },
                enabled = selectedNewPrj.isNotBlank()
            ) {
                Text("Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==================== PROFILE TAB ====================

@Composable
fun ContractorProfileTab(
    viewModel: ContractorViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val contractorId = viewModel.contractorId
    val userProfile = viewModel.projects.collectAsState().value.firstOrNull { it.contractorId == contractorId }
    val referralCode = "MUK12345" // default contractor referral
    val contractorName = "Mukesh Sharma"
    val companyName = "Sharma Constructions"
    val mobile = "9876543210"

    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Business,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = contractorName,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = companyName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "+91 $mobile",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Referral Code Card (Section 18)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Labour Referral Code",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Share this code with your labours to let them register and join your projects.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = referralCode,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Referral Code", referralCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Referral Code Copied: $referralCode", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Referral Code", referralCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Referral Code Copied!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Code")
                    }

                    Button(
                        onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Join my construction projects on Labour Attendance Manager! Use my Referral Code: $referralCode"
                                )
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Code")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Options List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                ListItem(
                    headlineContent = { Text("Edit Profile") },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.clickable { showEditDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ListItem(
                    headlineContent = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { showLogoutDialog = true }
                )
            }
        }
    }

    if (showEditDialog) {
        var nameInput by remember { mutableStateOf(contractorName) }
        var companyInput by remember { mutableStateOf(companyName) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Contractor Profile") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Contractor Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = companyInput,
                        onValueChange = { companyInput = it },
                        label = { Text("Company Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(nameInput, companyInput)
                        showEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout of Labour Attendance Manager?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
