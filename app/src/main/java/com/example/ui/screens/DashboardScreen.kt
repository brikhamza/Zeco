package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Correspondence
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.ArchiveNumberBadge
import com.example.ui.components.CorrespondenceItemCard
import com.example.ui.components.CorrespondenceTypeBadge
import com.example.ui.components.DeadlineBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.util.DateUtils

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val upcomingDeadlines by viewModel.upcomingDeadlines.collectAsStateWithLifecycle()
    val showHijri by viewModel.showHijriDate.collectAsStateWithLifecycle()
    val institutionName by viewModel.institutionName.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Institutional Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = institutionName.ifBlank { "الجمهورية الجزائرية الديمقراطية الشعبية" },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "نظام الأرشيف والمراسلات الإدارية",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تاريخ اليوم: ${DateUtils.format(System.currentTimeMillis())}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                        if (showHijri) {
                            Text(
                                text = DateUtils.toHijri(System.currentTimeMillis()),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Overdue & Due Today Warning Banner
        if (stats.overdueCount > 0 || stats.dueTodayCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(Screen.Reminders) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDC2626).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationImportant,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تنبيه مواعيد وآجال هامة!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            val details = buildString {
                                if (stats.overdueCount > 0) append("${stats.overdueCount} مراسلة متأخرة تجاوزت أجل الرد ")
                                if (stats.dueTodayCount > 0) {
                                    if (isNotEmpty()) append("• ")
                                    append("${stats.dueTodayCount} مراسلة يحل أجلها اليوم")
                                }
                            }
                            Text(
                                text = details,
                                fontSize = 12.sp,
                                color = Color(0xFFB91C1C)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color(0xFFDC2626)
                        )
                    }
                }
            }
        }

        // Primary Quick Actions
        item {
            Column {
                Text(
                    text = "الإجراءات السريعة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.navigateToAdd() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_correspondence_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل مراسلة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.navigateTo(Screen.Search) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بحث متقدم", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionChipButton(
                        text = "التذكيرات والآجال",
                        icon = Icons.Default.Alarm,
                        onClick = { viewModel.navigateTo(Screen.Reminders) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionChipButton(
                        text = "التقارير",
                        icon = Icons.Default.Assessment,
                        onClick = { viewModel.navigateTo(Screen.Reports) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionChipButton(
                        text = "سجل المراسلات",
                        icon = Icons.Default.Folder,
                        onClick = { viewModel.navigateTo(Screen.CorrespondenceList) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Live Statistics Grid
        item {
            Column {
                Text(
                    text = "إحصائيات الأرشيف",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Row 1: Total & Requiring Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "إجمالي المحفوظات",
                        value = "${stats.totalCount}",
                        icon = Icons.Default.Folder,
                        color = Color(0xFF0F3E61),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_total_card"),
                        onClick = {
                            viewModel.setListTab(0)
                            viewModel.navigateTo(Screen.CorrespondenceList)
                        }
                    )
                    StatCard(
                        title = "تتطلب إجراء",
                        value = "${stats.requiringActionCount}",
                        icon = Icons.Default.PendingActions,
                        color = Color(0xFFD97706),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_requiring_action_card"),
                        onClick = {
                            viewModel.setListTab(4)
                            viewModel.navigateTo(Screen.CorrespondenceList)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 2: Incoming, Outgoing, Internal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "بريد وارد",
                        value = "${stats.incomingCount}",
                        icon = Icons.Default.CallReceived,
                        color = Color(0xFF0284C7),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_incoming_card"),
                        onClick = {
                            viewModel.setListTab(1)
                            viewModel.navigateTo(Screen.CorrespondenceList)
                        }
                    )
                    StatCard(
                        title = "بريد صادر",
                        value = "${stats.outgoingCount}",
                        icon = Icons.Default.CallMade,
                        color = Color(0xFF0D9488),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_outgoing_card"),
                        onClick = {
                            viewModel.setListTab(2)
                            viewModel.navigateTo(Screen.CorrespondenceList)
                        }
                    )
                    StatCard(
                        title = "داخلي",
                        value = "${stats.internalCount}",
                        icon = Icons.Default.CompareArrows,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_internal_card"),
                        onClick = {
                            viewModel.setListTab(3)
                            viewModel.navigateTo(Screen.CorrespondenceList)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 3: Deadlines & Overdue
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "متأخرة",
                        value = "${stats.overdueCount}",
                        icon = Icons.Default.Warning,
                        color = Color(0xFFDC2626),
                        subText = "تجاوزت الأجل",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_overdue_card"),
                        onClick = {
                            viewModel.setListTab(5)
                            viewModel.navigateTo(Screen.CorrespondenceList)
                        }
                    )
                    StatCard(
                        title = "أجلها اليوم",
                        value = "${stats.dueTodayCount}",
                        icon = Icons.Default.HourglassTop,
                        color = Color(0xFFB45309),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_due_today_card"),
                        onClick = { viewModel.navigateTo(Screen.Reminders) }
                    )
                    StatCard(
                        title = "الـ 7 أيام القادمة",
                        value = "${stats.dueNextSevenDaysCount}",
                        icon = Icons.Default.DateRange,
                        color = Color(0xFF2563EB),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_due_7days_card"),
                        onClick = { viewModel.navigateTo(Screen.Reminders) }
                    )
                }
            }
        }

        // Upcoming Deadlines Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المواعيد والآجال القادمة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { viewModel.navigateTo(Screen.Reminders) }) {
                    Text("عرض الكل (${upcomingDeadlines.size})", fontSize = 12.sp)
                }
            }
        }

        if (upcomingDeadlines.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "لا توجد مواعيد رد أو متابعة قريبة مستحقة.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(upcomingDeadlines.take(3)) { item ->
                UpcomingDeadLineRow(
                    correspondence = item,
                    onClick = { viewModel.navigateToDetail(item.id) }
                )
            }
        }

        // Recently Added Correspondence Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أحدث المراسلات المسجلة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { viewModel.navigateTo(Screen.CorrespondenceList) }) {
                    Text("سجل المراسلات", fontSize = 12.sp)
                }
            }
        }

        if (recentlyAdded.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.Folder,
                    title = "الأرشيف فارغ حالياً",
                    message = "ابدأ بتسجيل أول مراسلة إدارية في السجل الرسمي",
                    actionButtonText = "تسجيل مراسلة جديدة",
                    onActionClick = { viewModel.navigateToAdd() }
                )
            }
        } else {
            items(recentlyAdded) { item ->
                CorrespondenceItemCard(
                    correspondence = item,
                    onClick = { viewModel.navigateToDetail(item.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun ActionChipButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun UpcomingDeadLineRow(
    correspondence: Correspondence,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArchiveNumberBadge(archiveNumber = correspondence.archiveNumber)
                    Spacer(modifier = Modifier.width(6.dp))
                    CorrespondenceTypeBadge(type = correspondence.type)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = correspondence.subject,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                if (correspondence.requiredAction.isNotBlank()) {
                    Text(
                        text = "الإجراء: ${correspondence.requiredAction}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            DeadlineBadge(deadline = correspondence.responseDeadline)
        }
    }
}
