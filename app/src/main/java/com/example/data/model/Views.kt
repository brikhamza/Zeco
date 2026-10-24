package com.example.data.model

data class LinkedCorrespondenceItem(
    val relationId: Long,
    val relationType: RelationType,
    val isSource: Boolean,
    val linkedCorrespondenceId: Long,
    val linkedArchiveNumber: String,
    val linkedSubject: String,
    val linkedType: CorrespondenceType,
    val linkedDate: Long,
    val linkedStatus: CorrespondenceStatus,
    val linkedSender: String,
    val linkedRecipient: String,
    val notes: String = ""
)

data class CorrespondenceListItem(
    val correspondence: Correspondence,
    val attachmentCount: Int = 0,
    val linkedCount: Int = 0,
    val pendingRemindersCount: Int = 0
)

data class CorrespondenceDetailView(
    val correspondence: Correspondence,
    val attachments: List<Attachment> = emptyList(),
    val relations: List<LinkedCorrespondenceItem> = emptyList(),
    val followUps: List<FollowUpHistory> = emptyList(),
    val reminders: List<Reminder> = emptyList()
)

data class DashboardStats(
    val totalCount: Int = 0,
    val incomingCount: Int = 0,
    val outgoingCount: Int = 0,
    val internalCount: Int = 0,
    val requiringActionCount: Int = 0,
    val overdueCount: Int = 0,
    val dueTodayCount: Int = 0,
    val dueNextSevenDaysCount: Int = 0
)

data class FilterCriteria(
    val query: String = "",
    val type: CorrespondenceType? = null,
    val status: CorrespondenceStatus? = null,
    val priority: PriorityLevel? = null,
    val department: String? = null,
    val category: String? = null,
    val confidentiality: ConfidentialityLevel? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val onlyOverdue: Boolean = false,
    val onlyRequiresResponse: Boolean = false,
    val onlyWithAttachments: Boolean = false,
    val onlyLinked: Boolean = false
) {
    val isActive: Boolean
        get() = query.isNotBlank() || type != null || status != null || priority != null ||
                department != null || category != null || confidentiality != null ||
                startDate != null || endDate != null || onlyOverdue ||
                onlyRequiresResponse || onlyWithAttachments || onlyLinked
}
