package com.stepandemianenko.sdtfitness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyQuestRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: DailyQuestRecordEntity)

    @Query(
        """
        SELECT * FROM daily_quest_records
        WHERE accountId = :accountId AND questId = :questId AND date = :date
        LIMIT 1
        """
    )
    suspend fun getByQuestAndDate(
        accountId: String,
        questId: String,
        date: String
    ): DailyQuestRecordEntity?

    @Query(
        """
        SELECT * FROM daily_quest_records
        WHERE accountId = :accountId AND date = :date AND deletedAt IS NULL
        """
    )
    suspend fun getForDate(accountId: String, date: String): List<DailyQuestRecordEntity>

    @Query("DELETE FROM daily_quest_records WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: String)
}
