package com.stepandemianenko.sdtfitness.data.account

import androidx.room.withTransaction
import com.stepandemianenko.sdtfitness.data.local.AccountDao
import com.stepandemianenko.sdtfitness.data.local.AccountEntity
import com.stepandemianenko.sdtfitness.data.local.AccountType
import com.stepandemianenko.sdtfitness.data.local.SessionExerciseDao
import com.stepandemianenko.sdtfitness.data.local.SessionSetLogDao
import com.stepandemianenko.sdtfitness.data.local.SyncState
import com.stepandemianenko.sdtfitness.data.local.UserSettingsDao
import com.stepandemianenko.sdtfitness.data.local.UserSettingsEntity
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import com.stepandemianenko.sdtfitness.data.local.WorkoutSessionDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AccountSummary(
    val id: String,
    val type: String,
    val createdAt: Long,
    val isActive: Boolean,
    val remoteUserId: String?,
    val email: String?,
    val displayName: String?,
    val authProvider: String?,
    val lastLoginAt: Long?
)

data class AccountScopeKey(
    val accountId: String,
    val revision: Long
)

class AccountSessionManager(
    private val database: WorkoutDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val accountDao: AccountDao = database.accountDao()
    private val userSettingsDao: UserSettingsDao = database.userSettingsDao()
    private val sessionDao: WorkoutSessionDao = database.workoutSessionDao()
    private val sessionExerciseDao: SessionExerciseDao = database.sessionExerciseDao()
    private val setLogDao: SessionSetLogDao = database.sessionSetLogDao()

    private val _activeAccountId = MutableStateFlow<String?>(null)
    val activeAccountId: StateFlow<String?> = _activeAccountId.asStateFlow()
    private val _scopeRevision = MutableStateFlow(0L)

    val nonNullActiveAccountId: Flow<String> = activeAccountId.filterNotNull()
    val accountScope: Flow<AccountScopeKey> = combine(nonNullActiveAccountId, _scopeRevision) { accountId, revision ->
        AccountScopeKey(accountId = accountId, revision = revision)
    }

    init {
        scope.launch {
            val accountId = ensureBootstrappedAccount()
            _activeAccountId.value = accountId
            bumpScopeRevision()
        }
    }

    suspend fun requireActiveAccountId(): String {
        _activeAccountId.value?.let { return it }
        return ensureBootstrappedAccount().also { resolved ->
            _activeAccountId.update { resolved }
        }
    }

    fun observeAccounts(): Flow<List<AccountEntity>> = accountDao.observeAllAccounts()

    suspend fun linkAuthenticatedAccountAndSwitch(
        remoteUserId: String,
        email: String,
        displayName: String?,
        authProvider: String
    ): String {
        val now = System.currentTimeMillis()
        val normalizedEmail = email.trim()
        val normalizedProvider = authProvider.trim().ifBlank { DEFAULT_AUTH_PROVIDER }
        val accountId = database.withTransaction {
            val existing = accountDao.getByRemoteIdentity(
                authProvider = normalizedProvider,
                remoteUserId = remoteUserId
            )

            val resolvedAccountId = existing?.id ?: UUID.randomUUID().toString().also { newAccountId ->
                accountDao.insert(
                    AccountEntity(
                        id = newAccountId,
                        type = AccountType.AUTH,
                        createdAt = now,
                        isActive = false,
                        updatedAt = now,
                        remoteUserId = remoteUserId,
                        email = normalizedEmail,
                        displayName = displayName,
                        authProvider = normalizedProvider,
                        lastLoginAt = now
                    )
                )
                ensureUserSettings(accountId = newAccountId, now = now)
            }

            accountDao.updateAuthProfile(
                accountId = resolvedAccountId,
                type = AccountType.AUTH,
                remoteUserId = remoteUserId,
                email = normalizedEmail,
                displayName = displayName,
                authProvider = normalizedProvider,
                lastLoginAt = now,
                updatedAt = now
            )
            accountDao.deactivateAll(updatedAt = now)
            accountDao.activate(accountId = resolvedAccountId, updatedAt = now)
            ensureUserSettings(accountId = resolvedAccountId, now = now)
            resolvedAccountId
        }

        _activeAccountId.value = accountId
        bumpScopeRevision()
        return accountId
    }

    suspend fun switchToGuestAccount(): String {
        val now = System.currentTimeMillis()
        val accountId = database.withTransaction {
            val guest = accountDao.getAllAccounts()
                .filter { it.type == AccountType.GUEST }
                .maxByOrNull { it.createdAt }

            val resolvedAccountId = guest?.id ?: UUID.randomUUID().toString().also { guestId ->
                accountDao.insert(
                    AccountEntity(
                        id = guestId,
                        type = AccountType.GUEST,
                        createdAt = now,
                        isActive = false,
                        updatedAt = now
                    )
                )
            }

            accountDao.deactivateAll(updatedAt = now)
            accountDao.activate(accountId = resolvedAccountId, updatedAt = now)
            ensureUserSettings(accountId = resolvedAccountId, now = now)
            resolvedAccountId
        }

        _activeAccountId.value = accountId
        bumpScopeRevision()
        return accountId
    }

    suspend fun createTestUserAndSwitch(): String {
        val now = System.currentTimeMillis()
        val accountId = UUID.randomUUID().toString()

        database.withTransaction {
            accountDao.deactivateAll(updatedAt = now)
            accountDao.insert(
                AccountEntity(
                    id = accountId,
                    type = AccountType.GUEST,
                    createdAt = now,
                    isActive = true,
                    updatedAt = now
                )
            )
            ensureUserSettings(accountId = accountId, now = now)
        }

        _activeAccountId.value = accountId
        bumpScopeRevision()
        return accountId
    }

    suspend fun switchActiveAccount(accountId: String): Boolean {
        val now = System.currentTimeMillis()
        val exists = accountDao.getById(accountId) != null
        if (!exists) return false

        database.withTransaction {
            accountDao.deactivateAll(updatedAt = now)
            accountDao.activate(accountId = accountId, updatedAt = now)
            ensureUserSettings(accountId = accountId, now = now)
        }

        _activeAccountId.value = accountId
        bumpScopeRevision()
        return true
    }

    suspend fun wipeAccountData(accountId: String) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            setLogDao.deleteAllForAccount(accountId)
            sessionExerciseDao.deleteAllForAccount(accountId)
            sessionDao.deleteAllForAccount(accountId)
            userSettingsDao.deleteForAccount(accountId)
            ensureUserSettings(accountId = accountId, now = now)
        }
        bumpScopeRevision()
    }

    suspend fun currentAccountSummary(): AccountSummary {
        val activeId = requireActiveAccountId()
        val active = accountDao.getById(activeId)
            ?: error("Active account missing: $activeId")
        return active.toSummary()
    }

    private suspend fun ensureBootstrappedAccount(): String {
        return database.withTransaction {
            val now = System.currentTimeMillis()

            val active = accountDao.getActiveAccount()
            if (active != null) {
                ensureUserSettings(active.id, now)
                return@withTransaction active.id
            }

            val existing = accountDao.getMostRecentAccount()
            if (existing != null) {
                accountDao.deactivateAll(updatedAt = now)
                accountDao.activate(accountId = existing.id, updatedAt = now)
                ensureUserSettings(existing.id, now)
                return@withTransaction existing.id
            }

            val guestId = UUID.randomUUID().toString()
            accountDao.insert(
                AccountEntity(
                    id = guestId,
                    type = AccountType.GUEST,
                    createdAt = now,
                    isActive = true,
                    updatedAt = now
                )
            )
            ensureUserSettings(guestId, now)
            guestId
        }
    }

    private suspend fun ensureUserSettings(accountId: String, now: Long) {
        val current = userSettingsDao.getByAccountId(accountId)
        if (current != null) return
        userSettingsDao.upsert(
            UserSettingsEntity(
                accountId = accountId,
                createdAt = now,
                updatedAt = now,
                syncState = SyncState.LOCAL_ONLY
            )
        )
    }

    private fun AccountEntity.toSummary(): AccountSummary {
        return AccountSummary(
            id = id,
            type = type,
            createdAt = createdAt,
            isActive = isActive,
            remoteUserId = remoteUserId,
            email = email,
            displayName = displayName,
            authProvider = authProvider,
            lastLoginAt = lastLoginAt
        )
    }

    private fun bumpScopeRevision() {
        _scopeRevision.update { current -> current + 1L }
    }

    private companion object {
        const val DEFAULT_AUTH_PROVIDER = "password"
    }
}
