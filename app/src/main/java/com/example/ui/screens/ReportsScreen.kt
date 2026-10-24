package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CorrespondenceStatus
import com.example.data.model.CorrespondenceType
import com.example.ui.MainViewModel
import com.example.util.DateUtils

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val allCorrespondences by viewModel.allCorrespondences.collectAsStateWithLifecycle()
    val institutionName by viewModel.institutionName.collectAsStateWithLifecycle()

    val total = allCorrespondences.size.coerceAtLeast(1)

    // Grouping by status
    val statusGroups = CorrespondenceStatus.entries.map { st ->
        val count = allCorrespondences.count { it.status == st }
        st to count
    }

    // Grouping by department
    val departmentGroups = allCorrespondences
        .filter { it.department.isNotBlank() }
        .groupBy { it.department }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    // Grouping by category
    val categoryGroups = allCorrespondences
        .filter { it.category.isNotBlank() }
        .groupBy { it.category }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    // Processing metrics
    val completedCount = allCorrespondences.count { it.status == CorrespondenceStatus.COMPLETED }
    val pendingCount = allCorrespondences.count { it.status != CorrespondenceStatus.COMPLETED && it.status != CorrespondenceStatus.ARCHIVED }
    val withoutResponseCount = allCorrespondences.count {
        it.type == CorrespondenceType.INCOMING && it.status != CorrespondenceStatus.COMPLETED && it.status != CorrespondenceStatus.ARCHIVED
    }

    val completionRate = if (total > 0) ((completedCount.toDouble() / total) * 100).toInt() else 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "التقرير الإداري الإحصائي للأرشيف",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = institutionName.ifBlank { "الإدارة العامة للأرشيف والمراسلات" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تاريخ التوليد: ${DateUtils.format(System.currentTimeMillis())} | إجمالي السجلات: ${allCorrespondences.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Actions: Export & Share
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.exportCsv(context, allCorrespondences) { file ->
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "تصدير السجل الكامل..."))
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير الأرشيف (CSV)", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = {
                        val summaryText = buildString {
                            append("📊 التقرير الإحصائي للأرشيف الإداري\n")
                            append("الجهة: $institutionName\n")
                            append("التاريخ: ${DateUtils.format(System.currentTimeMillis())}\n")
                            append("• إجمالي المحفوظات: ${allCorrespondences.size}\n")
                            append("• بريد وارد: ${stats.incomingCount}\n")
                            append("• بريد صادر: ${stats.outgoingCount}\n")
                            append("• مراسلات داخلية: ${stats.internalCount}\n")
                            append("• مراسلات منجزة: $completedCount ($completionRate%)\n")
                            append("• مراسلات قيد المتابعة: $pendingCount\n")
                            append("• مراسلات متأخرة تجاوزت الأجل: ${stats.overdueCount}\n")
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, summaryText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الملخص الإحصائي..."))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة الملخص", fontSize = 12.sp)
                }
            }
        }

        // Processing Performance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "مؤشرات الإنجاز والمعالجة الإدارية",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("نسبة الملفات المنجزة:", fontSize = 13.sp)
                        Text("$completionRate %", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = (completionRate / 100f).coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFF16A34A))
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ملفات قيد المعالجة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$pendingCount", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("وارد دون رد مسجل", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$withoutResponseCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                        Column {
                            Text("متأخرة عن الأجل", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${stats.overdueCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                }
            }
        }

        // Breakdown by Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "توزيع المراسلات حسب الحالة الإدارية",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    statusGroups.forEach { (st, count) ->
                        val pct = ((count.toDouble() / total) * 100).toInt()
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(st.labelAr, fontSize = 13.sp)
                                Text("$count ($pct%)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = (count.toFloat() / total).coerceIn(0f, 1f))
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Breakdown by Department
        if (departmentGroups.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "المراسلات حسب المصلحة أو المكتب",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        departmentGroups.take(6).forEach { (deptName, count) ->
                            val pct = ((count.toDouble() / total) * 100).toInt()
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(deptName, fontSize = 13.sp, maxLines = 1)
                                    Text("$count ($pct%)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = (count.toFloat() / total).coerceIn(0f, 1f))
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(MaterialTheme.colorScheme.secondary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}
