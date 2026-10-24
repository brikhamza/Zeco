package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "correspondences",
    indices = [
        Index(value = ["archiveNumber"], unique = true),
        Index(value = ["type"]),
        Index(value = ["status"]),
        Index(value = ["priority"]),
        Index(value = ["department"]),
        Index(value = ["responseDeadline"]),
        Index(value = ["registeredDate"])
    ]
)
data class Correspondence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val archiveNumber: String,
    val year: Int,
    val sequenceNumber: Int,
    val type: CorrespondenceType,
    val subject: String,
    val summary: String = "",
    val keywords: String = "",
    val category: String = "مراسلة رسمية",
    val confidentiality: ConfidentialityLevel = ConfidentialityLevel.NORMAL,
    val externalReference: String = "",
    val correspondenceDate: Long = System.currentTimeMillis(),
    val receivedDate: Long = System.currentTimeMillis(),
    val registeredDate: Long = System.currentTimeMillis(),
    val sender: String,
    val recipient: String,
    val department: String = "",
    val responsibleEmployee: String = "",
    val status: CorrespondenceStatus = CorrespondenceStatus.NEW,
    val priority: PriorityLevel = PriorityLevel.NORMAL,
    val requiredAction: String = "",
    val assignedOffice: String = "",
    val responseDeadline: Long? = null,
    val nextFollowUpDate: Long? = null,
    val completedDate: Long? = null,
    val internalNotes: String = "",
    val updatedDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = Correspondence::class,
            parentColumns = ["id"],
            childColumns = ["correspondenceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("correspondenceId")]
)
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val correspondenceId: Long,
    val fileName: String,
    val filePath: String,
    val fileType: AttachmentType,
    val mimeType: String,
    val fileSize: Long,
    val attachmentDate: Long = System.currentTimeMillis(),
    val description: String = ""
)

@Entity(
    tableName = "correspondence_relations",
    foreignKeys = [
        ForeignKey(
            entity = Correspondence::class,
            parentColumns = ["id"],
            childColumns = ["sourceCorrespondenceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Correspondence::class,
            parentColumns = ["id"],
            childColumns = ["targetCorrespondenceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sourceCorrespondenceId"),
        Index("targetCorrespondenceId"),
        Index(value = ["sourceCorrespondenceId", "targetCorrespondenceId"], unique = true)
    ]
)
data class CorrespondenceRelation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceCorrespondenceId: Long,
    val targetCorrespondenceId: Long,
    val relationType: RelationType,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(
    tableName = "follow_up_history",
    foreignKeys = [
        ForeignKey(
            entity = Correspondence::class,
            parentColumns = ["id"],
            childColumns = ["correspondenceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("correspondenceId")]
)
data class FollowUpHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val correspondenceId: Long,
    val actionDate: Long = System.currentTimeMillis(),
    val actionTitle: String,
    val actionDescription: String = "",
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val recordedBy: String? = null
)

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Correspondence::class,
            parentColumns = ["id"],
            childColumns = ["correspondenceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("correspondenceId"), Index("reminderTime")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val correspondenceId: Long,
    val title: String,
    val reminderTime: Long,
    val advanceNoticeDays: Int = 0,
    val priority: PriorityLevel = PriorityLevel.NORMAL,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

@Entity(tableName = "departments")
data class DepartmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val directorate: String = "",
    val headName: String = ""
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = ""
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val organization: String = "",
    val role: String = "",
    val phone: String = "",
    val email: String = ""
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
