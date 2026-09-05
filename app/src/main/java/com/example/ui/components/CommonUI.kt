package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        TopAppBar(
            title = {
                Column {
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 11.sp
                            ),
                            color = GeoTextSecondary
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = GeoTextPrimary
                    )
                }
            },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("top_bar_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GeoTextPrimary
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = GeoSurface
            )
        )
        HorizontalDivider(color = GeoBorder, thickness = 1.dp)
    }
}

/**
 * Geometric Balance Stat Card:
 * Rounded-3xl (24.dp), fixed height (96-104.dp), uppercase tracking label at top,
 * bold prominent number at bottom.
 */
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector? = null,
    containerColor: Color = GeoBlueContainer,
    contentColor: Color = GeoOnBlueContainer,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(102.dp)
            .clip(RoundedCornerShape(24.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = containerColor,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 11.sp
                    ),
                    color = contentColor.copy(alpha = 0.75f)
                )
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = contentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                ),
                color = contentColor
            )
        }
    }
}

/**
 * Geometric Balance Pill Badge:
 * Fully rounded pill (RoundedCornerShape(50)) with uppercase tracking label.
 */
@Composable
fun AttendanceBadge(status: String) {
    val (bgColor, textColor, label) = when (status.uppercase()) {
        AttendanceStatus.PRESENT.name, "PRESENT" -> Triple(GeoGreenContainer, GeoOnGreenContainer, "PRESENT")
        AttendanceStatus.HALF_DAY.name, "HALF DAY", "HALF_DAY" -> Triple(GeoAmberContainer, GeoOnAmberContainer, "HALF DAY")
        AttendanceStatus.ABSENT.name, "ABSENT" -> Triple(GeoRedContainer, GeoOnRedContainer, "ABSENT")
        else -> Triple(GeoSurfaceVariant, GeoTextSecondary, "NOT MARKED")
    }

    Surface(
        color = bgColor,
        shape = CircleShape
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

/**
 * Attendance 3-Button Selector:
 * Geometric rounded-2xl buttons (16.dp) with bold typography and touch target of 48.dp.
 */
@Composable
fun AttendanceSelectorButtons(
    currentStatus: String,
    onStatusSelected: (AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Present button
        val isPresent = currentStatus == AttendanceStatus.PRESENT.name
        OutlinedButton(
            onClick = { onStatusSelected(AttendanceStatus.PRESENT) },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("attendance_btn_present"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isPresent) GeoGreenContainer else GeoSurface,
                contentColor = if (isPresent) GeoOnGreenContainer else GeoTextSecondary
            ),
            border = BorderStroke(
                width = if (isPresent) 1.5.dp else 1.dp,
                color = if (isPresent) GeoOnGreenContainer else GeoOutline
            ),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isPresent) GeoOnGreenContainer else GeoTextSecondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Present",
                fontWeight = if (isPresent) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }

        // Half Day button
        val isHalfDay = currentStatus == AttendanceStatus.HALF_DAY.name
        OutlinedButton(
            onClick = { onStatusSelected(AttendanceStatus.HALF_DAY) },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("attendance_btn_half_day"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isHalfDay) GeoAmberContainer else GeoSurface,
                contentColor = if (isHalfDay) GeoOnAmberContainer else GeoTextSecondary
            ),
            border = BorderStroke(
                width = if (isHalfDay) 1.5.dp else 1.dp,
                color = if (isHalfDay) GeoOnAmberContainer else GeoOutline
            ),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isHalfDay) GeoOnAmberContainer else GeoTextSecondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Half Day",
                fontWeight = if (isHalfDay) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }

        // Absent button
        val isAbsent = currentStatus == AttendanceStatus.ABSENT.name
        OutlinedButton(
            onClick = { onStatusSelected(AttendanceStatus.ABSENT) },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("attendance_btn_absent"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isAbsent) GeoRedContainer else GeoSurface,
                contentColor = if (isAbsent) GeoOnRedContainer else GeoTextSecondary
            ),
            border = BorderStroke(
                width = if (isAbsent) 1.5.dp else 1.dp,
                color = if (isAbsent) GeoOnRedContainer else GeoOutline
            ),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isAbsent) GeoOnRedContainer else GeoTextSecondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Absent",
                fontWeight = if (isAbsent) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Geometric Balance Empty State
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(GeoBlueContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GeoBluePrimary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = GeoTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = GeoTextSecondary,
            textAlign = TextAlign.Center
        )
        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GeoBluePrimary),
                modifier = Modifier.testTag("empty_state_action_btn")
            ) {
                Text(actionButtonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}
