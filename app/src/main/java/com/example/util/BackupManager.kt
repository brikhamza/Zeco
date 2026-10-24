package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.model.Attachment
import com.example.data.model.CategoryEntity
import com.example.data.model.ContactEntity
import com.example.data.model.Correspondence
import com.example.data.model.CorrespondenceRelation
import com.example.data.model.DepartmentEntity
import com.example.data.model.FollowUpHistory
import com.example.data.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object BackupManager {

    suspend fun exportCorrespondenceToCsv(
        context: Context,
        correspondences: List<Correspondence>
    ): File = withContext(Dispatchers.IO) {
        val fileName = "archive_export_${System.currentTimeMillis()}.csv"
        val exportFile = File(context.cacheDir, fileName)

        FileOutputStream(exportFile).use { fos ->
            // Write UTF-8 BOM so Arabic letters render cleanly in Excel
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Header
                writer.write("رقم الأرشيف,النوع,الموضوع,المرسل,المستقبل,التصنيف,المصلحة,الحالة,الأولوية,السرية,الرقم الإشاري,تاريخ المراسلة,تاريخ الاستلام,أجل الرد,الإجراء المطلوب,ملاحظات\n")
                for (item in correspondences) {
                    val line = listOf(
                        escapeCsv(item.archiveNumber),
                        escapeCsv(item.type.labelAr),
                        escapeCsv(item.subject),
                        escapeCsv(item.sender),
                        escapeCsv(item.recipient),
                        escapeCsv(item.category),
                        escapeCsv(item.department),
                        escapeCsv(item.status.labelAr),
                        escapeCsv(item.priority.labelAr),
                        escapeCsv(item.confidentiality.labelAr),
                        escapeCsv(item.externalReference),
                        escapeCsv(DateUtils.format(item.correspondenceDate)),
                        escapeCsv(DateUtils.format(item.receivedDate)),
                        escapeCsv(DateUtils.format(item.responseDeadline)),
                        escapeCsv(item.requiredAction),
                        escapeCsv(item.internalNotes)
                    ).joinToString(",")
                    writer.write(line + "\n")
                }
            }
        }
        exportFile
    }

    suspend fun createFullJsonBackup(context: Context, db: AppDatabase): File = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "الأرشيف الإداري")
        root.put("createdAt", System.currentTimeMillis())

        val corrDao = db.correspondenceDao()
        val allCorr = corrDao.getAllDirect()
        val corrArray = JSONArray()
        for (c in allCorr) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("archiveNumber", c.archiveNumber)
                put("year", c.year)
                put("sequenceNumber", c.sequenceNumber)
                put("type", c.type.name)
                put("subject", c.subject)
                put("summary", c.summary)
                put("keywords", c.keywords)
                put("category", c.category)
                put("confidentiality", c.confidentiality.name)
                put("externalReference", c.externalReference)
                put("correspondenceDate", c.correspondenceDate)
                put("receivedDate", c.receivedDate)
                put("registeredDate", c.registeredDate)
                put("sender", c.sender)
                put("recipient", c.recipient)
                put("department", c.department)
                put("responsibleEmployee", c.responsibleEmployee)
                put("status", c.status.name)
                put("priority", c.priority.name)
                put("requiredAction", c.requiredAction)
                put("assignedOffice", c.assignedOffice)
                if (c.responseDeadline != null) put("responseDeadline", c.responseDeadline)
                if (c.nextFollowUpDate != null) put("nextFollowUpDate", c.nextFollowUpDate)
                if (c.completedDate != null) put("completedDate", c.completedDate)
                put("internalNotes", c.internalNotes)
                put("updatedDate", c.updatedDate)
            }
            corrArray.put(obj)
        }
        root.put("correspondences", corrArray)

        // Attachments
        val attDao = db.attachmentDao()
        val allAtt = attDao.getAllAttachmentsDirect()
        val attArray = JSONArray()
        for (a in allAtt) {
            val obj = JSONObject().apply {
                put("id", a.id)
                put("correspondenceId", a.correspondenceId)
                put("fileName", a.fileName)
                put("filePath", a.filePath)
                put("fileType", a.fileType.name)
                put("mimeType", a.mimeType)
                put("fileSize", a.fileSize)
                put("attachmentDate", a.attachmentDate)
                put("description", a.description)
            }
            attArray.put(obj)
        }
        root.put("attachments", attArray)

        // Relations
        val relDao = db.relationDao()
        val allRel = relDao.getAllRelationsDirect()
        val relArray = JSONArray()
        for (r in allRel) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("sourceCorrespondenceId", r.sourceCorrespondenceId)
                put("targetCorrespondenceId", r.targetCorrespondenceId)
                put("relationType", r.relationType.name)
                put("createdAt", r.createdAt)
                put("notes", r.notes)
            }
            relArray.put(obj)
        }
        root.put("relations", relArray)

        val backupFile = File(context.cacheDir, "administrative_archive_backup_${System.currentTimeMillis()}.json")
        backupFile.writeText(root.toString(2), StandardCharsets.UTF_8)
        backupFile
    }

    data class RestoreSummary(
        val totalRestored: Int,
        val attachmentsCount: Int,
        val relationsCount: Int
    )

    suspend fun restoreFromJson(context: Context, uri: Uri, db: AppDatabase): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { it.reader().readText() }
                ?: return@withContext Result.failure(Exception("تعذر قراءة ملف النسخة الاحتياطية"))

            val root = JSONObject(content)
            if (!root.has("correspondences")) {
                return@withContext Result.failure(Exception("ملف النسخة الاحتياطية غير متوافق"))
            }

            val corrArray = root.getJSONArray("correspondences")
            val corrDao = db.correspondenceDao()
            var corrCount = 0

            for (i in 0 until corrArray.length()) {
                val obj = corrArray.getJSONObject(i)
                val archiveNum = obj.getString("archiveNumber")
                val existing = corrDao.getByArchiveNumber(archiveNum)

                val corr = Correspondence(
                    id = existing?.id ?: 0,
                    archiveNumber = archiveNum,
                    year = obj.optInt("year", DateUtils.getCurrentYear()),
                    sequenceNumber = obj.optInt("sequenceNumber", 1),
                    type = com.example.data.model.CorrespondenceType.fromString(obj.optString("type")),
                    subject = obj.optString("subject", "بدون موضوع"),
                    summary = obj.optString("summary", ""),
                    keywords = obj.optString("keywords", ""),
                    category = obj.optString("category", "مراسلة رسمية"),
                    confidentiality = com.example.data.model.ConfidentialityLevel.fromString(obj.optString("confidentiality")),
                    externalReference = obj.optString("externalReference", ""),
                    correspondenceDate = obj.optLong("correspondenceDate", System.currentTimeMillis()),
                    receivedDate = obj.optLong("receivedDate", System.currentTimeMillis()),
                    registeredDate = obj.optLong("registeredDate", System.currentTimeMillis()),
                    sender = obj.optString("sender", "غير محدد"),
                    recipient = obj.optString("recipient", "غير محدد"),
                    department = obj.optString("department", ""),
                    responsibleEmployee = obj.optString("responsibleEmployee", ""),
                    status = com.example.data.model.CorrespondenceStatus.fromString(obj.optString("status")),
                    priority = com.example.data.model.PriorityLevel.fromString(obj.optString("priority")),
                    requiredAction = obj.optString("requiredAction", ""),
                    assignedOffice = obj.optString("assignedOffice", ""),
                    responseDeadline = if (obj.has("responseDeadline")) obj.getLong("responseDeadline") else null,
                    nextFollowUpDate = if (obj.has("nextFollowUpDate")) obj.getLong("nextFollowUpDate") else null,
                    completedDate = if (obj.has("completedDate")) obj.getLong("completedDate") else null,
                    internalNotes = obj.optString("internalNotes", ""),
                    updatedDate = obj.optLong("updatedDate", System.currentTimeMillis())
                )

                if (existing != null) {
                    corrDao.update(corr)
                } else {
                    corrDao.insert(corr)
                }
                corrCount++
            }

            var attCount = 0
            if (root.has("attachments")) {
                val attArray = root.getJSONArray("attachments")
                val attDao = db.attachmentDao()
                for (i in 0 until attArray.length()) {
                    val obj = attArray.getJSONObject(i)
                    val att = Attachment(
                        correspondenceId = obj.getLong("correspondenceId"),
                        fileName = obj.getString("fileName"),
                        filePath = obj.getString("filePath"),
                        fileType = com.example.data.model.AttachmentType.fromFileName(obj.getString("fileName")),
                        mimeType = obj.optString("mimeType", "application/octet-stream"),
                        fileSize = obj.optLong("fileSize", 0),
                        attachmentDate = obj.optLong("attachmentDate", System.currentTimeMillis()),
                        description = obj.optString("description", "")
                    )
                    attDao.insert(att)
                    attCount++
                }
            }

            var relCount = 0
            if (root.has("relations")) {
                val relArray = root.getJSONArray("relations")
                val relDao = db.relationDao()
                for (i in 0 until relArray.length()) {
                    val obj = relArray.getJSONObject(i)
                    val rel = CorrespondenceRelation(
                        sourceCorrespondenceId = obj.getLong("sourceCorrespondenceId"),
                        targetCorrespondenceId = obj.getLong("targetCorrespondenceId"),
                        relationType = com.example.data.model.RelationType.fromString(obj.getString("relationType")),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        notes = obj.optString("notes", "")
                    )
                    relDao.insert(rel)
                    relCount++
                }
            }

            Result.success(RestoreSummary(corrCount, attCount, relCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(text: String): String {
        val escaped = text.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
