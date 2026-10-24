package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class CorrespondenceType(val labelAr: String, val shortPrefix: String) {
    INCOMING("وارد", "وارد"),
    OUTGOING("صادر", "صادر"),
    INTERNAL("داخلي", "داخلي");

    companion object {
        fun fromString(value: String): CorrespondenceType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.labelAr == value } ?: INCOMING
        }
    }
}

enum class CorrespondenceStatus(val labelAr: String) {
    NEW("جديد"),
    UNDER_REVIEW("قيد الدراسة"),
    IN_PROGRESS("قيد المعالجة"),
    AWAITING_RESPONSE("في انتظار الرد"),
    COMPLETED("منجز"),
    ARCHIVED("مؤرشف");

    companion object {
        fun fromString(value: String): CorrespondenceStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.labelAr == value } ?: NEW
        }
    }
}

enum class PriorityLevel(val labelAr: String) {
    LOW("منخفض"),
    NORMAL("عادي"),
    HIGH("هام"),
    URGENT("عاجل");

    companion object {
        fun fromString(value: String): PriorityLevel {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.labelAr == value } ?: NORMAL
        }
    }
}

enum class ConfidentialityLevel(val labelAr: String) {
    NORMAL("عادي"),
    CONFIDENTIAL("سري"),
    HIGHLY_CONFIDENTIAL("سري للغاية");

    companion object {
        fun fromString(value: String): ConfidentialityLevel {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.labelAr == value } ?: NORMAL
        }
    }
}

enum class RelationType(val labelAr: String, val inverseLabelAr: String) {
    REPLY_TO("رد على", "متبوع برد من"),
    RESPONSE_TO("جواب على", "متبوع بجواب من"),
    FOLLOW_UP_TO("متابعة لـ", "متبوع بمتابعة من"),
    CONTINUATION_OF("امتداد لـ", "له امتداد"),
    RELATED_TO("مرتبط بـ", "مرتبط بـ"),
    SUPERSEDES("تعويض / إلغاء لـ", "تم تعويضه بـ"),
    SAME_CASE("نفس الملف الإداري", "نفس الملف الإداري");

    companion object {
        fun fromString(value: String): RelationType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.labelAr == value } ?: RELATED_TO
        }
    }
}

enum class AttachmentType(val labelAr: String) {
    PDF("وثيقة PDF"),
    DOCX("مستند Word"),
    XLSX("جدول Excel"),
    IMAGE("صورة / وثيقة ممسوحة"),
    OTHER("ملف مرفق");

    companion object {
        fun fromFileName(fileName: String): AttachmentType {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            return when (extension) {
                "pdf" -> PDF
                "doc", "docx" -> DOCX
                "xls", "xlsx" -> XLSX
                "jpg", "jpeg", "png", "webp", "bmp" -> IMAGE
                else -> OTHER
            }
        }
    }
}
