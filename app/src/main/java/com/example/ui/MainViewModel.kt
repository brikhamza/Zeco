package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ArchiveApplication
import com.example.data.model.Attachment
import com.example.data.model.CategoryEntity
import com.example.data.model.ContactEntity
import com.example.data.model.Correspondence
import com.example.data.model.CorrespondenceDetailView
import com.example.data.model.CorrespondenceStatus
import com.example.data.model.CorrespondenceType
import com.example.data.model.DashboardStats
import com.example.data.model.DepartmentEntity
import com.example.data.model.FilterCriteria
import com.example.data.model.RelationType
import com.example.data.model.Reminder
import com.example.data.model.TagEntity
import com.example.util.BackupManager
import com.example.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class Screen {
    data object Dashboard : Screen()
    data object CorrespondenceList : Screen()
    data class AddEditCorrespondence(
        val correspondenceId: Long? = null,
        val initialLinkedId: Long? = null,
        val initialRelationType: RelationType? = null
    ) : Screen()
    data class CorrespondenceDetail(val correspondenceId: Long) : Screen()
    data object Search : Screen()
    data object Reminders : Screen()
    data class RelationsTimeline(val correspondenceId: Long? = null) : Screen()
    data object Directories : Screen()
    data object Reports : Screen()
    data object BackupRestore : Screen()
    data object Settings : Screen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ArchiveApplication).repository
    private val database = (application as ArchiveApplication).database
    private val prefs = application.getSharedPreferences("admin_archive_prefs", Context.MODE_PRIVATE)

    // Navigation BackStack
    private val _screenStack = MutableStateFlow<List<Screen>>(listOf(Screen.Dashboard))
    val currentScreen: StateFlow<Screen> = _screenStack.map { it.lastOrNull() ?: Screen.Dashboard }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Screen.Dashboard)

    fun navigateTo(screen: Screen) {
        val current = _screenStack.value
        // Avoid duplicate push
        if (current.lastOrNull() != screen) {
            _screenStack.value = current + screen
        }
    }

    fun navigateBack(): Boolean {
        val current = _screenStack.value
        return if (current.size > 1) {
            _screenStack.value = current.dropLast(1)
            true
        } else {
            false
        }
    }

    fun navigateToDetail(id: Long) {
        navigateTo(Screen.CorrespondenceDetail(id))
    }

    fun navigateToAdd(initialLinkedId: Long? = null, initialRelationType: RelationType? = null) {
        navigateTo(Screen.AddEditCorrespondence(null, initialLinkedId, initialRelationType))
    }

    fun navigateToEdit(id: Long) {
        navigateTo(Screen.AddEditCorrespondence(id))
    }

    // App Lock & Settings
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _showHijriDate = MutableStateFlow(prefs.getBoolean("show_hijri", true))
    val showHijriDate: StateFlow<Boolean> = _showHijriDate.asStateFlow()

    private val _institutionName = MutableStateFlow(
        prefs.getString("institution_name", "الجمهورية الجزائرية الديمقراطية الشعبية - ولاية الجزائر") ?: ""
    )
    val institutionName: StateFlow<String> = _institutionName.asStateFlow()

    init {
        val pin = prefs.getString("security_pin", null)
        if (!pin.isNullOrBlank()) {
            _isLocked.value = true
        }
    }

    fun verifyPin(enteredPin: String): Boolean {
        val saved = prefs.getString("security_pin", null)
        return if (saved == enteredPin) {
            _isLocked.value = false
            true
        } else {
            false
        }
    }

    fun setSecurityPin(pin: String?) {
        if (pin.isNullOrBlank()) {
            prefs.edit().remove("security_pin").apply()
            _isLocked.value = false
        } else {
            prefs.edit().putString("security_pin", pin).apply()
        }
    }

    fun toggleHijriDate(show: Boolean) {
        _showHijriDate.value = show
        prefs.edit().putBoolean("show_hijri", show).apply()
    }

    fun setInstitutionName(name: String) {
        _institutionName.value = name
        prefs.edit().putString("institution_name", name).apply()
    }

    // Live Stats & Dashboard
    val dashboardStats: StateFlow<DashboardStats> = repository.getDashboardStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    val recentlyAdded = repository.getRecentlyAdded(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyUpdated = repository.getRecentlyUpdated(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingDeadlines = repository.getUpcomingDeadlines(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overdueList = repository.getOverdueList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All correspondences
    val allCorrespondences: StateFlow<List<Correspondence>> = repository.allCorrespondences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List Tab Filter (All, Incoming, Outgoing, Internal, Overdue, Requires Action)
    private val _listFilterCriteria = MutableStateFlow(FilterCriteria())
    val listFilterCriteria: StateFlow<FilterCriteria> = _listFilterCriteria.asStateFlow()

    val filteredList = _listFilterCriteria.flatMapLatest { criteria ->
        repository.filterCorrespondences(criteria)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateListFilter(criteria: FilterCriteria) {
        _listFilterCriteria.value = criteria
    }

    fun setListTab(tabIndex: Int) {
        val current = _listFilterCriteria.value
        val updated = when (tabIndex) {
            0 -> current.copy(type = null, onlyOverdue = false, onlyRequiresResponse = false)
            1 -> current.copy(type = CorrespondenceType.INCOMING, onlyOverdue = false, onlyRequiresResponse = false)
            2 -> current.copy(type = CorrespondenceType.OUTGOING, onlyOverdue = false, onlyRequiresResponse = false)
            3 -> current.copy(type = CorrespondenceType.INTERNAL, onlyOverdue = false, onlyRequiresResponse = false)
            4 -> current.copy(type = null, onlyRequiresResponse = true, onlyOverdue = false)
            5 -> current.copy(type = null, onlyOverdue = true, onlyRequiresResponse = false)
            else -> current
        }
        _listFilterCriteria.value = updated
    }

    // Search & Advanced Filters
    private val _searchCriteria = MutableStateFlow(FilterCriteria())
    val searchCriteria: StateFlow<FilterCriteria> = _searchCriteria.asStateFlow()

    val searchResults = _searchCriteria.flatMapLatest { criteria ->
        repository.filterCorrespondences(criteria)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchCriteria(criteria: FilterCriteria) {
        _searchCriteria.value = criteria
    }

    fun resetSearch() {
        _searchCriteria.value = FilterCriteria()
    }

    // Detail View
    private val _selectedDetailId = MutableStateFlow<Long?>(null)
    val selectedDetail: StateFlow<CorrespondenceDetailView?> = _selectedDetailId.flatMapLatest { id ->
        if (id != null) repository.getCorrespondenceDetail(id)
        else kotlinx.coroutines.flow.flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectCorrespondence(id: Long) {
        _selectedDetailId.value = id
    }

    // Reminders
    val allReminders: StateFlow<List<Reminder>> = repository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeReminders: StateFlow<List<Reminder>> = repository.getActiveReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Directories
    val departments: StateFlow<List<DepartmentEntity>> = repository.getDepartments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<ContactEntity>> = repository.getContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = repository.getTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Messages / Snackbars
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    // Operations
    fun saveCorrespondence(
        correspondence: Correspondence,
        newAttachments: List<Attachment>,
        initialLinkedId: Long?,
        initialRelationType: RelationType,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (correspondence.id == 0L) {
                    val newId = repository.createCorrespondence(
                        correspondence = correspondence,
                        initialAttachments = newAttachments,
                        initialLinkedId = initialLinkedId,
                        initialRelationType = initialRelationType
                    )
                    showMessage("تم تسجيل المراسلة بنجاح برقم: ${correspondence.archiveNumber}")
                    onComplete(newId)
                } else {
                    repository.updateCorrespondence(correspondence)
                    // Add new attachments if any
                    for (att in newAttachments) {
                        repository.addAttachment(att.copy(correspondenceId = correspondence.id))
                    }
                    showMessage("تم تحديث بيانات المراسلة بنجاح")
                    onComplete(correspondence.id)
                }
            } catch (e: Exception) {
                showMessage("حدث خطأ أثناء الحفظ: ${e.message}")
            }
        }
    }

    fun updateStatus(id: Long, newStatus: CorrespondenceStatus, note: String = "") {
        viewModelScope.launch {
            repository.updateStatus(id, newStatus, note)
            showMessage("تم تغيير حالة المراسلة إلى: ${newStatus.labelAr}")
        }
    }

    fun addFollowUp(correspondenceId: Long, title: String, desc: String, author: String) {
        viewModelScope.launch {
            repository.addFollowUpAction(correspondenceId, title, desc, author)
            showMessage("تم تسجيل إجراء المتابعة")
        }
    }

    fun deleteCorrespondence(id: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteCorrespondence(id)
            showMessage("تم حذف المراسلة من الأرشيف")
            onDeleted()
        }
    }

    suspend fun getDeletableWarning(id: Long): Pair<Int, Int> {
        return repository.getDeletableWarning(id)
    }

    fun linkCorrespondence(sourceId: Long, targetId: Long, type: RelationType, notes: String) {
        viewModelScope.launch {
            val res = repository.addRelation(sourceId, targetId, type, notes)
            if (res.isSuccess) {
                showMessage("تم ربط المراسلتين بنجاح (${type.labelAr})")
            } else {
                showMessage(res.exceptionOrNull()?.message ?: "تعذر ربط المراسلة")
            }
        }
    }

    fun deleteRelation(relationId: Long) {
        viewModelScope.launch {
            repository.deleteRelation(relationId)
            showMessage("تم فك الارتباط بين المراسلتين")
        }
    }

    fun addReminder(reminder: Reminder, archiveNumber: String, subject: String) {
        viewModelScope.launch {
            repository.createReminder(reminder, archiveNumber, subject)
            showMessage("تمت جدولة التذكير بنجاح")
        }
    }

    fun toggleReminder(reminderId: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleReminderCompleted(reminderId, completed)
            showMessage(if (completed) "تم إنجاز التذكير" else "تمت إعادة تفعيل التذكير")
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            repository.deleteReminder(reminderId)
            showMessage("تم حذف التذكير")
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch {
            repository.deleteAttachment(attachment)
            showMessage("تم حذف المرفق")
        }
    }

    // Directory operations
    fun addDepartment(dept: DepartmentEntity) = viewModelScope.launch { repository.addDepartment(dept) }
    fun deleteDepartment(dept: DepartmentEntity) = viewModelScope.launch { repository.deleteDepartment(dept) }
    fun addCategory(cat: CategoryEntity) = viewModelScope.launch { repository.addCategory(cat) }
    fun deleteCategory(cat: CategoryEntity) = viewModelScope.launch { repository.deleteCategory(cat) }
    fun addContact(contact: ContactEntity) = viewModelScope.launch { repository.addContact(contact) }
    fun deleteContact(contact: ContactEntity) = viewModelScope.launch { repository.deleteContact(contact) }
    fun addTag(tag: TagEntity) = viewModelScope.launch { repository.addTag(tag) }
    fun deleteTag(tag: TagEntity) = viewModelScope.launch { repository.deleteTag(tag) }

    // Export & Backup
    fun exportCsv(context: Context, correspondences: List<Correspondence>, onExported: (File) -> Unit) {
        viewModelScope.launch {
            try {
                val file = BackupManager.exportCorrespondenceToCsv(context, correspondences)
                showMessage("تم تصدير ملف CSV بنجاح (${correspondences.size} مراسلة)")
                onExported(file)
            } catch (e: Exception) {
                showMessage("فشل التصدير: ${e.message}")
            }
        }
    }

    fun createBackup(context: Context, onBackupCreated: (File) -> Unit) {
        viewModelScope.launch {
            try {
                val file = BackupManager.createFullJsonBackup(context, database)
                showMessage("تم إنشاء النسخة الاحتياطية بنجاح")
                onBackupCreated(file)
            } catch (e: Exception) {
                showMessage("فشل إنشاء النسخة الاحتياطية: ${e.message}")
            }
        }
    }

    fun restoreBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val result = BackupManager.restoreFromJson(context, uri, database)
                if (result.isSuccess) {
                    val summary = result.getOrThrow()
                    showMessage("تمت استعادة ${summary.totalRestored} مراسلة و${summary.attachmentsCount} مرفق بنجاح")
                } else {
                    showMessage("فشلت الاستعادة: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showMessage("فشلت الاستعادة: ${e.message}")
            }
        }
    }
}
