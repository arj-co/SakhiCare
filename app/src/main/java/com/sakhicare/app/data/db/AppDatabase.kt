package com.sakhicare.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 2,
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
                .fallbackToDestructiveMigration()
                .build()
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
