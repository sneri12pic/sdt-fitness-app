package com.stepandemianenko.sdtfitness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: UserSettingsEntity)

    @Query("SELECT * FROM user_settings WHERE accountId = :accountId LIMIT 1")
    suspend fun getByAccountId(accountId: String): UserSettingsEntity?

    @Query("DELETE FROM user_settings WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}
