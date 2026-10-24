package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Attachment
import com.example.data.model.CategoryEntity
import com.example.data.model.ContactEntity
import com.example.data.model.Correspondence
import com.example.data.model.CorrespondenceRelation
import com.example.data.model.DepartmentEntity
import com.example.data.model.FollowUpHistory
import com.example.data.model.Reminder
import com.example.data.model.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CorrespondenceDao {
    @Query("SELECT * FROM correspondences ORDER BY registeredDate DESC")
    fun getAllFlow(): Flow<List<Correspondence>>

    @Query("SELECT * FROM correspondences ORDER BY registeredDate DESC")
    suspend fun getAllDirect(): List<Correspondence>

    @Query("SELECT * FROM correspondences WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: Long): Flow<Correspondence?>

    @Query("SELECT * FROM correspondences WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Correspondence?

    @Query("SELECT * FROM correspondences WHERE archiveNumber = :archiveNumber LIMIT 1")
    suspend fun getByArchiveNumber(archiveNumber: String): Correspondence?

    @Query("SELECT MAX(sequenceNumber) FROM correspondences WHERE year = :year")
    suspend fun getMaxSequenceForYear(year: Int): Int?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(correspondence: Correspondence): Long

    @Update
    suspend fun update(correspondence: Correspondence)

    @Delete
    suspend fun delete(correspondence: Correspondence)

    @Query("DELETE FROM correspondences WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT * FROM correspondences 
        WHERE (archiveNumber LIKE '%' || :query || '%' 
           OR subject LIKE '%' || :query || '%' 
           OR summary LIKE '%' || :query || '%' 
           OR sender LIKE '%' || :query || '%' 
           OR recipient LIKE '%' || :query || '%' 
           OR externalReference LIKE '%' || :query || '%' 
           OR department LIKE '%' || :query || '%'
           OR keywords LIKE '%' || :query || '%'
           OR internalNotes LIKE '%' || :query || '%')
        ORDER BY registeredDate DESC
    """)
    fun search(query: String): Flow<List<Correspondence>>

    @Query("SELECT COUNT(*) FROM correspondences")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM correspondences WHERE type = 'INCOMING'")
    fun getIncomingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM correspondences WHERE type = 'OUTGOING'")
    fun getOutgoingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM correspondences WHERE type = 'INTERNAL'")
    fun getInternalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM correspondences WHERE status NOT IN ('COMPLETED', 'ARCHIVED') AND (requiredAction != '' OR responseDeadline IS NOT NULL)")
    fun getRequiringActionCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM correspondences WHERE status NOT IN ('COMPLETED', 'ARCHIVED') AND responseDeadline IS NOT NULL AND responseDeadline < :currentTime")
    fun getOverdueCountFlow(currentTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM correspondences WHERE status NOT IN ('COMPLETED', 'ARCHIVED') AND responseDeadline IS NOT NULL AND responseDeadline >= :startOfDay AND responseDeadline <= :endOfDay")
    fun getDueTodayCountFlow(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM correspondences WHERE status NOT IN ('COMPLETED', 'ARCHIVED') AND responseDeadline IS NOT NULL AND responseDeadline >= :currentTime AND responseDeadline <= :futureTime")
    fun getDueNextSevenDaysCountFlow(currentTime: Long, futureTime: Long): Flow<Int>

    @Query("SELECT * FROM correspondences ORDER BY registeredDate DESC LIMIT :limit")
    fun getRecentlyAddedFlow(limit: Int = 5): Flow<List<Correspondence>>

    @Query("SELECT * FROM correspondences ORDER BY updatedDate DESC LIMIT :limit")
    fun getRecentlyUpdatedFlow(limit: Int = 5): Flow<List<Correspondence>>

    @Query("SELECT * FROM correspondences WHERE status NOT IN ('COMPLETED', 'ARCHIVED') AND responseDeadline IS NOT NULL AND responseDeadline < :currentTime ORDER BY responseDeadline ASC")
    fun getOverdueListFlow(currentTime: Long): Flow<List<Correspondence>>

    @Query("SELECT * FROM correspondences WHERE status NOT IN ('COMPLETED', 'ARCHIVED') AND responseDeadline IS NOT NULL AND responseDeadline >= :currentTime ORDER BY responseDeadline ASC LIMIT :limit")
    fun getUpcomingDeadlinesFlow(currentTime: Long, limit: Int = 10): Flow<List<Correspondence>>
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE correspondenceId = :correspondenceId ORDER BY attachmentDate DESC")
    fun getAttachmentsForCorrespondence(correspondenceId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE correspondenceId = :correspondenceId ORDER BY attachmentDate DESC")
    suspend fun getAttachmentsDirect(correspondenceId: Long): List<Attachment>

    @Query("SELECT * FROM attachments WHERE id = :id LIMIT 1")
    suspend fun getAttachmentById(id: Long): Attachment?

    @Query("SELECT COUNT(*) FROM attachments WHERE correspondenceId = :correspondenceId")
    suspend fun getCountForCorrespondence(correspondenceId: Long): Int

    @Query("SELECT COUNT(*) FROM attachments WHERE correspondenceId = :correspondenceId")
    fun getCountForCorrespondenceFlow(correspondenceId: Long): Flow<Int>

    @Query("SELECT * FROM attachments ORDER BY attachmentDate DESC")
    fun getAllAttachmentsFlow(): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments ORDER BY attachmentDate DESC")
    suspend fun getAllAttachmentsDirect(): List<Attachment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: Attachment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<Attachment>)

    @Delete
    suspend fun delete(attachment: Attachment)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface RelationDao {
    @Query("""
        SELECT * FROM correspondence_relations 
        WHERE sourceCorrespondenceId = :correspondenceId OR targetCorrespondenceId = :correspondenceId
        ORDER BY createdAt DESC
    """)
    fun getRelationsForCorrespondence(correspondenceId: Long): Flow<List<CorrespondenceRelation>>

    @Query("""
        SELECT * FROM correspondence_relations 
        WHERE sourceCorrespondenceId = :correspondenceId OR targetCorrespondenceId = :correspondenceId
        ORDER BY createdAt DESC
    """)
    suspend fun getRelationsDirect(correspondenceId: Long): List<CorrespondenceRelation>

    @Query("SELECT COUNT(*) FROM correspondence_relations WHERE sourceCorrespondenceId = :correspondenceId OR targetCorrespondenceId = :correspondenceId")
    suspend fun getRelationCount(correspondenceId: Long): Int

    @Query("SELECT COUNT(*) FROM correspondence_relations WHERE (sourceCorrespondenceId = :sourceId AND targetCorrespondenceId = :targetId) OR (sourceCorrespondenceId = :targetId AND targetCorrespondenceId = :sourceId)")
    suspend fun relationExists(sourceId: Long, targetId: Long): Int

    @Query("SELECT * FROM correspondence_relations ORDER BY createdAt DESC")
    fun getAllRelationsFlow(): Flow<List<CorrespondenceRelation>>

    @Query("SELECT * FROM correspondence_relations ORDER BY createdAt DESC")
    suspend fun getAllRelationsDirect(): List<CorrespondenceRelation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(relation: CorrespondenceRelation): Long

    @Delete
    suspend fun delete(relation: CorrespondenceRelation)

    @Query("DELETE FROM correspondence_relations WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface FollowUpDao {
    @Query("SELECT * FROM follow_up_history WHERE correspondenceId = :correspondenceId ORDER BY actionDate DESC")
    fun getHistoryForCorrespondence(correspondenceId: Long): Flow<List<FollowUpHistory>>

    @Query("SELECT * FROM follow_up_history WHERE correspondenceId = :correspondenceId ORDER BY actionDate DESC")
    suspend fun getHistoryDirect(correspondenceId: Long): List<FollowUpHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: FollowUpHistory): Long

    @Delete
    suspend fun delete(history: FollowUpHistory)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY reminderTime ASC")
    fun getAllRemindersFlow(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders ORDER BY reminderTime ASC")
    suspend fun getAllRemindersDirect(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY reminderTime ASC")
    fun getActiveRemindersFlow(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE correspondenceId = :correspondenceId ORDER BY reminderTime ASC")
    fun getRemindersForCorrespondence(correspondenceId: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE reminders SET isCompleted = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, completedAt: Long?)
}

@Dao
interface DirectoryDao {
    // Departments
    @Query("SELECT * FROM departments ORDER BY name ASC")
    fun getAllDepartments(): Flow<List<DepartmentEntity>>

    @Query("SELECT * FROM departments ORDER BY name ASC")
    suspend fun getAllDepartmentsDirect(): List<DepartmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepartment(department: DepartmentEntity): Long

    @Delete
    suspend fun deleteDepartment(department: DepartmentEntity)

    // Categories
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategoriesDirect(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    // Contacts
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    suspend fun getAllContactsDirect(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    // Tags
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsDirect(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Delete
    suspend fun deleteTag(tag: TagEntity)
}
