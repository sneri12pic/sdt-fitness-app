package com.stepandemianenko.sdtfitness.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDatabaseMigrationTest {

    @Test
    fun migrateFrom1To3_backfillsAccountAndScopesData() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val dbName = "workout-migration-test"
            context.deleteDatabase(dbName)

        val openHelperFactory = FrameworkSQLiteOpenHelperFactory()
        val createV1Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `workout_sessions` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `templateId` TEXT,
                            `startedAt` INTEGER NOT NULL,
                            `completedAt` INTEGER,
                            `status` TEXT NOT NULL,
                            `currentExerciseIndex` INTEGER NOT NULL,
                            `currentSetIndex` INTEGER NOT NULL,
                            `totalSetsTarget` INTEGER NOT NULL,
                            `totalSetsCompleted` INTEGER NOT NULL,
                            `totalRepsCompleted` INTEGER NOT NULL,
                            `totalVolumeCompleted` REAL NOT NULL
                        )
                        """
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_status` ON `workout_sessions` (`status`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_startedAt` ON `workout_sessions` (`startedAt`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_completedAt` ON `workout_sessions` (`completedAt`)")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `session_exercises` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `sessionId` INTEGER NOT NULL,
                            `exerciseId` TEXT NOT NULL,
                            `exerciseName` TEXT NOT NULL,
                            `exerciseOrder` INTEGER NOT NULL,
                            `targetSets` INTEGER NOT NULL,
                            `targetReps` INTEGER NOT NULL,
                            `targetWeightKg` INTEGER NOT NULL,
                            `status` TEXT NOT NULL,
                            FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_exercises_sessionId` ON `session_exercises` (`sessionId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_exercises_exerciseId` ON `session_exercises` (`exerciseId`)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_session_exercises_sessionId_exerciseOrder` ON `session_exercises` (`sessionId`, `exerciseOrder`)")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `session_set_logs` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `sessionId` INTEGER NOT NULL,
                            `sessionExerciseId` INTEGER NOT NULL,
                            `setNumber` INTEGER NOT NULL,
                            `targetWeightKg` INTEGER NOT NULL,
                            `actualWeightKg` INTEGER NOT NULL,
                            `targetReps` INTEGER NOT NULL,
                            `actualReps` INTEGER NOT NULL,
                            `rpe` INTEGER,
                            `completedAt` INTEGER NOT NULL,
                            `source` TEXT NOT NULL,
                            FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                            FOREIGN KEY(`sessionExerciseId`) REFERENCES `session_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_set_logs_sessionId` ON `session_set_logs` (`sessionId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_set_logs_sessionExerciseId` ON `session_set_logs` (`sessionExerciseId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_set_logs_completedAt` ON `session_set_logs` (`completedAt`)")

                    db.execSQL(
                        """
                        INSERT INTO `workout_sessions` (
                            `id`, `templateId`, `startedAt`, `completedAt`, `status`,
                            `currentExerciseIndex`, `currentSetIndex`, `totalSetsTarget`,
                            `totalSetsCompleted`, `totalRepsCompleted`, `totalVolumeCompleted`
                        ) VALUES (1, 'upper_body', 1000, NULL, 'active', 0, 0, 4, 0, 0, 0.0)
                        """
                    )
                    db.execSQL(
                        """
                        INSERT INTO `session_exercises` (
                            `id`, `sessionId`, `exerciseId`, `exerciseName`, `exerciseOrder`,
                            `targetSets`, `targetReps`, `targetWeightKg`, `status`
                        ) VALUES (1, 1, 'bench_press', 'Bench Press', 0, 4, 8, 60, 'active')
                        """
                    )
                    db.execSQL(
                        """
                        INSERT INTO `session_set_logs` (
                            `id`, `sessionId`, `sessionExerciseId`, `setNumber`, `targetWeightKg`,
                            `actualWeightKg`, `targetReps`, `actualReps`, `rpe`, `completedAt`, `source`
                        ) VALUES (1, 1, 1, 1, 60, 60, 8, 8, 7, 2000, 'manual')
                        """
                    )
                }

                override fun onUpgrade(
                    db: androidx.sqlite.db.SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) = Unit
            })
            .build()

        openHelperFactory.create(createV1Config).apply {
            writableDatabase.close()
            close()
        }

        val db = Room.databaseBuilder(context, WorkoutDatabase::class.java, dbName)
            .addMigrations(
                WorkoutDatabase.MIGRATION_1_2,
                WorkoutDatabase.MIGRATION_2_3
            )
            .build()

        db.openHelper.writableDatabase

        val migratedAccount = db.accountDao().getActiveAccount()
        assertNotNull(migratedAccount)
        assertEquals(WorkoutDatabase.MIGRATED_DEFAULT_ACCOUNT_ID, migratedAccount?.id)

        val migratedSessions = db.workoutSessionDao().getByStatus(
            accountId = WorkoutDatabase.MIGRATED_DEFAULT_ACCOUNT_ID,
            status = WorkoutSessionStatus.ACTIVE
        )
        assertEquals(1, migratedSessions.size)

        val migratedSettings = db.userSettingsDao().getByAccountId(WorkoutDatabase.MIGRATED_DEFAULT_ACCOUNT_ID)
        assertNotNull(migratedSettings)
        assertEquals(null, migratedSettings?.healthConnectLastImportedSteps)
        assertEquals(null, migratedSettings?.healthConnectLatestWeightKg)
        assertEquals(null, migratedSettings?.healthConnectLastSyncedAt)

        val now = System.currentTimeMillis()
        val secondAccountId = "test-account-2"
        db.accountDao().insert(
            AccountEntity(
                id = secondAccountId,
                type = AccountType.GUEST,
                createdAt = now,
                isActive = false,
                updatedAt = now
            )
        )
        db.userSettingsDao().upsert(
            UserSettingsEntity(
                accountId = secondAccountId,
                createdAt = now,
                updatedAt = now
            )
        )
        db.workoutSessionDao().insert(
            WorkoutSessionEntity(
                accountId = secondAccountId,
                templateId = "lower_body",
                startedAt = now,
                completedAt = now + 1_000,
                status = WorkoutSessionStatus.COMPLETED,
                currentExerciseIndex = 0,
                currentSetIndex = 0,
                totalSetsTarget = 3,
                totalSetsCompleted = 3,
                totalRepsCompleted = 30,
                totalVolumeCompleted = 1200.0,
                createdAt = now,
                updatedAt = now + 1_000
            )
        )

        val scopedMigrated = db.workoutSessionDao().getByStatus(
            accountId = WorkoutDatabase.MIGRATED_DEFAULT_ACCOUNT_ID,
            status = WorkoutSessionStatus.ACTIVE
        )
        val scopedSecond = db.workoutSessionDao().getByStatus(
            accountId = secondAccountId,
            status = WorkoutSessionStatus.COMPLETED
        )
        assertEquals(1, scopedMigrated.size)
        assertEquals(1, scopedSecond.size)

            db.close()
            context.deleteDatabase(dbName)
        }
    }
}
