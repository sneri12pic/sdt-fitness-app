package com.stepandemianenko.sdtfitness.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class,
        UserSettingsEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SessionSetLogEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun sessionExerciseDao(): SessionExerciseDao
    abstract fun sessionSetLogDao(): SessionSetLogDao

    companion object {
        private const val DATABASE_NAME = "sdt_fitness.db"
        const val MIGRATED_DEFAULT_ACCOUNT_ID: String = "migrated_guest_account"

        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys=OFF")
                db.beginTransaction()
                try {
                    val nowExpr = "(CAST(strftime('%s','now') AS INTEGER) * 1000)"

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `accounts` (
                            `id` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_isActive` ON `accounts` (`isActive`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_createdAt` ON `accounts` (`createdAt`)")

                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO `accounts` (`id`, `type`, `createdAt`, `isActive`, `updatedAt`)
                        VALUES ('$MIGRATED_DEFAULT_ACCOUNT_ID', 'guest', $nowExpr, 1, $nowExpr)
                        """
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `workout_sessions_new` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `accountId` TEXT NOT NULL,
                            `templateId` TEXT,
                            `startedAt` INTEGER NOT NULL,
                            `completedAt` INTEGER,
                            `status` TEXT NOT NULL,
                            `currentExerciseIndex` INTEGER NOT NULL,
                            `currentSetIndex` INTEGER NOT NULL,
                            `totalSetsTarget` INTEGER NOT NULL,
                            `totalSetsCompleted` INTEGER NOT NULL,
                            `totalRepsCompleted` INTEGER NOT NULL,
                            `totalVolumeCompleted` REAL NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            `deletedAt` INTEGER,
                            `syncState` TEXT NOT NULL,
                            FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """
                    )
                    db.execSQL(
                        """
                        INSERT INTO `workout_sessions_new` (
                            `id`, `accountId`, `templateId`, `startedAt`, `completedAt`, `status`,
                            `currentExerciseIndex`, `currentSetIndex`, `totalSetsTarget`, `totalSetsCompleted`,
                            `totalRepsCompleted`, `totalVolumeCompleted`, `createdAt`, `updatedAt`, `deletedAt`, `syncState`
                        )
                        SELECT
                            `id`,
                            '$MIGRATED_DEFAULT_ACCOUNT_ID',
                            `templateId`,
                            `startedAt`,
                            `completedAt`,
                            `status`,
                            `currentExerciseIndex`,
                            `currentSetIndex`,
                            `totalSetsTarget`,
                            `totalSetsCompleted`,
                            `totalRepsCompleted`,
                            `totalVolumeCompleted`,
                            `startedAt`,
                            COALESCE(`completedAt`, `startedAt`),
                            NULL,
                            'local_only'
                        FROM `workout_sessions`
                        """
                    )
                    db.execSQL("DROP TABLE `workout_sessions`")
                    db.execSQL("ALTER TABLE `workout_sessions_new` RENAME TO `workout_sessions`")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_status` ON `workout_sessions` (`status`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_startedAt` ON `workout_sessions` (`startedAt`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_completedAt` ON `workout_sessions` (`completedAt`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_accountId_status` ON `workout_sessions` (`accountId`, `status`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_accountId_completedAt` ON `workout_sessions` (`accountId`, `completedAt`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_accountId_startedAt` ON `workout_sessions` (`accountId`, `startedAt`)")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `session_exercises_new` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `accountId` TEXT NOT NULL,
                            `sessionId` INTEGER NOT NULL,
                            `exerciseId` TEXT NOT NULL,
                            `exerciseName` TEXT NOT NULL,
                            `exerciseOrder` INTEGER NOT NULL,
                            `targetSets` INTEGER NOT NULL,
                            `targetReps` INTEGER NOT NULL,
                            `targetWeightKg` INTEGER NOT NULL,
                            `status` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            `deletedAt` INTEGER,
                            `syncState` TEXT NOT NULL,
                            FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                            FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """
                    )
                    db.execSQL(
                        """
                        INSERT INTO `session_exercises_new` (
                            `id`, `accountId`, `sessionId`, `exerciseId`, `exerciseName`, `exerciseOrder`,
                            `targetSets`, `targetReps`, `targetWeightKg`, `status`, `createdAt`, `updatedAt`, `deletedAt`, `syncState`
                        )
                        SELECT
                            se.`id`,
                            ws.`accountId`,
                            se.`sessionId`,
                            se.`exerciseId`,
                            se.`exerciseName`,
                            se.`exerciseOrder`,
                            se.`targetSets`,
                            se.`targetReps`,
                            se.`targetWeightKg`,
                            se.`status`,
                            ws.`createdAt`,
                            ws.`updatedAt`,
                            NULL,
                            'local_only'
                        FROM `session_exercises` se
                        INNER JOIN `workout_sessions` ws ON ws.`id` = se.`sessionId`
                        """
                    )
                    db.execSQL("DROP TABLE `session_exercises`")
                    db.execSQL("ALTER TABLE `session_exercises_new` RENAME TO `session_exercises`")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_exercises_sessionId` ON `session_exercises` (`sessionId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_exercises_exerciseId` ON `session_exercises` (`exerciseId`)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_session_exercises_sessionId_exerciseOrder` ON `session_exercises` (`sessionId`, `exerciseOrder`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_exercises_accountId_sessionId_exerciseOrder` ON `session_exercises` (`accountId`, `sessionId`, `exerciseOrder`)")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `session_set_logs_new` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `accountId` TEXT NOT NULL,
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
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            `deletedAt` INTEGER,
                            `syncState` TEXT NOT NULL,
                            FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                            FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                            FOREIGN KEY(`sessionExerciseId`) REFERENCES `session_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """
                    )
                    db.execSQL(
                        """
                        INSERT INTO `session_set_logs_new` (
                            `id`, `accountId`, `sessionId`, `sessionExerciseId`, `setNumber`,
                            `targetWeightKg`, `actualWeightKg`, `targetReps`, `actualReps`, `rpe`,
                            `completedAt`, `source`, `createdAt`, `updatedAt`, `deletedAt`, `syncState`
                        )
                        SELECT
                            ssl.`id`,
                            ws.`accountId`,
                            ssl.`sessionId`,
                            ssl.`sessionExerciseId`,
                            ssl.`setNumber`,
                            ssl.`targetWeightKg`,
                            ssl.`actualWeightKg`,
                            ssl.`targetReps`,
                            ssl.`actualReps`,
                            ssl.`rpe`,
                            ssl.`completedAt`,
                            ssl.`source`,
                            ssl.`completedAt`,
                            ssl.`completedAt`,
                            NULL,
                            'local_only'
                        FROM `session_set_logs` ssl
                        INNER JOIN `workout_sessions` ws ON ws.`id` = ssl.`sessionId`
                        """
                    )
                    db.execSQL("DROP TABLE `session_set_logs`")
                    db.execSQL("ALTER TABLE `session_set_logs_new` RENAME TO `session_set_logs`")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_set_logs_sessionId` ON `session_set_logs` (`sessionId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_set_logs_sessionExerciseId` ON `session_set_logs` (`sessionExerciseId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_set_logs_completedAt` ON `session_set_logs` (`completedAt`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_set_logs_accountId_sessionId_completedAt` ON `session_set_logs` (`accountId`, `sessionId`, `completedAt`)")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `user_settings` (
                            `accountId` TEXT NOT NULL,
                            `dailyStepsSource` TEXT NOT NULL,
                            `dailyStepsTarget` INTEGER NOT NULL,
                            `dailyStepsCurrent` INTEGER NOT NULL,
                            `dailyStepsLastUpdated` INTEGER,
                            `workoutCompletedDatesCsv` TEXT NOT NULL,
                            `activeMinutesToday` INTEGER NOT NULL,
                            `routineCompletedDatesCsv` TEXT NOT NULL,
                            `recoveryLogDate` TEXT,
                            `recoveryLogOption` TEXT,
                            `recoveryLogAtMillis` INTEGER,
                            `quickLogDate` TEXT,
                            `quickLogType` TEXT,
                            `quickLogDurationMinutes` INTEGER NOT NULL,
                            `quickLogTimestamp` INTEGER,
                            `quickLogSource` TEXT,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            `deletedAt` INTEGER,
                            `syncState` TEXT NOT NULL,
                            PRIMARY KEY(`accountId`),
                            FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """
                    )
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_user_settings_accountId` ON `user_settings` (`accountId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_settings_updatedAt` ON `user_settings` (`updatedAt`)")

                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO `user_settings` (
                            `accountId`, `dailyStepsSource`, `dailyStepsTarget`, `dailyStepsCurrent`,
                            `dailyStepsLastUpdated`, `workoutCompletedDatesCsv`, `activeMinutesToday`, `routineCompletedDatesCsv`,
                            `recoveryLogDate`, `recoveryLogOption`, `recoveryLogAtMillis`, `quickLogDate`, `quickLogType`,
                            `quickLogDurationMinutes`, `quickLogTimestamp`, `quickLogSource`,
                            `createdAt`, `updatedAt`, `deletedAt`, `syncState`
                        ) VALUES (
                            '$MIGRATED_DEFAULT_ACCOUNT_ID', 'MANUAL', 5000, 0,
                            NULL, '', 0, '',
                            NULL, NULL, NULL, NULL, NULL,
                            0, NULL, NULL,
                            $nowExpr, $nowExpr, NULL, 'local_only'
                        )
                        """
                    )

                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                    db.execSQL("PRAGMA foreign_keys=ON")
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `healthConnectLastSyncedAt` INTEGER")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `healthConnectLastImportedSteps` INTEGER")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `healthConnectLatestWeightKg` REAL")
            }
        }

        fun getInstance(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
