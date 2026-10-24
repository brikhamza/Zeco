package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.AttachmentType
import com.example.data.model.ConfidentialityLevel
import com.example.data.model.CorrespondenceStatus
import com.example.data.model.CorrespondenceType
import com.example.data.model.PriorityLevel
import com.example.data.model.RelationType

class Converters {
    @TypeConverter
    fun fromCorrespondenceType(value: CorrespondenceType?): String = value?.name ?: CorrespondenceType.INCOMING.name

    @TypeConverter
    fun toCorrespondenceType(value: String?): CorrespondenceType =
        value?.let { runCatching { CorrespondenceType.valueOf(it) }.getOrNull() } ?: CorrespondenceType.INCOMING

    @TypeConverter
    fun fromCorrespondenceStatus(value: CorrespondenceStatus?): String = value?.name ?: CorrespondenceStatus.NEW.name

    @TypeConverter
    fun toCorrespondenceStatus(value: String?): CorrespondenceStatus =
        value?.let { runCatching { CorrespondenceStatus.valueOf(it) }.getOrNull() } ?: CorrespondenceStatus.NEW

    @TypeConverter
    fun fromPriorityLevel(value: PriorityLevel?): String = value?.name ?: PriorityLevel.NORMAL.name

    @TypeConverter
    fun toPriorityLevel(value: String?): PriorityLevel =
        value?.let { runCatching { PriorityLevel.valueOf(it) }.getOrNull() } ?: PriorityLevel.NORMAL

    @TypeConverter
    fun fromConfidentialityLevel(value: ConfidentialityLevel?): String = value?.name ?: ConfidentialityLevel.NORMAL.name

    @TypeConverter
    fun toConfidentialityLevel(value: String?): ConfidentialityLevel =
        value?.let { runCatching { ConfidentialityLevel.valueOf(it) }.getOrNull() } ?: ConfidentialityLevel.NORMAL

    @TypeConverter
    fun fromRelationType(value: RelationType?): String = value?.name ?: RelationType.RELATED_TO.name

    @TypeConverter
    fun toRelationType(value: String?): RelationType =
        value?.let { runCatching { RelationType.valueOf(it) }.getOrNull() } ?: RelationType.RELATED_TO

    @TypeConverter
    fun fromAttachmentType(value: AttachmentType?): String = value?.name ?: AttachmentType.OTHER.name

    @TypeConverter
    fun toAttachmentType(value: String?): AttachmentType =
        value?.let { runCatching { AttachmentType.valueOf(it) }.getOrNull() } ?: AttachmentType.OTHER
}
