package com.stepandemianenko.sdtfitness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CreatineIntakeLogDao {

    @Insert
    suspend fun insert(log: CreatineIntakeLogEntity): Long

    @Query(
        """
        SELECT * FROM creatine_intake_logs
        WHERE accountId = :accountId AND date = :date AND deletedAt IS NULL
        ORDER BY timestamp DESC, id DESC
        """
    )
    suspend fun getForDate(accountId: String, date: String): List<CreatineIntakeLogEntity>

    @Query(
        """
        UPDATE creatine_intake_logs
        SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncState = :syncState
        WHERE accountId = :accountId AND id = :logId AND deletedAt IS NULL
        """
    )
    suspend fun markDeleted(
        accountId: String,
        logId: Long,
        deletedAt: Long,
        syncState: String
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM creatine_intake_logs
        WHERE accountId = :accountId AND deletedAt IS NULL AND timestamp >= :sinceMillis
        """
    )
    suspend fun countSince(accountId: String, sinceMillis: Long): Int

    @Query("DELETE FROM creatine_intake_logs WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: String)
}
