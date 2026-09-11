package com.sakhicare.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sakhicare.app.data.db.daos.*
import com.sakhicare.app.data.db.entities.*
import com.sakhicare.app.data.security.SecureKeyStorage
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        WorkerEntity::class,
        PregnancyCaseEntity::class,
        AssessmentEntity::class,
        ObservationEntity::class,
        CaseEventEntity::class,
        OutboxItemEntity::class,
        VoiceArtifactEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workerDao(): WorkerDao
    abstract fun caseDao(): CaseDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun observationDao(): ObservationDao
    abstract fun caseEventDao(): CaseEventDao
    abstract fun outboxDao(): OutboxDao
    abstract fun voiceArtifactDao(): VoiceArtifactDao

    companion object {
        const val DB_NAME = "sakhicare_encrypted.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): AppDatabase {
            val keyStorage = SecureKeyStorage(appContext)
            val passphrase = keyStorage.getOrCreateDatabasePassphrase()
            val supportFactory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(supportFactory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS voice_artifacts (
                        id TEXT NOT NULL PRIMARY KEY,
                        caseId TEXT NOT NULL,
                        localAudioPath TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        fileSizeBytes INTEGER NOT NULL,
                        sha256Checksum TEXT NOT NULL,
                        language TEXT NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        transcript TEXT,
                        processingStatus TEXT NOT NULL,
                        uploadStatus TEXT NOT NULL,
                        retentionDeadlineTimestamp INTEGER,
                        confirmedAtTimestamp INTEGER,
                        createdAtTimestamp INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pregnancy_cases ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE pregnancy_cases ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE pregnancy_cases ADD COLUMN locationAccuracyM REAL")
                db.execSQL("ALTER TABLE pregnancy_cases ADD COLUMN locationCapturedAt INTEGER")
            }
        }

        /**
         * For unit & instrumentation tests: provides an in-memory database instance.
         */
        fun createInMemoryTestDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context,
                AppDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
