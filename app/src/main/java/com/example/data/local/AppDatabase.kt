package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Attachment
import com.example.data.model.CategoryEntity
import com.example.data.model.ContactEntity
import com.example.data.model.Correspondence
import com.example.data.model.CorrespondenceRelation
import com.example.data.model.DepartmentEntity
import com.example.data.model.FollowUpHistory
import com.example.data.model.Reminder
import com.example.data.model.TagEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Correspondence::class,
        Attachment::class,
        CorrespondenceRelation::class,
        FollowUpHistory::class,
        Reminder::class,
        DepartmentEntity::class,
        CategoryEntity::class,
        ContactEntity::class,
        TagEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun correspondenceDao(): CorrespondenceDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun relationDao(): RelationDao
    abstract fun followUpDao(): FollowUpDao
    abstract fun reminderDao(): ReminderDao
    abstract fun directoryDao(): DirectoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "administrative_archive.db"
                )
                    .addCallback(DatabasePrepopulationCallback(context.applicationContext))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabasePrepopulationCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = getDatabase(context)
                val directoryDao = database.directoryDao()

                // Pre-seed default Algerian administrative categories
                val defaultCategories = listOf(
                    CategoryEntity(name = "مراسلة رسمية", description = "المراسلات والخطابات الرسمية العامة"),
                    CategoryEntity(name = "تحقيقات إدارية", description = "تحقيقات ومتابعات إدارية وتفتيشية"),
                    CategoryEntity(name = "طلبات معلومات", description = "استفسارات وطلبات بيانات وإحصائيات"),
                    CategoryEntity(name = "شكاوى وتظلمات", description = "عرائض وشكاوى المواطنين والموظفين"),
                    CategoryEntity(name = "شؤون قانونية ومنازعات", description = "قضايا ومنازعات إدارية واستشارات"),
                    CategoryEntity(name = "صفقات عمومية ومحاسبة", description = "ملفات الصفقات، الفواتير والالتزامات المالية"),
                    CategoryEntity(name = "تقارير دورية", description = "تقارير الأنشطة الشهرية والسنوية"),
                    CategoryEntity(name = "تذكير ومتابعة إدارية", description = "إعذارات ورسائل تذكير بالمواعيد"),
                    CategoryEntity(name = "شؤون الموظفين", description = "قرارات التعيين، الترقية، الإجازات والعقوبات")
                )
                defaultCategories.forEach { directoryDao.insertCategory(it) }

                // Pre-seed default Algerian departments
                val defaultDepartments = listOf(
                    DepartmentEntity(name = "الأمانة العامة", code = "SG", directorate = "المديرية العامة", headName = "الأمين العام"),
                    DepartmentEntity(name = "مصلحة الشؤون القانونية والمنازعات", code = "SAJ", directorate = "مديرية التنظيم", headName = "رئيس المصلحة"),
                    DepartmentEntity(name = "مصلحة المستخدمين والتكوين", code = "SPF", directorate = "مديرية الموارد البشرية", headName = "رئيس مصلحة المستخدمين"),
                    DepartmentEntity(name = "مصلحة الميزانية والصفقات العمومية", code = "SMP", directorate = "مديرية المالية والوسائل", headName = "رئيس مصلحة الصفقات"),
                    DepartmentEntity(name = "مكتب البريد المركزي والأرشيف", code = "BCA", directorate = "الأمانة العامة", headName = "مسؤول الأرشيف المركزي"),
                    DepartmentEntity(name = "مصلحة التجهيز والاستثمار", code = "SEI", directorate = "مديرية التخطيط", headName = "رئيس مصلحة التجهيز")
                )
                defaultDepartments.forEach { directoryDao.insertDepartment(it) }

                // Pre-seed default contacts (institutions / authorities)
                val defaultContacts = listOf(
                    ContactEntity(name = "وزارة الداخلية والجماعات المحلية والتهيئة العمرانية", organization = "الإدارة المركزية", role = "الوزارة الوصية", phone = "021731111", email = "contact@interieur.gov.dz"),
                    ContactEntity(name = "السيد والي الولاية - الديوان", organization = "ولاية الجزائر", role = "السلطة الولائية", phone = "021711212", email = "cabinet@wilaya.dz"),
                    ContactEntity(name = "الخزينة العمومية للولاية", organization = "وزارة المالية", role = "الرقابة المالية والمحاسبية", phone = "021631415", email = "tresor@mf.gov.dz"),
                    ContactEntity(name = "مجلس قضاء الولاية", organization = "وزارة العدل", role = "الهيئة القضائية", phone = "021822020", email = "justice@cour.dz"),
                    ContactEntity(name = "المفتشية العامة للولاية", organization = "الولاية", role = "التفتيش والرقابة", phone = "021735500", email = "inspection@wilaya.dz")
                )
                defaultContacts.forEach { directoryDao.insertContact(it) }

                // Pre-seed common tags
                listOf("عاجل_جداً", "للمتابعة", "يتطلب_رد", "سري", "صفقة_عمومية", "تظلم_مواطن", "تحقيق").forEach {
                    directoryDao.insertTag(TagEntity(name = it))
                }
            }
        }
    }
}
