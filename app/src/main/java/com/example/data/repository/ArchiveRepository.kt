package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.Attachment
import com.example.data.model.CategoryEntity
import com.example.data.model.ContactEntity
import com.example.data.model.Correspondence
import com.example.data.model.CorrespondenceDetailView
import com.example.data.model.CorrespondenceListItem
import com.example.data.model.CorrespondenceRelation
import com.example.data.model.CorrespondenceStatus
import com.example.data.model.DashboardStats
import com.example.data.model.DepartmentEntity
import com.example.data.model.FilterCriteria
import com.example.data.model.FollowUpHistory
import com.example.data.model.LinkedCorrespondenceItem
import com.example.data.model.RelationType
import com.example.data.model.Reminder
import com.example.data.model.TagEntity
import com.example.service.ReminderScheduler
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class ArchiveRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val corrDao = database.correspondenceDao()
    private val attDao = database.attachmentDao()
    private val relDao = database.relationDao()
    private val followDao = database.followUpDao()
    private val reminderDao = database.reminderDao()
    private val dirDao = database.directoryDao()

    val allCorrespondences: Flow<List<Correspondence>> = corrDao.getAllFlow()

    fun getDashboardStats(): Flow<DashboardStats> {
        val now = System.currentTimeMillis()
        val startOfDay = DateUtils.getStartOfDay(now)
        val endOfDay = DateUtils.getEndOfOfDay(now)
        val inSevenDays = now + (7L * 24 * 60 * 60 * 1000L)

        return combine(
            listOf(
                corrDao.getTotalCountFlow(),
                corrDao.getIncomingCountFlow(),
                corrDao.getOutgoingCountFlow(),
                corrDao.getInternalCountFlow(),
                corrDao.getRequiringActionCountFlow(),
                corrDao.getOverdueCountFlow(now),
                corrDao.getDueTodayCountFlow(startOfDay, endOfDay),
                corrDao.getDueNextSevenDaysCountFlow(now, inSevenDays)
            )
        ) { array ->
            DashboardStats(
                totalCount = array[0],
                incomingCount = array[1],
                outgoingCount = array[2],
                internalCount = array[3],
                requiringActionCount = array[4],
                overdueCount = array[5],
                dueTodayCount = array[6],
                dueNextSevenDaysCount = array[7]
            )
        }.flowOn(Dispatchers.IO)
    }

    fun getRecentlyAdded(limit: Int = 5): Flow<List<Correspondence>> = corrDao.getRecentlyAddedFlow(limit)
    fun getRecentlyUpdated(limit: Int = 5): Flow<List<Correspondence>> = corrDao.getRecentlyUpdatedFlow(limit)
    fun getUpcomingDeadlines(limit: Int = 10): Flow<List<Correspondence>> =
        corrDao.getUpcomingDeadlinesFlow(System.currentTimeMillis(), limit)
    fun getOverdueList(): Flow<List<Correspondence>> =
        corrDao.getOverdueListFlow(System.currentTimeMillis())

    fun getCorrespondenceDetail(id: Long): Flow<CorrespondenceDetailView?> {
        return combine(
            corrDao.getByIdFlow(id),
            attDao.getAttachmentsForCorrespondence(id),
            relDao.getRelationsForCorrespondence(id),
            followDao.getHistoryForCorrespondence(id),
            reminderDao.getRemindersForCorrespondence(id)
        ) { corr, attachments, relations, followUps, reminders ->
            if (corr == null) return@combine null

            // Enriched relations with target/source details
            val linkedItems = relations.mapNotNull { rel ->
                val isSource = rel.sourceCorrespondenceId == id
                val otherId = if (isSource) rel.targetCorrespondenceId else rel.sourceCorrespondenceId
                val otherCorr = corrDao.getById(otherId) ?: return@mapNotNull null

                LinkedCorrespondenceItem(
                    relationId = rel.id,
                    relationType = rel.relationType,
                    isSource = isSource,
                    linkedCorrespondenceId = otherCorr.id,
                    linkedArchiveNumber = otherCorr.archiveNumber,
                    linkedSubject = otherCorr.subject,
                    linkedType = otherCorr.type,
                    linkedDate = otherCorr.correspondenceDate,
                    linkedStatus = otherCorr.status,
                    linkedSender = otherCorr.sender,
                    linkedRecipient = otherCorr.recipient,
                    notes = rel.notes
                )
            }

            CorrespondenceDetailView(
                correspondence = corr,
                attachments = attachments,
                relations = linkedItems,
                followUps = followUps,
                reminders = reminders
            )
        }.flowOn(Dispatchers.IO)
    }

    suspend fun generateNextArchiveNumber(year: Int = DateUtils.getCurrentYear()): Pair<String, Int> = withContext(Dispatchers.IO) {
        val maxSeq = corrDao.getMaxSequenceForYear(year) ?: 0
        val nextSeq = maxSeq + 1
        val archiveNumber = "ARCH-$year-${String.format(java.util.Locale.US, "%04d", nextSeq)}"
        archiveNumber to nextSeq
    }

    suspend fun createCorrespondence(
        correspondence: Correspondence,
        initialAttachments: List<Attachment> = emptyList(),
        initialLinkedId: Long? = null,
        initialRelationType: RelationType = RelationType.RELATED_TO
    ): Long = withContext(Dispatchers.IO) {
        val (archiveNumber, nextSeq) = generateNextArchiveNumber(correspondence.year)
        val toInsert = correspondence.copy(
            archiveNumber = archiveNumber,
            sequenceNumber = nextSeq,
            registeredDate = System.currentTimeMillis(),
            updatedDate = System.currentTimeMillis()
        )
        val newId = corrDao.insert(toInsert)

        // Save attachments
        if (initialAttachments.isNotEmpty()) {
            val updatedAttachments = initialAttachments.map { it.copy(correspondenceId = newId) }
            attDao.insertAll(updatedAttachments)
        }

        // Add initial link if provided
        if (initialLinkedId != null && initialLinkedId > 0 && initialLinkedId != newId) {
            relDao.insert(
                CorrespondenceRelation(
                    sourceCorrespondenceId = newId,
                    targetCorrespondenceId = initialLinkedId,
                    relationType = initialRelationType,
                    notes = "ربط تلقائي عند التسجيل"
                )
            )
        }

        // Record initial history
        followDao.insert(
            FollowUpHistory(
                correspondenceId = newId,
                actionDate = System.currentTimeMillis(),
                actionTitle = "تسجيل المراسلة في الأرشيف",
                actionDescription = "تم إنشاء السجل برقم أرشيف $archiveNumber",
                newStatus = toInsert.status.name,
                recordedBy = toInsert.responsibleEmployee.ifBlank { "أمين الأرشيف" }
            )
        )

        newId
    }

    suspend fun updateCorrespondence(
        updated: Correspondence,
        historyNote: String = "تعديل بيانات المراسلة"
    ) = withContext(Dispatchers.IO) {
        val existing = corrDao.getById(updated.id) ?: return@withContext
        // Archive number must never change!
        val preserved = updated.copy(
            archiveNumber = existing.archiveNumber,
            year = existing.year,
            sequenceNumber = existing.sequenceNumber,
            registeredDate = existing.registeredDate,
            updatedDate = System.currentTimeMillis()
        )
        corrDao.update(preserved)

        // Record history of status change if changed
        val statusChanged = existing.status != preserved.status
        followDao.insert(
            FollowUpHistory(
                correspondenceId = preserved.id,
                actionDate = System.currentTimeMillis(),
                actionTitle = if (statusChanged) "تغيير حالة المراسلة إلى: ${preserved.status.labelAr}" else historyNote,
                actionDescription = if (statusChanged) "من: ${existing.status.labelAr} إلى: ${preserved.status.labelAr}" else historyNote,
                previousStatus = existing.status.name,
                newStatus = preserved.status.name,
                recordedBy = preserved.responsibleEmployee.ifBlank { "المستخدم" }
            )
        )
    }

    suspend fun updateStatus(id: Long, newStatus: CorrespondenceStatus, notes: String = "") = withContext(Dispatchers.IO) {
        val corr = corrDao.getById(id) ?: return@withContext
        val updated = corr.copy(
            status = newStatus,
            completedDate = if (newStatus == CorrespondenceStatus.COMPLETED) System.currentTimeMillis() else corr.completedDate,
            updatedDate = System.currentTimeMillis()
        )
        corrDao.update(updated)

        followDao.insert(
            FollowUpHistory(
                correspondenceId = id,
                actionDate = System.currentTimeMillis(),
                actionTitle = "تحديث الحالة إلى: ${newStatus.labelAr}",
                actionDescription = notes.ifBlank { "تم تغيير حالة المعالجة" },
                previousStatus = corr.status.name,
                newStatus = newStatus.name,
                recordedBy = corr.responsibleEmployee.ifBlank { "المستخدم" }
            )
        )
    }

    suspend fun addFollowUpAction(
        correspondenceId: Long,
        title: String,
        description: String,
        recordedBy: String?
    ) = withContext(Dispatchers.IO) {
        followDao.insert(
            FollowUpHistory(
                correspondenceId = correspondenceId,
                actionDate = System.currentTimeMillis(),
                actionTitle = title,
                actionDescription = description,
                recordedBy = recordedBy
            )
        )
    }

    suspend fun deleteCorrespondence(id: Long): Boolean = withContext(Dispatchers.IO) {
        // Delete attachment files on disk
        val attachments = attDao.getAttachmentsDirect(id)
        for (att in attachments) {
            try {
                val f = File(att.filePath)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        corrDao.deleteById(id)
        true
    }

    suspend fun getDeletableWarning(id: Long): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val attCount = attDao.getCountForCorrespondence(id)
        val relCount = relDao.getRelationCount(id)
        attCount to relCount
    }

    // Attachments
    suspend fun addAttachment(attachment: Attachment): Long = withContext(Dispatchers.IO) {
        attDao.insert(attachment)
    }

    suspend fun deleteAttachment(attachment: Attachment) = withContext(Dispatchers.IO) {
        try {
            val f = File(attachment.filePath)
            if (f.exists()) f.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        attDao.delete(attachment)
    }

    // Relations
    suspend fun addRelation(
        sourceId: Long,
        targetId: Long,
        relationType: RelationType,
        notes: String = ""
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (sourceId == targetId) {
            return@withContext Result.failure(Exception("لا يمكن ربط المراسلة بنفسها"))
        }
        val exists = relDao.relationExists(sourceId, targetId) > 0
        if (exists) {
            return@withContext Result.failure(Exception("هذه المراسلة مرتبطة بالفعل"))
        }
        val id = relDao.insert(
            CorrespondenceRelation(
                sourceCorrespondenceId = sourceId,
                targetCorrespondenceId = targetId,
                relationType = relationType,
                notes = notes
            )
        )
        Result.success(id)
    }

    suspend fun deleteRelation(relationId: Long) = withContext(Dispatchers.IO) {
        relDao.deleteById(relationId)
    }

    // Reminders
    fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllRemindersFlow()
    fun getActiveReminders(): Flow<List<Reminder>> = reminderDao.getActiveRemindersFlow()

    suspend fun createReminder(reminder: Reminder, archiveNumber: String, subject: String): Long = withContext(Dispatchers.IO) {
        val id = reminderDao.insert(reminder)
        val saved = reminder.copy(id = id)
        ReminderScheduler.scheduleReminder(context, saved, archiveNumber, subject)
        id
    }

    suspend fun toggleReminderCompleted(reminderId: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        reminderDao.setCompleted(
            id = reminderId,
            completed = completed,
            completedAt = if (completed) System.currentTimeMillis() else null
        )
        if (completed) {
            ReminderScheduler.cancelReminder(context, reminderId)
        }
    }

    suspend fun deleteReminder(reminderId: Long) = withContext(Dispatchers.IO) {
        ReminderScheduler.cancelReminder(context, reminderId)
        reminderDao.deleteById(reminderId)
    }

    // Directories
    fun getDepartments(): Flow<List<DepartmentEntity>> = dirDao.getAllDepartments()
    suspend fun addDepartment(department: DepartmentEntity) = dirDao.insertDepartment(department)
    suspend fun deleteDepartment(department: DepartmentEntity) = dirDao.deleteDepartment(department)

    fun getCategories(): Flow<List<CategoryEntity>> = dirDao.getAllCategories()
    suspend fun addCategory(category: CategoryEntity) = dirDao.insertCategory(category)
    suspend fun deleteCategory(category: CategoryEntity) = dirDao.deleteCategory(category)

    fun getContacts(): Flow<List<ContactEntity>> = dirDao.getAllContacts()
    suspend fun addContact(contact: ContactEntity) = dirDao.insertContact(contact)
    suspend fun deleteContact(contact: ContactEntity) = dirDao.deleteContact(contact)

    fun getTags(): Flow<List<TagEntity>> = dirDao.getAllTags()
    suspend fun addTag(tag: TagEntity) = dirDao.insertTag(tag)
    suspend fun deleteTag(tag: TagEntity) = dirDao.deleteTag(tag)

    fun filterCorrespondences(criteria: FilterCriteria): Flow<List<Correspondence>> {
        return corrDao.getAllFlow().combine(attDao.getAllAttachmentsFlow()) { list, attachments ->
            val attachmentsMap = attachments.groupBy { it.correspondenceId }

            list.filter { item ->
                // Query
                if (criteria.query.isNotBlank()) {
                    val q = criteria.query.trim().lowercase()
                    val matchMain = item.archiveNumber.lowercase().contains(q) ||
                            item.subject.lowercase().contains(q) ||
                            item.summary.lowercase().contains(q) ||
                            item.sender.lowercase().contains(q) ||
                            item.recipient.lowercase().contains(q) ||
                            item.externalReference.lowercase().contains(q) ||
                            item.department.lowercase().contains(q) ||
                            item.keywords.lowercase().contains(q) ||
                            item.internalNotes.lowercase().contains(q)

                    val matchAtt = attachmentsMap[item.id]?.any { it.fileName.lowercase().contains(q) } == true
                    if (!matchMain && !matchAtt) return@filter false
                }

                if (criteria.type != null && item.type != criteria.type) return@filter false
                if (criteria.status != null && item.status != criteria.status) return@filter false
                if (criteria.priority != null && item.priority != criteria.priority) return@filter false
                if (criteria.confidentiality != null && item.confidentiality != criteria.confidentiality) return@filter false
                if (!criteria.department.isNullOrBlank() && item.department != criteria.department) return@filter false
                if (!criteria.category.isNullOrBlank() && item.category != criteria.category) return@filter false

                if (criteria.startDate != null && item.correspondenceDate < criteria.startDate) return@filter false
                if (criteria.endDate != null && item.correspondenceDate > criteria.endDate) return@filter false

                if (criteria.onlyOverdue) {
                    val isOverdue = item.status != CorrespondenceStatus.COMPLETED &&
                            item.status != CorrespondenceStatus.ARCHIVED &&
                            item.responseDeadline != null &&
                            item.responseDeadline < System.currentTimeMillis()
                    if (!isOverdue) return@filter false
                }

                if (criteria.onlyRequiresResponse) {
                    val req = item.status != CorrespondenceStatus.COMPLETED &&
                            item.status != CorrespondenceStatus.ARCHIVED &&
                            (item.requiredAction.isNotBlank() || item.responseDeadline != null)
                    if (!req) return@filter false
                }

                if (criteria.onlyWithAttachments) {
                    val hasAtt = (attachmentsMap[item.id]?.size ?: 0) > 0
                    if (!hasAtt) return@filter false
                }

                true
            }
        }.flowOn(Dispatchers.IO)
    }
}
