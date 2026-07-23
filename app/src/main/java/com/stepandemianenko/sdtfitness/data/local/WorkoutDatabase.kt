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
        ExerciseCatalogEntity::class,
        DailyQuestRecordEntity::class,
        CreatineIntakeLogEntity::class,
        WaterIntakeLogEntity::class,
        WorkoutPlanEntity::class,
        WorkoutPlanExerciseEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SessionSetLogEntity::class
    ],
    version = 13,
    exportSchema = true
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun exerciseCatalogDao(): ExerciseCatalogDao
    abstract fun dailyQuestRecordDao(): DailyQuestRecordDao
    abstract fun creatineIntakeLogDao(): CreatineIntakeLogDao
    abstract fun waterIntakeLogDao(): WaterIntakeLogDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `remoteUserId` TEXT")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `email` TEXT")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `displayName` TEXT")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `authProvider` TEXT")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `lastLoginAt` INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_remoteUserId` ON `accounts` (`remoteUserId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_authProvider_remoteUserId` ON `accounts` (`authProvider`, `remoteUserId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_quest_records` (
                        `accountId` TEXT NOT NULL,
                        `questId` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `isAdded` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `completionSource` TEXT,
                        `completedAt` INTEGER,
                        `valueKg` REAL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        `syncState` TEXT NOT NULL,
                        PRIMARY KEY(`accountId`, `questId`, `date`),
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_quest_records_accountId_date` ON `daily_quest_records` (`accountId`, `date`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_quest_records_accountId_questId_date` ON `daily_quest_records` (`accountId`, `questId`, `date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_quest_records_updatedAt` ON `daily_quest_records` (`updatedAt`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout_plans` (
                        `accountId` TEXT NOT NULL,
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        `syncState` TEXT NOT NULL,
                        PRIMARY KEY(`accountId`, `id`),
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_plans_accountId` ON `workout_plans` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_plans_accountId_name` ON `workout_plans` (`accountId`, `name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_plans_accountId_updatedAt` ON `workout_plans` (`accountId`, `updatedAt`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout_plan_exercises` (
                        `accountId` TEXT NOT NULL,
                        `planId` TEXT NOT NULL,
                        `exerciseId` TEXT NOT NULL,
                        `exerciseOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        `syncState` TEXT NOT NULL,
                        PRIMARY KEY(`accountId`, `planId`, `exerciseId`),
                        FOREIGN KEY(`accountId`, `planId`) REFERENCES `workout_plans`(`accountId`, `id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_plan_exercises_accountId_planId` ON `workout_plan_exercises` (`accountId`, `planId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workout_plan_exercises_accountId_planId_exerciseOrder` ON `workout_plan_exercises` (`accountId`, `planId`, `exerciseOrder`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exercise_catalog` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `muscleGroup` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_catalog_muscleGroup` ON `exercise_catalog` (`muscleGroup`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_catalog_title` ON `exercise_catalog` (`title`)")
                SeedExerciseCatalog.seed(db)
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `creatineQuestEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `creatineTargetGrams` INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `creatinePortionGrams` INTEGER NOT NULL DEFAULT 5")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `creatine_intake_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accountId` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `amountGrams` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        `syncState` TEXT NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_creatine_intake_logs_accountId_date` ON `creatine_intake_logs` (`accountId`, `date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_creatine_intake_logs_accountId_date_timestamp` ON `creatine_intake_logs` (`accountId`, `date`, `timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_creatine_intake_logs_updatedAt` ON `creatine_intake_logs` (`updatedAt`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `weightInQuestEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    UPDATE `user_settings`
                    SET `weightInQuestEnabled` = 1
                    WHERE EXISTS (
                        SELECT 1
                        FROM `daily_quest_records`
                        WHERE `daily_quest_records`.`accountId` = `user_settings`.`accountId`
                          AND `daily_quest_records`.`questId` = 'weight_in'
                          AND `daily_quest_records`.`isAdded` = 1
                    )
                    """
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `routineGoalId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `routineFrequencyId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `routineDayIdsCsv` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `routineReminderEnabled` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `routineReminderTimesCsv` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `routineCustomReminderTimesCsv` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `waterQuestEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `waterTargetMl` INTEGER NOT NULL DEFAULT 2000")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `waterPortionMl` INTEGER NOT NULL DEFAULT 250")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `water_intake_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accountId` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `amountMl` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        `syncState` TEXT NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_water_intake_logs_accountId_date` ON `water_intake_logs` (`accountId`, `date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_water_intake_logs_accountId_date_timestamp` ON `water_intake_logs` (`accountId`, `date`, `timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_water_intake_logs_updatedAt` ON `water_intake_logs` (`updatedAt`)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `healthShareMode` TEXT NOT NULL DEFAULT 'OFF'")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `restDayDatesCsv` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `quickLogDatesCsv` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    UPDATE `user_settings`
                    SET `restDayDatesCsv` = COALESCE(`recoveryLogDate`, '')
                    WHERE `recoveryLogOption` = 'REST_DAY'
                      AND `recoveryLogDate` IS NOT NULL
                    """
                )
                db.execSQL(
                    """
                    UPDATE `user_settings`
                    SET `quickLogDatesCsv` = COALESCE(`quickLogDate`, '')
                    WHERE `quickLogDate` IS NOT NULL
                    """
                )
            }
        }

        fun getInstance(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
