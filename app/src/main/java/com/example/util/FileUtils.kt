package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.data.model.AttachmentType
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FileUtils {

    fun getAttachmentsDir(context: Context): File {
        val dir = File(context.filesDir, "attachments")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    data class ImportedFileInfo(
        val originalName: String,
        val savedRelativePath: String,
        val mimeType: String,
        val fileSize: Long,
        val attachmentType: AttachmentType
    )

    fun importUriToInternalStorage(context: Context, uri: Uri): ImportedFileInfo? {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "doc_${System.currentTimeMillis()}"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromFileName(fileName)
            val attachmentType = AttachmentType.fromFileName(fileName)

            val ext = fileName.substringAfterLast('.', "")
            val uniqueFileName = "att_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}" +
                    if (ext.isNotBlank()) ".$ext" else ""

            val targetFile = File(getAttachmentsDir(context), uniqueFileName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (fileSize <= 0) {
                fileSize = targetFile.length()
            }

            ImportedFileInfo(
                originalName = fileName,
                savedRelativePath = targetFile.absolutePath,
                mimeType = mimeType,
                fileSize = fileSize,
                attachmentType = attachmentType
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 ب"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f ميغابايت", mb)
            kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f كيلوبايت", kb)
            else -> "$bytes بايت"
        }
    }

    fun openFileWithExternalApp(context: Context, filePath: String, mimeType: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "فتح الوثيقة بواسطة...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun renderPdfFirstPage(filePath: String, width: Int = 400, height: Int = 560): Bitmap? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                fileDescriptor.close()
                bitmap
            } else {
                renderer.close()
                fileDescriptor.close()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getMimeTypeFromFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }
}
