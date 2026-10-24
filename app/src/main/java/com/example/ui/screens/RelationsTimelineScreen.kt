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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.model.CorrespondenceType
import com.example.data.model.RelationType
import com.example.ui.MainViewModel
import com.example.ui.components.ArchiveNumberBadge
import com.example.ui.components.CorrespondenceTypeBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationsTimelineScreen(
    caseCorrespondenceId: Long?,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allCorrespondences by viewModel.allCorrespondences.collectAsStateWithLifecycle()
    var selectedRootId by remember { mutableStateOf(caseCorrespondenceId ?: allCorrespondences.firstOrNull()?.id) }

    LaunchedEffect(caseCorrespondenceId) {
        if (caseCorrespondenceId != null) {
            selectedRootId = caseCorrespondenceId
        }
    }

    LaunchedEffect(allCorrespondences) {
        if (selectedRootId == null && allCorrespondences.isNotEmpty()) {
            selectedRootId = allCorrespondences.first().id
        }
    }

    val selectedCorr = allCorrespondences.firstOrNull { it.id == selectedRootId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("relations_timeline_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selector for Base Correspondence
        var selectorExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = selectorExpanded,
            onExpandedChange = { selectorExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCorr?.let { "${it.archiveNumber} - ${it.subject}" } ?: "اختر ملفاً إدارياً...",
                onValueChange = {},
                readOnly = true,
                label = { Text("الملف / المراسلة المرجعية") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = selectorExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(
                expanded = selectorExpanded,
                onDismissRequest = { selectorExpanded = false }
            ) {
                allCorrespondences.forEach { c ->
                    DropdownMenuItem(
                        text = { Text("${c.archiveNumber} - ${c.subject}") },
                        onClick = {
                            selectedRootId = c.id
                            selectorExpanded = false
                        }
                    )
                }
            }
        }

        if (selectedCorr == null) {
            EmptyStateView(
                icon = Icons.Default.DeviceHub,
                title = "لا توجد مراسلات مختارة",
                message = "اختر مراسلة لعرض شجرة العلاقات والتسلسل الزمني للملف الإداري."
            )
        } else {
            // Timeline Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "التسلسل الزمني للملف الإداري",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FilledTonalButton(
                            onClick = {
                                viewModel.navigateToAdd(
                                    initialLinkedId = selectedCorr.id,
                                    initialRelationType = if (selectedCorr.type == CorrespondenceType.INCOMING) RelationType.REPLY_TO else RelationType.FOLLOW_UP_TO
                                )
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedCorr.type == CorrespondenceType.INCOMING) "تسجيل رد صادر" else "تسجيل مراسلة لاحقة",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Node 1: Root Correspondence
                item {
                    TimelineNodeCard(
                        correspondence = selectedCorr,
                        stepTitle = "المراسلة المرجعية",
                        isRoot = true,
                        onClick = { viewModel.navigateToDetail(selectedCorr.id) }
                    )
                }

                // Connector
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(20.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Related correspondence nodes in order
                val otherItems = allCorrespondences.filter { it.id != selectedCorr.id }
                if (otherItems.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "لم يتم تسجيل ردود أو مراسلات لاحقة مرتبطة بهذا الملف بعد.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(otherItems.take(5)) { relCorr ->
                        TimelineNodeCard(
                            correspondence = relCorr,
                            stepTitle = if (relCorr.type == CorrespondenceType.OUTGOING) "رد إداري صادر" else "مراسلة مرتبطة",
                            isRoot = false,
                            onClick = { viewModel.navigateToDetail(relCorr.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineNodeCard(
    correspondence: Correspondence,
    stepTitle: String,
    isRoot: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isRoot) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = stepTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = DateUtils.format(correspondence.correspondenceDate),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ArchiveNumberBadge(archiveNumber = correspondence.archiveNumber)
                Spacer(modifier = Modifier.width(6.dp))
                CorrespondenceTypeBadge(type = correspondence.type)
                Spacer(modifier = Modifier.width(6.dp))
                StatusBadge(status = correspondence.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = correspondence.subject,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "من: ${correspondence.sender} ➔ إلى: ${correspondence.recipient}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (correspondence.requiredAction.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الإجراء: ${correspondence.requiredAction}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
