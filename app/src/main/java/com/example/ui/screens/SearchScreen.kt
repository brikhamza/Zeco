package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ConfidentialityLevel
import com.example.data.model.CorrespondenceStatus
import com.example.data.model.CorrespondenceType
import com.example.data.model.FilterCriteria
import com.example.data.model.PriorityLevel
import com.example.ui.MainViewModel
import com.example.ui.components.CorrespondenceItemCard
import com.example.ui.components.DatePickerField
import com.example.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val criteria by viewModel.searchCriteria.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showFilterPanel by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen")
    ) {
        // Search Input Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = criteria.query,
                    onValueChange = { viewModel.updateSearchCriteria(criteria.copy(query = it)) },
                    placeholder = { Text("بحث عام: رقم الأرشيف، الموضوع، المرسل، المستقبل، الكلمات المفتاحية...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (criteria.query.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateSearchCriteria(criteria.copy(query = "")) }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showFilterPanel = !showFilterPanel }) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showFilterPanel) "إخفاء خيارات التصفية" else "إظهار خيارات التصفية المتقدمة")
                    }

                    if (criteria.isActive) {
                        TextButton(onClick = { viewModel.resetSearch() }) {
                            Text("إعادة ضبط", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Expandable Filter Panel
                if (showFilterPanel) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Type Chips
                    Column {
                        Text("نوع المراسلة:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = criteria.type == null,
                                onClick = { viewModel.updateSearchCriteria(criteria.copy(type = null)) },
                                label = { Text("الكل") }
                            )
                            CorrespondenceType.entries.forEach { t ->
                                FilterChip(
                                    selected = criteria.type == t,
                                    onClick = { viewModel.updateSearchCriteria(criteria.copy(type = t)) },
                                    label = { Text(t.labelAr) }
                                )
                            }
                        }
                    }

                    // Status Chips
                    Column {
                        Text("الحالة الإدارية:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = criteria.status == null,
                                onClick = { viewModel.updateSearchCriteria(criteria.copy(status = null)) },
                                label = { Text("كل الحالات") }
                            )
                            CorrespondenceStatus.entries.forEach { st ->
                                FilterChip(
                                    selected = criteria.status == st,
                                    onClick = { viewModel.updateSearchCriteria(criteria.copy(status = st)) },
                                    label = { Text(st.labelAr) }
                                )
                            }
                        }
                    }

                    // Priority Chips
                    Column {
                        Text("درجة الأولوية:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = criteria.priority == null,
                                onClick = { viewModel.updateSearchCriteria(criteria.copy(priority = null)) },
                                label = { Text("كل الأولويات") }
                            )
                            PriorityLevel.entries.forEach { pr ->
                                FilterChip(
                                    selected = criteria.priority == pr,
                                    onClick = { viewModel.updateSearchCriteria(criteria.copy(priority = pr)) },
                                    label = { Text(pr.labelAr) }
                                )
                            }
                        }
                    }

                    // Date Range Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DatePickerField(
                            label = "من تاريخ",
                            timestamp = criteria.startDate,
                            onDateSelected = { viewModel.updateSearchCriteria(criteria.copy(startDate = it)) },
                            allowClear = true,
                            onClear = { viewModel.updateSearchCriteria(criteria.copy(startDate = null)) },
                            modifier = Modifier.weight(1f)
                        )
                        DatePickerField(
                            label = "إلى تاريخ",
                            timestamp = criteria.endDate,
                            onDateSelected = { viewModel.updateSearchCriteria(criteria.copy(endDate = it)) },
                            allowClear = true,
                            onClear = { viewModel.updateSearchCriteria(criteria.copy(endDate = null)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Toggles: Overdue, Requires Action, Has Attachments
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSearchCriteria(criteria.copy(onlyOverdue = !criteria.onlyOverdue))
                                }
                        ) {
                            Checkbox(
                                checked = criteria.onlyOverdue,
                                onCheckedChange = { viewModel.updateSearchCriteria(criteria.copy(onlyOverdue = it)) }
                            )
                            Text("المراسلات المتأخرة فقط (تجاوزت أجل الرد)", fontSize = 13.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSearchCriteria(criteria.copy(onlyRequiresResponse = !criteria.onlyRequiresResponse))
                                }
                        ) {
                            Checkbox(
                                checked = criteria.onlyRequiresResponse,
                                onCheckedChange = { viewModel.updateSearchCriteria(criteria.copy(onlyRequiresResponse = it)) }
                            )
                            Text("المراسلات التي تتطلب رداً أو إجراء", fontSize = 13.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSearchCriteria(criteria.copy(onlyWithAttachments = !criteria.onlyWithAttachments))
                                }
                        ) {
                            Checkbox(
                                checked = criteria.onlyWithAttachments,
                                onCheckedChange = { viewModel.updateSearchCriteria(criteria.copy(onlyWithAttachments = it)) }
                            )
                            Text("تحتوي على مرفقات ووثائق رقمية فقط", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Results Header with Export Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "نتائج البحث (${results.size} مراسلة)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (results.isNotEmpty()) {
                TextButton(
                    onClick = {
                        viewModel.exportCsv(context, results) { file ->
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "تصدير نتائج البحث عبر..."))
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تصدير النتائج (CSV)", fontSize = 12.sp)
                }
            }
        }

        // Results List
        if (results.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Search,
                title = "لا توجد نتائج",
                message = "جرب تغيير مصطلحات البحث أو تخفيف شروط التصفية."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(results, key = { it.id }) { item ->
                    CorrespondenceItemCard(
                        correspondence = item,
                        onClick = { viewModel.navigateToDetail(item.id) }
                    )
                }
            }
        }
    }
}
