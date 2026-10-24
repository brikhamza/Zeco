package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PriorityLevel
import com.example.data.model.Reminder
import com.example.ui.MainViewModel
import com.example.ui.components.ArchiveNumberBadge
import com.example.ui.components.DatePickerField
import com.example.ui.components.DeadlineBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PriorityBadge
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val reminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val allCorrespondences by viewModel.allCorrespondences.collectAsStateWithLifecycle()
    val overdueItems by viewModel.overdueList.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val startOfDay = DateUtils.getStartOfDay(now)
    val endOfDay = DateUtils.getEndOfOfDay(now)

    // Filter reminders by tab
    val displayedReminders = when (selectedTab) {
        0 -> reminders.filter { !it.isCompleted && it.reminderTime < startOfDay } // Overdue
        1 -> reminders.filter { !it.isCompleted && it.reminderTime in startOfDay..endOfDay } // Today
        2 -> reminders.filter { !it.isCompleted && it.reminderTime > endOfDay } // Upcoming
        3 -> reminders.filter { it.isCompleted } // Completed
        else -> reminders
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("reminders_screen")
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val overdueCount = reminders.count { !it.isCompleted && it.reminderTime < startOfDay } + overdueItems.size
            val todayCount = reminders.count { !it.isCompleted && it.reminderTime in startOfDay..endOfDay }

            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "متأخرة ($overdueCount)",
                        color = if (overdueCount > 0) Color(0xFFDC2626) else Color.Unspecified,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "اليوم ($todayCount)",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Text(
                        text = "القادمة",
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = {
                    Text(
                        text = "المكتملة",
                        fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
            )
        }

        if (displayedReminders.isEmpty() && (selectedTab != 0 || overdueItems.isEmpty())) {
            EmptyStateView(
                icon = Icons.Default.AlarmOn,
                title = "لا توجد مواعيد في هذا القسم",
                message = "جميع المراسلات والمواعيد الإدارية في هذا القسم مستوفاة.",
                actionButtonText = "إضافة موعد أو تذكير",
                onActionClick = { showCreateDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // If on Overdue tab, also show correspondences whose responseDeadline has passed!
                if (selectedTab == 0 && overdueItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "مراسلات رسمية تجاوزت أجل الرد القانوني (${overdueItems.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                    items(overdueItems) { corr ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateToDetail(corr.id) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ArchiveNumberBadge(archiveNumber = corr.archiveNumber)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        DeadlineBadge(deadline = corr.responseDeadline)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = corr.subject,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "الطرف: ${corr.sender} | تاريخ الأجل: ${DateUtils.format(corr.responseDeadline)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                if (displayedReminders.isNotEmpty()) {
                    item {
                        Text(
                            text = "التذكيرات المجدولة",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(displayedReminders, key = { it.id }) { reminder ->
                        val relatedCorr = allCorrespondences.firstOrNull { it.id == reminder.correspondenceId }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (relatedCorr != null) {
                                        viewModel.navigateToDetail(relatedCorr.id)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (reminder.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = reminder.isCompleted,
                                    onCheckedChange = { checked ->
                                        viewModel.toggleReminder(reminder.id, checked)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    if (relatedCorr != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            ArchiveNumberBadge(archiveNumber = relatedCorr.archiveNumber)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            PriorityBadge(priority = reminder.priority)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    Text(
                                        text = reminder.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "الموعد: ${DateUtils.formatDateTime(reminder.reminderTime)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (reminder.notes.isNotBlank()) {
                                        Text(
                                            text = reminder.notes,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteReminder(reminder.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف التذكير",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CREATE REMINDER DIALOG
    if (showCreateDialog) {
        var selectedCorrId by remember { mutableStateOf<Long?>(allCorrespondences.firstOrNull()?.id) }
        var title by remember { mutableStateOf("") }
        var time by remember { mutableStateOf(System.currentTimeMillis() + 86400000L) }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("إنشاء تذكير إداري جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    var corrExpanded by remember { mutableStateOf(false) }
                    val currentCorr = allCorrespondences.firstOrNull { it.id == selectedCorrId }

                    ExposedDropdownMenuBox(
                        expanded = corrExpanded,
                        onExpandedChange = { corrExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentCorr?.let { "${it.archiveNumber} - ${it.subject}" } ?: "اختر المراسلة...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("المراسلة المعنية *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = corrExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = corrExpanded,
                            onDismissRequest = { corrExpanded = false }
                        ) {
                            allCorrespondences.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.archiveNumber} - ${c.subject}") },
                                    onClick = {
                                        selectedCorrId = c.id
                                        if (title.isBlank()) {
                                            title = "متابعة مراسلة: ${c.subject}"
                                        }
                                        corrExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان التذكير *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DatePickerField(
                        label = "تاريخ الاستحقاق",
                        timestamp = time,
                        onDateSelected = { time = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val corr = allCorrespondences.firstOrNull { it.id == selectedCorrId }
                        if (corr != null && title.isNotBlank()) {
                            viewModel.addReminder(
                                Reminder(
                                    correspondenceId = corr.id,
                                    title = title,
                                    reminderTime = time,
                                    notes = notes
                                ),
                                corr.archiveNumber,
                                corr.subject
                            )
                            showCreateDialog = false
                        }
                    },
                    enabled = selectedCorrId != null && title.isNotBlank()
                ) {
                    Text("حفظ التذكير")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
