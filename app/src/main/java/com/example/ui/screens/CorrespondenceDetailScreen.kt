package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Attachment
import com.example.data.model.ConfidentialityLevel
import com.example.data.model.Correspondence
import com.example.data.model.CorrespondenceDetailView
import com.example.data.model.CorrespondenceStatus
import com.example.data.model.CorrespondenceType
import com.example.data.model.PriorityLevel
import com.example.data.model.RelationType
import com.example.data.model.Reminder
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.ArchiveNumberBadge
import com.example.ui.components.AttachmentItemRow
import com.example.ui.components.ConfidentialityBadge
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.CorrespondenceTypeBadge
import com.example.ui.components.DatePickerField
import com.example.ui.components.DeadlineBadge
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.util.DateUtils
import com.example.util.FileUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorrespondenceDetailScreen(
    correspondenceId: Long,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(correspondenceId) {
        viewModel.selectCorrespondence(correspondenceId)
    }

    val detailView by viewModel.selectedDetail.collectAsStateWithLifecycle()
    val allCorrespondences by viewModel.allCorrespondences.collectAsStateWithLifecycle()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deleteWarningMessage by remember { mutableStateOf("") }
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var showAddFollowUpDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showStatusChangeDialog by remember { mutableStateOf(false) }

    // File picker for adding more attachments to existing record
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        coroutineScope.launch {
            for (uri in uris) {
                val fileInfo = FileUtils.importUriToInternalStorage(context, uri)
                if (fileInfo != null) {
                    val att = Attachment(
                        correspondenceId = correspondenceId,
                        fileName = fileInfo.originalName,
                        filePath = fileInfo.savedRelativePath,
                        fileType = fileInfo.attachmentType,
                        mimeType = fileInfo.mimeType,
                        fileSize = fileInfo.fileSize
                    )
                    viewModel.saveCorrespondence(
                        correspondence = detailView!!.correspondence,
                        newAttachments = listOf(att),
                        initialLinkedId = null,
                        initialRelationType = RelationType.RELATED_TO
                    ) {}
                }
            }
        }
    }

    if (detailView == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("جاري تحميل تفاصيل المراسلة...")
        }
        return
    }

    val corr = detailView!!.correspondence
    val attachments = detailView!!.attachments
    val relations = detailView!!.relations
    val followUps = detailView!!.followUps
    val reminders = detailView!!.reminders

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("correspondence_detail_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArchiveNumberBadge(archiveNumber = corr.archiveNumber)
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ArchiveNumber", corr.archiveNumber))
                        Toast.makeText(context, "تم نسخ رقم الأرشيف", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ رقم الأرشيف",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row {
                IconButton(
                    onClick = { viewModel.navigateToEdit(corr.id) },
                    modifier = Modifier.testTag("edit_corr_btn")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            val (attCount, relCount) = viewModel.getDeletableWarning(corr.id)
                            deleteWarningMessage = buildString {
                                append("هل أنت متأكد من حذف هذه المراسلة نهائياً؟\n")
                                if (attCount > 0) append("• تحتوي على $attCount ملف مرفق سيتم حذفها.\n")
                                if (relCount > 0) append("• مرتبطة بـ $relCount مراسلة أخرى سيتم فك الارتباط عنها بأمان.")
                            }
                            showDeleteConfirmDialog = true
                        }
                    },
                    modifier = Modifier.testTag("delete_corr_btn")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Confidentiality Banner if Confidential
        if (corr.confidentiality != ConfidentialityLevel.NORMAL) {
            Surface(
                color = if (corr.confidentiality == ConfidentialityLevel.HIGHLY_CONFIDENTIAL) Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (corr.confidentiality == ConfidentialityLevel.HIGHLY_CONFIDENTIAL) Color(0xFFDC2626) else Color(0xFFD97706)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تحذير: هذه المراسلة مصنفة كـ [${corr.confidentiality.labelAr}]، يرجى مراعاة سرية الوثائق والبيانات الإدارية.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (corr.confidentiality == ConfidentialityLevel.HIGHLY_CONFIDENTIAL) Color(0xFF991B1B) else Color(0xFF92400E)
                    )
                }
            }
        }

        // MAIN INFORMATION CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type & Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CorrespondenceTypeBadge(type = corr.type)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showStatusChangeDialog = true }
                    ) {
                        StatusBadge(status = corr.status)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تغيير الحالة",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Subject
                Text(
                    text = corr.subject,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (corr.summary.isNotBlank()) {
                    Text(
                        text = corr.summary,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                // Administrative Data Table
                InfoRow(label = "المرسل:", value = corr.sender)
                InfoRow(label = "المستقبل:", value = corr.recipient)
                if (corr.department.isNotBlank()) InfoRow(label = "المصلحة المكلفة:", value = corr.department)
                if (corr.responsibleEmployee.isNotBlank()) InfoRow(label = "الموظف المسؤول:", value = corr.responsibleEmployee)
                if (corr.category.isNotBlank()) InfoRow(label = "التصنيف الإداري:", value = corr.category)
                if (corr.externalReference.isNotBlank()) InfoRow(label = "الرقم الإشاري الخارجي:", value = corr.externalReference)
                InfoRow(label = "تاريخ المراسلة:", value = DateUtils.format(corr.correspondenceDate))
                InfoRow(label = "تاريخ الاستلام / الوصول:", value = DateUtils.format(corr.receivedDate))
                InfoRow(label = "تاريخ التسجيل بالأرشيف:", value = DateUtils.formatDateTime(corr.registeredDate))

                if (corr.requiredAction.isNotBlank() || corr.responseDeadline != null) {
                    HorizontalDivider()
                    Text(
                        text = "التوجيه والآجال",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (corr.requiredAction.isNotBlank()) InfoRow(label = "الإجراء المطلوب:", value = corr.requiredAction)
                    if (corr.assignedOffice.isNotBlank()) InfoRow(label = "المكتب المكلف:", value = corr.assignedOffice)
                    if (corr.responseDeadline != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("أجل الرد القانوني:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(DateUtils.format(corr.responseDeadline), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                DeadlineBadge(deadline = corr.responseDeadline)
                            }
                        }
                    }
                }

                if (corr.internalNotes.isNotBlank()) {
                    HorizontalDivider()
                    InfoRow(label = "ملاحظات داخلية:", value = corr.internalNotes)
                }

                if (corr.keywords.isNotBlank()) {
                    InfoRow(label = "الوسوم والكلمات المفتاحية:", value = corr.keywords)
                }
            }
        }

        // RELATED CORRESPONDENCE SECTION (CRITICAL FEATURE)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DeviceHub, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "المراسلات والوثائق المرتبطة (${relations.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row {
                        if (relations.isNotEmpty()) {
                            IconButton(onClick = { viewModel.navigateTo(Screen.RelationsTimeline(corr.id)) }) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = "عرض التسلسل الزمني للملف",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(
                            onClick = { showAddLinkDialog = true },
                            modifier = Modifier.testTag("add_link_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة رابط مراسلة", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (relations.isEmpty()) {
                    Text(
                        text = "لا توجد مراسلات مرتبطة بهذا السجل. يمكنك ربط هذا الخطاب بردوده أو استفساراته أو بملفات إدارية سابقة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    relations.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateToDetail(item.linkedCorrespondenceId) }
                                .testTag("linked_item_${item.linkedCorrespondenceId}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (item.isSource) item.relationType.labelAr else item.relationType.inverseLabelAr,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        ArchiveNumberBadge(archiveNumber = item.linkedArchiveNumber)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        CorrespondenceTypeBadge(type = item.linkedType)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.linkedSubject,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "الطرف: ${item.linkedSender} | التاريخ: ${DateUtils.format(item.linkedDate)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (item.notes.isNotBlank()) {
                                        Text(
                                            text = "ملاحظة الربط: ${item.notes}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteRelation(item.relationId) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "فك الارتباط",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ATTACHMENTS & DIGITAL ARCHIVE SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "المرفقات والوثائق الرقمية (${attachments.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            filePicker.launch(
                                arrayOf(
                                    "application/pdf",
                                    "image/*",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "application/vnd.ms-excel",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                )
                            )
                        },
                        modifier = Modifier.testTag("add_attachment_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة مرفق", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (attachments.isEmpty()) {
                    Text(
                        text = "لا توجد ملفات مرفقة بهذه المراسلة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    attachments.forEach { att ->
                        AttachmentItemRow(
                            attachment = att,
                            onOpenExternal = {
                                FileUtils.openFileWithExternalApp(context, att.filePath, att.mimeType)
                            },
                            onDelete = {
                                viewModel.deleteAttachment(att)
                            }
                        )
                    }
                }
            }
        }

        // REMINDERS & DEADLINES FOR THIS CORRESPONDENCE
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "التذكيرات والمواعيد (${reminders.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showAddReminderDialog = true },
                        modifier = Modifier.testTag("add_reminder_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة تذكير", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (reminders.isEmpty()) {
                    Text(
                        text = "لا توجد تذكيرات مجدولة لهذه المراسلة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    reminders.forEach { reminder ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (reminder.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.toggleReminder(reminder.id, !reminder.isCompleted) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.Alarm,
                                            contentDescription = null,
                                            tint = if (reminder.isCompleted) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = reminder.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "الموعد: ${DateUtils.formatDateTime(reminder.reminderTime)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteReminder(reminder.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FOLLOW-UP HISTORY & AUDIT TRAIL
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "سجل التتبع والمتابعة الإدارية (${followUps.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    FilledTonalButton(
                        onClick = { showAddFollowUpDialog = true },
                        modifier = Modifier.testTag("add_followup_btn")
                    ) {
                        Text("تسجيل إجراء", fontSize = 12.sp)
                    }
                }

                followUps.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.actionTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = DateUtils.formatDateTime(item.actionDate),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (item.actionDescription.isNotBlank()) {
                                Text(
                                    text = item.actionDescription,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!item.recordedBy.isNullOrBlank()) {
                                Text(
                                    text = "بواسطة: ${item.recordedBy}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // DIALOG: Confirm Delete Record
    ConfirmationDialog(
        show = showDeleteConfirmDialog,
        title = "تأكيد حذف المراسلة",
        message = deleteWarningMessage,
        confirmText = "حذف نهائي",
        isDestructive = true,
        onConfirm = {
            viewModel.deleteCorrespondence(corr.id) {
                viewModel.navigateBack()
            }
        },
        onDismiss = { showDeleteConfirmDialog = false }
    )

    // DIALOG: Change Status
    if (showStatusChangeDialog) {
        var selectedNewStatus by remember { mutableStateOf(corr.status) }
        var statusNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showStatusChangeDialog = false },
            title = { Text("تحديث حالة معالجة المراسلة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CorrespondenceStatus.entries.forEach { st ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedNewStatus == st) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedNewStatus = st }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(status = st)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = statusNote,
                        onValueChange = { statusNote = it },
                        label = { Text("سبب التغيير أو ملاحظات إضافية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateStatus(corr.id, selectedNewStatus, statusNote)
                    showStatusChangeDialog = false
                }) {
                    Text("حفظ الحالة")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showStatusChangeDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // DIALOG: Add Relation / Link
    if (showAddLinkDialog) {
        var targetId by remember { mutableStateOf<Long?>(null) }
        var relType by remember { mutableStateOf(RelationType.RELATED_TO) }
        var relNotes by remember { mutableStateOf("") }
        val candidates = allCorrespondences.filter { it.id != corr.id }

        AlertDialog(
            onDismissRequest = { showAddLinkDialog = false },
            title = { Text("ربط بمراسلة إدارية أخرى", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    var candidateExpanded by remember { mutableStateOf(false) }
                    val currentTarget = candidates.firstOrNull { it.id == targetId }

                    ExposedDropdownMenuBox(
                        expanded = candidateExpanded,
                        onExpandedChange = { candidateExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentTarget?.let { "${it.archiveNumber} - ${it.subject}" } ?: "اختر المراسلة المستهدفة...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("المراسلة الأخرى *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = candidateExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = candidateExpanded,
                            onDismissRequest = { candidateExpanded = false }
                        ) {
                            candidates.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.archiveNumber} - ${c.subject}") },
                                    onClick = {
                                        targetId = c.id
                                        candidateExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    var relTypeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = relTypeExpanded,
                        onExpandedChange = { relTypeExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = relType.labelAr,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("نوع العلاقة الإدارية") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = relTypeExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = relTypeExpanded,
                            onDismissRequest = { relTypeExpanded = false }
                        ) {
                            RelationType.entries.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.labelAr) },
                                    onClick = {
                                        relType = r
                                        relTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = relNotes,
                        onValueChange = { relNotes = it },
                        label = { Text("ملاحظة أو تبرير الربط") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetId != null) {
                            viewModel.linkCorrespondence(corr.id, targetId!!, relType, relNotes)
                            showAddLinkDialog = false
                        }
                    },
                    enabled = targetId != null
                ) {
                    Text("تأكيد الربط")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddLinkDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // DIALOG: Add Follow-up Action
    if (showAddFollowUpDialog) {
        var actionTitle by remember { mutableStateOf("") }
        var actionDesc by remember { mutableStateOf("") }
        var author by remember { mutableStateOf(corr.responsibleEmployee) }

        AlertDialog(
            onDismissRequest = { showAddFollowUpDialog = false },
            title = { Text("تسجيل إجراء متابعة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = actionTitle,
                        onValueChange = { actionTitle = it },
                        label = { Text("عنوان الإجراء *") },
                        placeholder = { Text("مثال: توجيه للمصلحة المعنية، استدعاء، رد أولي") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = actionDesc,
                        onValueChange = { actionDesc = it },
                        label = { Text("تفاصيل الإجراء والملاحظات") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("الموظف المسجل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (actionTitle.isNotBlank()) {
                            viewModel.addFollowUp(corr.id, actionTitle, actionDesc, author)
                            showAddFollowUpDialog = false
                        }
                    },
                    enabled = actionTitle.isNotBlank()
                ) {
                    Text("حفظ الإجراء")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddFollowUpDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // DIALOG: Add Reminder
    if (showAddReminderDialog) {
        var reminderTitle by remember { mutableStateOf("متابعة الرد على المراسلة رقم ${corr.archiveNumber}") }
        var reminderTime by remember { mutableStateOf(corr.responseDeadline ?: (System.currentTimeMillis() + 86400000L)) }
        var advanceDays by remember { mutableStateOf(1) }
        var reminderNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddReminderDialog = false },
            title = { Text("جدولة تذكير إداري", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = reminderTitle,
                        onValueChange = { reminderTitle = it },
                        label = { Text("عنوان التذكير *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DatePickerField(
                        label = "تاريخ ووقت الاستحقاق",
                        timestamp = reminderTime,
                        onDateSelected = { reminderTime = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reminderNotes,
                        onValueChange = { reminderNotes = it },
                        label = { Text("ملاحظات إضافية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reminderTitle.isNotBlank()) {
                            val r = Reminder(
                                correspondenceId = corr.id,
                                title = reminderTitle,
                                reminderTime = reminderTime,
                                advanceNoticeDays = advanceDays,
                                priority = corr.priority,
                                notes = reminderNotes
                            )
                            viewModel.addReminder(r, corr.archiveNumber, corr.subject)
                            showAddReminderDialog = false
                        }
                    },
                    enabled = reminderTitle.isNotBlank()
                ) {
                    Text("جدولة التذكير")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddReminderDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
