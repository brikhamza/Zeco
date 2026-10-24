package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.ConfidentialityLevel
import com.example.data.model.Correspondence
import com.example.data.model.CorrespondenceStatus
import com.example.data.model.CorrespondenceType
import com.example.data.model.PriorityLevel
import com.example.data.model.RelationType
import com.example.data.model.Reminder
import com.example.data.repository.ArchiveRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ArchiveRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ArchiveRepository(context, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAppNameString() {
        val appName = context.getString(R.string.app_name)
        assertEquals("الأرشيف الإداري", appName)
    }

    @Test
    fun testSequentialArchiveNumberGenerationAndPersistence() = runBlocking {
        val (firstArchiveNum, firstSeq) = repository.generateNextArchiveNumber(2026)
        assertEquals("ARCH-2026-0001", firstArchiveNum)
        assertEquals(1, firstSeq)

        val incomingLetter = Correspondence(
            archiveNumber = firstArchiveNum,
            year = 2026,
            sequenceNumber = firstSeq,
            type = CorrespondenceType.INCOMING,
            subject = "طلب رخصة استغلال تجزئة سكنية",
            summary = "طلب مقدم من المواطن للحصول على موافقة إدارية",
            sender = "المواطن أحمد بن علي",
            recipient = "مديرية الإدارة المحلية",
            department = "مصلحة الشؤون القانونية والمنازعات",
            status = CorrespondenceStatus.NEW,
            priority = PriorityLevel.HIGH
        )

        val id1 = repository.createCorrespondence(incomingLetter)
        assertTrue(id1 > 0)

        // Verify stored in DB
        val stored1 = database.correspondenceDao().getById(id1)
        assertNotNull(stored1)
        assertEquals("ARCH-2026-0001", stored1?.archiveNumber)
        assertEquals("طلب رخصة استغلال تجزئة سكنية", stored1?.subject)

        // Generate second sequential archive number
        val (secondArchiveNum, secondSeq) = repository.generateNextArchiveNumber(2026)
        assertEquals("ARCH-2026-0002", secondArchiveNum)
        assertEquals(2, secondSeq)

        val outgoingLetter = Correspondence(
            archiveNumber = secondArchiveNum,
            year = 2026,
            sequenceNumber = secondSeq,
            type = CorrespondenceType.OUTGOING,
            subject = "رد على طلب رخصة استغلال التجزئة",
            summary = "إشعار بالموافقة المبدئية المشروطة",
            sender = "مديرية الإدارة المحلية",
            recipient = "المواطن أحمد بن علي",
            status = CorrespondenceStatus.IN_PROGRESS
        )

        val id2 = repository.createCorrespondence(outgoingLetter)
        assertTrue(id2 > 0)
        val stored2 = database.correspondenceDao().getById(id2)
        assertEquals("ARCH-2026-0002", stored2?.archiveNumber)
    }

    @Test
    fun testEditingRecordPreservesArchiveNumber() = runBlocking {
        val (archiveNum, seq) = repository.generateNextArchiveNumber(2026)
        val original = Correspondence(
            archiveNumber = archiveNum,
            year = 2026,
            sequenceNumber = seq,
            type = CorrespondenceType.INCOMING,
            subject = "الموضوع الأصلي",
            sender = "المرسل الأصلي",
            recipient = "المستقبل الأصلي"
        )
        val id = repository.createCorrespondence(original)
        val storedOriginal = database.correspondenceDao().getById(id)!!
        assertEquals(archiveNum, storedOriginal.archiveNumber)

        // Attempt to update with a tampered archive number
        val modified = storedOriginal.copy(
            archiveNumber = "FAKE-OVERWRITE-9999",
            subject = "الموضوع المعدل بعد المراجعة",
            status = CorrespondenceStatus.UNDER_REVIEW
        )
        repository.updateCorrespondence(modified)

        // Check that archiveNumber is preserved!
        val reloaded = database.correspondenceDao().getById(id)!!
        assertEquals(archiveNum, reloaded.archiveNumber)
        assertEquals("الموضوع المعدل بعد المراجعة", reloaded.subject)
        assertEquals(CorrespondenceStatus.UNDER_REVIEW, reloaded.status)
    }

    @Test
    fun testLinkingCorrespondenceAndPreventingDuplicates() = runBlocking {
        val id1 = repository.createCorrespondence(
            Correspondence(
                archiveNumber = "ARCH-2026-0001",
                year = 2026,
                sequenceNumber = 1,
                type = CorrespondenceType.INCOMING,
                subject = "كتاب وارد رقم 1",
                sender = "ولاية الجزائر",
                recipient = "البلدية"
            )
        )

        val id2 = repository.createCorrespondence(
            Correspondence(
                archiveNumber = "ARCH-2026-0002",
                year = 2026,
                sequenceNumber = 2,
                type = CorrespondenceType.OUTGOING,
                subject = "جواب صادر رقم 1",
                sender = "البلدية",
                recipient = "ولاية الجزائر"
            )
        )

        // Link them
        val linkResult = repository.addRelation(id1, id2, RelationType.REPLY_TO, "رد رسمي")
        assertTrue(linkResult.isSuccess)

        // Duplicate link should fail gracefully
        val duplicateResult = repository.addRelation(id1, id2, RelationType.RELATED_TO)
        assertFalse(duplicateResult.isSuccess)

        // Self-link should fail
        val selfResult = repository.addRelation(id1, id1, RelationType.RELATED_TO)
        assertFalse(selfResult.isSuccess)

        // Verify linked item appears in details of both
        val detail1 = repository.getCorrespondenceDetail(id1).first()!!
        assertEquals(1, detail1.relations.size)
        assertEquals(id2, detail1.relations[0].linkedCorrespondenceId)

        val detail2 = repository.getCorrespondenceDetail(id2).first()!!
        assertEquals(1, detail2.relations.size)
        assertEquals(id1, detail2.relations[0].linkedCorrespondenceId)
    }

    @Test
    fun testRemindersCompletionToggle() = runBlocking {
        val corrId = repository.createCorrespondence(
            Correspondence(
                archiveNumber = "ARCH-2026-0005",
                year = 2026,
                sequenceNumber = 5,
                type = CorrespondenceType.INCOMING,
                subject = "مراسلة بموعد استحقاق",
                sender = "الدائرة",
                recipient = "مكتب الصفقات"
            )
        )

        val reminderId = repository.createReminder(
            Reminder(
                correspondenceId = corrId,
                title = "تذكير بموعد الجلسة",
                reminderTime = System.currentTimeMillis() + 3600000L,
                priority = PriorityLevel.URGENT
            ),
            archiveNumber = "ARCH-2026-0005",
            subject = "مراسلة بموعد استحقاق"
        )
        assertTrue(reminderId > 0)

        // Active reminders should contain this reminder
        val active = repository.getActiveReminders().first()
        assertTrue(active.any { it.id == reminderId })

        // Mark completed
        repository.toggleReminderCompleted(reminderId, true)

        val activeAfter = repository.getActiveReminders().first()
        assertFalse(activeAfter.any { it.id == reminderId })
    }

    @Test
    fun testSafeDeletion() = runBlocking {
        val id1 = repository.createCorrespondence(
            Correspondence(
                archiveNumber = "ARCH-2026-0010",
                year = 2026,
                sequenceNumber = 10,
                type = CorrespondenceType.INCOMING,
                subject = "مراسلة سيتم حذفها",
                sender = "المرسل",
                recipient = "المستقبل"
            )
        )

        val id2 = repository.createCorrespondence(
            Correspondence(
                archiveNumber = "ARCH-2026-0011",
                year = 2026,
                sequenceNumber = 11,
                type = CorrespondenceType.OUTGOING,
                subject = "مراسلة مرتبطة تبقى محفوظة",
                sender = "المرسل",
                recipient = "المستقبل"
            )
        )

        repository.addRelation(id1, id2, RelationType.RELATED_TO)

        // Delete id1
        val deleted = repository.deleteCorrespondence(id1)
        assertTrue(deleted)

        assertNull(database.correspondenceDao().getById(id1))
        // id2 MUST still exist intact!
        assertNotNull(database.correspondenceDao().getById(id2))
    }
}
