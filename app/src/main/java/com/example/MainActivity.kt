package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.screens.AddEditCorrespondenceScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.CorrespondenceDetailScreen
import com.example.ui.screens.CorrespondenceListScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DirectoriesScreen
import com.example.ui.screens.RelationsTimelineScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AdministrativeArchiveTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle opening specific correspondence from notification intent
        handleIntent(intent)

        setContent {
            AdministrativeArchiveTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    ArchiveApp(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val corrId = intent?.getLongExtra("EXTRA_OPEN_CORRESPONDENCE_ID", -1L) ?: -1L
        if (corrId > 0) {
            viewModel.navigateToDetail(corrId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Intercept hardware / system back button
    BackHandler(enabled = currentScreen !is Screen.Dashboard) {
        viewModel.navigateBack()
    }

    // If locked by PIN
    if (isLocked) {
        PinLockScreen(viewModel)
        return
    }

    var showMenuDropdown by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            is Screen.Dashboard -> "الأرشيف الإداري"
                            is Screen.CorrespondenceList -> "سجل المراسلات"
                            is Screen.AddEditCorrespondence -> {
                                val edit = (currentScreen as Screen.AddEditCorrespondence).correspondenceId
                                if (edit != null) "تعديل المراسلة" else "تسجيل مراسلة جديدة"
                            }
                            is Screen.CorrespondenceDetail -> "تفاصيل المراسلة"
                            is Screen.Search -> "البحث والتصفية"
                            is Screen.Reminders -> "التذكيرات والآجال"
                            is Screen.RelationsTimeline -> "شجرة الملفات والروابط"
                            is Screen.Directories -> "الهيكل والتصنيفات"
                            is Screen.Reports -> "التقارير والإحصائيات"
                            is Screen.BackupRestore -> "النسخ الاحتياطي"
                            is Screen.Settings -> "إعدادات الأرشيف"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (currentScreen !is Screen.Dashboard) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Search) }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "بحث")
                    }

                    Box {
                        IconButton(onClick = { showMenuDropdown = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "المزيد")
                        }

                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("الهيكل والدليل الإداري") },
                                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.navigateTo(Screen.Directories)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("شجرة الروابط والتسلسل") },
                                leadingIcon = { Icon(Icons.Default.DeviceHub, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.navigateTo(Screen.RelationsTimeline())
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("التقارير والإحصائيات") },
                                leadingIcon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.navigateTo(Screen.Reports)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("النسخ الاحتياطي واستعادة البيانات") },
                                leadingIcon = { Icon(Icons.Default.Backup, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.navigateTo(Screen.BackupRestore)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("إعدادات الأرشيف والأمان") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.navigateTo(Screen.Settings)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen is Screen.Dashboard,
                    onClick = { viewModel.navigateTo(Screen.Dashboard) },
                    icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("الرئيسية", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.CorrespondenceList,
                    onClick = { viewModel.navigateTo(Screen.CorrespondenceList) },
                    icon = { Icon(imageVector = Icons.Default.Folder, contentDescription = null) },
                    label = { Text("المراسلات", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_list")
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.AddEditCorrespondence && (currentScreen as Screen.AddEditCorrespondence).correspondenceId == null,
                    onClick = { viewModel.navigateToAdd() },
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    label = { Text("تسجيل جديد", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_add")
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Reminders,
                    onClick = { viewModel.navigateTo(Screen.Reminders) },
                    icon = { Icon(imageVector = Icons.Default.Alarm, contentDescription = null) },
                    label = { Text("الآجال والتذكير", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_reminders")
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Reports,
                    onClick = { viewModel.navigateTo(Screen.Reports) },
                    icon = { Icon(imageVector = Icons.Default.Assessment, contentDescription = null) },
                    label = { Text("التقارير", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_reports")
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val screen = currentScreen) {
                is Screen.Dashboard -> DashboardScreen(viewModel)
                is Screen.CorrespondenceList -> CorrespondenceListScreen(viewModel)
                is Screen.AddEditCorrespondence -> AddEditCorrespondenceScreen(
                    correspondenceId = screen.correspondenceId,
                    initialLinkedId = screen.initialLinkedId,
                    initialRelationType = screen.initialRelationType,
                    viewModel = viewModel
                )
                is Screen.CorrespondenceDetail -> CorrespondenceDetailScreen(
                    correspondenceId = screen.correspondenceId,
                    viewModel = viewModel
                )
                is Screen.Search -> SearchScreen(viewModel)
                is Screen.Reminders -> RemindersScreen(viewModel)
                is Screen.RelationsTimeline -> RelationsTimelineScreen(
                    caseCorrespondenceId = screen.correspondenceId,
                    viewModel = viewModel
                )
                is Screen.Directories -> DirectoriesScreen(viewModel)
                is Screen.Reports -> ReportsScreen(viewModel)
                is Screen.BackupRestore -> BackupRestoreScreen(viewModel)
                is Screen.Settings -> SettingsScreen(viewModel)
            }
        }
    }
}

@Composable
fun PinLockScreen(viewModel: MainViewModel) {
    var enteredPin by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "الأرشيف الإداري محمي برمز PIN",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "يرجى إدخال رمز المرور السري للوصول إلى السجلات والوثائق",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = enteredPin,
                onValueChange = {
                    enteredPin = it.filter { c -> c.isDigit() }.take(6)
                    hasError = false
                },
                label = { Text("رمز المرور PIN") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = hasError,
                modifier = Modifier.fillMaxWidth(0.7f)
            )

            if (hasError) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "رمز PIN غير صحيح، يرجى المحاولة مرة أخرى",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val ok = viewModel.verifyPin(enteredPin)
                    if (!ok) {
                        hasError = true
                        enteredPin = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("إلغاء القفل والدخول")
            }
        }
    }
}
