package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ArchiveApplication
import com.example.data.model.Attachment
import com.example.data.model.ConfidentialityLevel
import com.example.data.model.Correspondence
import com.example.data.model.CorrespondenceStatus
import com.example.data.model.CorrespondenceType
import com.example.data.model.PriorityLevel
import com.example.data.model.RelationType
import com.example.ui.MainViewModel
import com.example.ui.components.DatePickerField
import com.example.util.DateUtils
import com.example.util.FileUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCorrespondenceScreen(
    correspondenceId: Long?,
    initialLinkedId: Long?,
    initialRelationType: RelationType?,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isEditMode = correspondenceId != null && correspondenceId > 0

    // Stored directories
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val allCorrespondences by viewModel.allCorrespondences.collectAsStateWithLifecycle()

    // Form Fields State
    var archiveNumberPreview by remember { mutableStateOf("") }
    var year by remember { mutableStateOf(DateUtils.getCurrentYear()) }
    var type by remember { mutableStateOf(CorrespondenceType.INCOMING) }
    var subject by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("مراسلة رسمية") }
    var confidentiality by remember { mutableStateOf(ConfidentialityLevel.NORMAL) }

    var externalReference by remember { mutableStateOf("") }
    var correspondenceDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var receivedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var sender by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var responsibleEmployee by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(CorrespondenceStatus.NEW) }
    var priority by remember { mutableStateOf(PriorityLevel.NORMAL) }

    var requiredAction by remember { mutableStateOf("") }
    var assignedOffice by remember { mutableStateOf("") }
    var responseDeadline by remember { mutableStateOf<Long?>(null) }
    var nextFollowUpDate by remember { mutableStateOf<Long?>(null) }
    var internalNotes by remember { mutableStateOf("") }

    // Staged Attachments
    val stagedAttachments = remember { mutableStateListOf<Attachment>() }

    // Initial Linking State
    var selectedLinkedId by remember { mutableStateOf(initialLinkedId) }
    var selectedRelationType by remember { mutableStateOf(initialRelationType ?: RelationType.RELATED_TO) }

    // Form Validation Error
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // File Picker for attaching files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        coroutineScope.launch {
            for (uri in uris) {
                val fileInfo = FileUtils.importUriToInternalStorage(context, uri)
                if (fileInfo != null) {
                    stagedAttachments.add(
                        Attachment(
                            correspondenceId = correspondenceId ?: 0L,
                            fileName = fileInfo.originalName,
                            filePath = fileInfo.savedRelativePath,
                            fileType = fileInfo.attachmentType,
                            mimeType = fileInfo.mimeType,
                            fileSize = fileInfo.fileSize
                        )
                    )
                }
            }
        }
    }

    // Load existing record if in Edit mode
    LaunchedEffect(correspondenceId) {
        if (isEditMode) {
            val repository = (context.applicationContext as ArchiveApplication).repository
            val dao = (context.applicationContext as ArchiveApplication).database.correspondenceDao()
            val existing = dao.getById(correspondenceId!!)
            if (existing != null) {
                archiveNumberPreview = existing.archiveNumber
                year = existing.year
                type = existing.type
                subject = existing.subject
                summary = existing.summary
                keywords = existing.keywords
                category = existing.category
                confidentiality = existing.confidentiality
                externalReference = existing.externalReference
                correspondenceDate = existing.correspondenceDate
                receivedDate = existing.receivedDate
                sender = existing.sender
                recipient = existing.recipient
                department = existing.department
                responsibleEmployee = existing.responsibleEmployee
                status = existing.status
                priority = existing.priority
                requiredAction = existing.requiredAction
                assignedOffice = existing.assignedOffice
                responseDeadline = existing.responseDeadline
                nextFollowUpDate = existing.nextFollowUpDate
                internalNotes = existing.internalNotes
            }
        } else {
            val repository = (context.applicationContext as ArchiveApplication).repository
            val (previewNum, _) = repository.generateNextArchiveNumber()
            archiveNumberPreview = previewNum
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("add_edit_correspondence_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isEditMode) "تعديل المراسلة الإدارية" else "تسجيل مراسلة جديدة",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isEditMode) "رقم الأرشيف الداخلي: $archiveNumberPreview (ثابت لا يتغير)"
                    else "سيتم تعيين رقم الأرشيف التلقائي: $archiveNumberPreview",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // SECTION 1: Identification & Classification
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "1. التعريف والتصنيف الإداري",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Correspondence Type Selector
                Column {
                    Text("نوع المراسلة *", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CorrespondenceType.entries.forEach { cType ->
                            val isSelected = type == cType
                            OutlinedButton(
                                onClick = { type = cType },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("type_${cType.name}"),
                                colors = if (isSelected) {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                }
                            ) {
                                Text(cType.labelAr, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                // Subject
                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("الموضوع / عنوان المراسلة *") },
                    placeholder = { Text("مثال: طلب رخصة بناء تجزئة رقم 45") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_input")
                )

                // Summary
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("ملخص أو فحوى المراسلة") },
                    placeholder = { Text("موجز عن محتوى الوثيقة والغرض منها...") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category & Confidentiality Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var categoryExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("التصنيف الإداري") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        category = cat.name
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    var confExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = confExpanded,
                        onExpandedChange = { confExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = confidentiality.labelAr,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("درجة السرية") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = confExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = confExpanded,
                            onDismissRequest = { confExpanded = false }
                        ) {
                            ConfidentialityLevel.entries.forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(level.labelAr) },
                                    onClick = {
                                        confidentiality = level
                                        confExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Keywords / Tags
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("الكلمات المفتاحية والوسوم (مفصولة بفواصل)") },
                    placeholder = { Text("مثال: صفقات, ميزانية_2026, استعجالي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // SECTION 2: Administrative Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "2. البيانات الإدارية وأطراف المراسلة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // External Reference
                OutlinedTextField(
                    value = externalReference,
                    onValueChange = { externalReference = it },
                    label = { Text("الرقم الإشاري الخارجي (إن وجد)") },
                    placeholder = { Text("رقم الخطاب الأصلي الصادر من الجهة الأخرى") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Dates Row: Correspondence Date & Received Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DatePickerField(
                        label = "تاريخ المراسلة *",
                        timestamp = correspondenceDate,
                        onDateSelected = { correspondenceDate = it },
                        modifier = Modifier.weight(1f)
                    )
                    DatePickerField(
                        label = "تاريخ الاستلام / التسجيل",
                        timestamp = receivedDate,
                        onDateSelected = { receivedDate = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Sender & Recipient
                OutlinedTextField(
                    value = sender,
                    onValueChange = {
                        sender = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("الجهة أو الشخص المرسل *") },
                    placeholder = { Text("مثال: وزارة الداخلية والجماعات المحلية") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sender_input")
                )

                OutlinedTextField(
                    value = recipient,
                    onValueChange = {
                        recipient = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("الجهة أو الشخص المستقبل *") },
                    placeholder = { Text("مثال: مديرية الإدارة المحلية") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recipient_input")
                )

                // Department & Responsible Employee
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var deptExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = deptExpanded,
                        onExpandedChange = { deptExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text("المصلحة / المكتب المسؤول") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = deptExpanded,
                            onDismissRequest = { deptExpanded = false }
                        ) {
                            departments.forEach { dept ->
                                DropdownMenuItem(
                                    text = { Text(dept.name) },
                                    onClick = {
                                        department = dept.name
                                        deptExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = responsibleEmployee,
                        onValueChange = { responsibleEmployee = it },
                        label = { Text("الموظف المكلف") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Status & Priority Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var statusExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = status.labelAr,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("حالة المعالجة") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            CorrespondenceStatus.entries.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st.labelAr) },
                                    onClick = {
                                        status = st
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    var priorityExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = priorityExpanded,
                        onExpandedChange = { priorityExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = priority.labelAr,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("درجة الأولوية") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = priorityExpanded,
                            onDismissRequest = { priorityExpanded = false }
                        ) {
                            PriorityLevel.entries.forEach { pr ->
                                DropdownMenuItem(
                                    text = { Text(pr.labelAr) },
                                    onClick = {
                                        priority = pr
                                        priorityExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 3: Follow-Up & Deadlines
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "3. المتابعة والتوجيه والآجال القانونية",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = requiredAction,
                    onValueChange = { requiredAction = it },
                    label = { Text("الإجراء المطلوب / توجيه المسؤول") },
                    placeholder = { Text("مثال: إعداد تقرير مفصل والرد قبل نهاية الشهر") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DatePickerField(
                        label = "أجل الرد القانوني",
                        timestamp = responseDeadline,
                        onDateSelected = { responseDeadline = it },
                        allowClear = true,
                        onClear = { responseDeadline = null },
                        modifier = Modifier.weight(1f)
                    )
                    DatePickerField(
                        label = "موعد المتابعة القادم",
                        timestamp = nextFollowUpDate,
                        onDateSelected = { nextFollowUpDate = it },
                        allowClear = true,
                        onClear = { nextFollowUpDate = null },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = internalNotes,
                    onValueChange = { internalNotes = it },
                    label = { Text("ملاحظات وتوجيهات داخلية إضافية") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // SECTION 4: Attachments
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
                    Text(
                        text = "4. الوثائق والمرفقات الرقمية",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FilledTonalButton(
                        onClick = {
                            filePickerLauncher.launch(
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
                        modifier = Modifier.testTag("pick_attachment_btn")
                    ) {
                        Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة ملفات", fontSize = 12.sp)
                    }
                }

                if (stagedAttachments.isEmpty()) {
                    Text(
                        text = "لم يتم إرفاق أي ملفات بعد. يمكنك إرفاق مستندات PDF، Word، Excel، أو صور المستندات الممسوحة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    stagedAttachments.forEachIndexed { index, att ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = att.fileName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${att.fileType.labelAr} - ${FileUtils.formatFileSize(att.fileSize)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { stagedAttachments.removeAt(index) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إزالة المرفق",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECTION 5: Initial Correspondence Link (Optional)
        if (!isEditMode && allCorrespondences.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "5. الربط بمراسلة سابقة (اختياري)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    var linkExpanded by remember { mutableStateOf(false) }
                    val currentLinked = allCorrespondences.firstOrNull { it.id == selectedLinkedId }

                    ExposedDropdownMenuBox(
                        expanded = linkExpanded,
                        onExpandedChange = { linkExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentLinked?.let { "${it.archiveNumber} - ${it.subject}" } ?: "بدون ربط",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("المراسلة المرتبطة") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = linkExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = linkExpanded,
                            onDismissRequest = { linkExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون ربط") },
                                onClick = {
                                    selectedLinkedId = null
                                    linkExpanded = false
                                }
                            )
                            allCorrespondences.take(15).forEach { corr ->
                                DropdownMenuItem(
                                    text = { Text("${corr.archiveNumber} - ${corr.subject}") },
                                    onClick = {
                                        selectedLinkedId = corr.id
                                        linkExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedLinkedId != null) {
                        var relationTypeExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = relationTypeExpanded,
                            onExpandedChange = { relationTypeExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedRelationType.labelAr,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("نوع العلاقة الإدارية") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = relationTypeExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = relationTypeExpanded,
                                onDismissRequest = { relationTypeExpanded = false }
                            ) {
                                RelationType.entries.forEach { rel ->
                                    DropdownMenuItem(
                                        text = { Text(rel.labelAr) },
                                        onClick = {
                                            selectedRelationType = rel
                                            relationTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Validation Error Display
        if (errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.weight(1f)
            ) {
                Text("إلغاء")
            }

            Button(
                onClick = {
                    if (subject.isBlank()) {
                        errorMessage = "يرجى إدخال موضوع المراسلة"
                        return@Button
                    }
                    if (sender.isBlank()) {
                        errorMessage = "يرجى تحديد الجهة أو الشخص المرسل"
                        return@Button
                    }
                    if (recipient.isBlank()) {
                        errorMessage = "يرجى تحديد الجهة أو الشخص المستقبل"
                        return@Button
                    }

                    val corr = Correspondence(
                        id = correspondenceId ?: 0L,
                        archiveNumber = archiveNumberPreview,
                        year = year,
                        sequenceNumber = 0,
                        type = type,
                        subject = subject.trim(),
                        summary = summary.trim(),
                        keywords = keywords.trim(),
                        category = category.trim(),
                        confidentiality = confidentiality,
                        externalReference = externalReference.trim(),
                        correspondenceDate = correspondenceDate,
                        receivedDate = receivedDate,
                        sender = sender.trim(),
                        recipient = recipient.trim(),
                        department = department.trim(),
                        responsibleEmployee = responsibleEmployee.trim(),
                        status = status,
                        priority = priority,
                        requiredAction = requiredAction.trim(),
                        assignedOffice = assignedOffice.trim(),
                        responseDeadline = responseDeadline,
                        nextFollowUpDate = nextFollowUpDate,
                        internalNotes = internalNotes.trim()
                    )

                    viewModel.saveCorrespondence(
                        correspondence = corr,
                        newAttachments = stagedAttachments.toList(),
                        initialLinkedId = selectedLinkedId,
                        initialRelationType = selectedRelationType
                    ) { savedId ->
                        viewModel.navigateBack()
                        viewModel.navigateToDetail(savedId)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_correspondence_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEditMode) "حفظ التعديلات" else "تسجيل وحفظ",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
