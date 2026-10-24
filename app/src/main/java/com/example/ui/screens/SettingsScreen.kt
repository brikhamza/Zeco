package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val institutionName by viewModel.institutionName.collectAsStateWithLifecycle()
    val showHijri by viewModel.showHijriDate.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }
    var editInstitutionName by remember { mutableStateOf(institutionName) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Institutional Identity
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الهيئة الإدارية والترويسة الرسمية",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = editInstitutionName,
                        onValueChange = { editInstitutionName = it },
                        label = { Text("اسم الإدارة أو المديرية") },
                        placeholder = { Text("مثال: ولاية الجزائر - مديرية الإدارة المحلية") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2
                    )

                    Button(
                        onClick = {
                            viewModel.setInstitutionName(editInstitutionName.trim())
                            viewModel.showMessage("تم تحديث اسم الإدارة")
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ الترويسة")
                    }
                }
            }
        }

        // Section: Dates & Calendar Display
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "التقويم والمنطقة الزمنية",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("عرض التاريخ الهجري بجانب الميلادي", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("إظهار التاريخ الهجري في الترويسة وبطاقات المراسلات", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = showHijri,
                            onCheckedChange = { viewModel.toggleHijriDate(it) }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("المنطقة الزمنية المعتمدة", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("توقيت الجزائر الرسمي (Africa/Algiers GMT+1)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Section: Security & App Lock
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "أمان وسرية التطبيق (قفل برمز PIN)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "يمكنك حماية الوصول للأرشيف وسجلات المراسلات برمز مرور PIN سري يطلب عند فتح التطبيق.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { showPinDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Password, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعداد أو تغيير رمز PIN السري")
                    }
                }
            }
        }

        // Section: About
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حول تطبيق الأرشيف الإداري",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "الإصدار 1.0.0 - تطبيق مصمم خصيصاً للمؤسسات والإدارات العمومية الجزائرية لإدارة وتتبع وأرشفة المراسلات الرسمية والروابط والتذكيرات القانونية دون الحاجة لاتصال بالإنترنت.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    // Set PIN Dialog
    if (showPinDialog) {
        var pinValue by remember { mutableStateOf("") }
        var pinConfirm by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("إعداد رمز قفل التطبيق (PIN)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل رمز PIN من 4 أرقام أو اتركه فارغاً لإلغاء القفل:", fontSize = 12.sp)

                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { pinValue = it.filter { char -> char.isDigit() }.take(6) },
                        label = { Text("رمز PIN الجديد") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinValue.isNotBlank()) {
                        OutlinedTextField(
                            value = pinConfirm,
                            onValueChange = { pinConfirm = it.filter { char -> char.isDigit() }.take(6) },
                            label = { Text("تأكيد رمز PIN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    pinError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinValue.isBlank()) {
                            viewModel.setSecurityPin(null)
                            viewModel.showMessage("تم إلغاء قفل التطبيق")
                            showPinDialog = false
                        } else if (pinValue.length < 4) {
                            pinError = "يجب أن يتكون الرمز من 4 أرقام على الأقل"
                        } else if (pinValue != pinConfirm) {
                            pinError = "الرمزان غير متطابقين"
                        } else {
                            viewModel.setSecurityPin(pinValue)
                            viewModel.showMessage("تم تفعيل رمز قفل التطبيق بنجاح")
                            showPinDialog = false
                        }
                    }
                ) {
                    Text(if (pinValue.isBlank()) "إلغاء القفل" else "حفظ الرمز")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPinDialog = false }) { Text("إغلاق") }
            }
        )
    }
}
