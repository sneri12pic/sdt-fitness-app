package com.stepandemianenko.sdtfitness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveAccount(): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    fun observeAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    suspend fun getAllAccounts(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    suspend fun getById(accountId: String): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY createdAt DESC LIMIT 1")
    suspend fun getMostRecentAccount(): AccountEntity?

    @Query("UPDATE accounts SET isActive = 0, updatedAt = :updatedAt")
    suspend fun deactivateAll(updatedAt: Long)

    @Query("UPDATE accounts SET isActive = 1, updatedAt = :updatedAt WHERE id = :accountId")
    suspend fun activate(accountId: String, updatedAt: Long)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteById(accountId: String)
}
