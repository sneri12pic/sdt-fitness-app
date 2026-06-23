package com.stepandemianenko.sdtfitness.home

import com.stepandemianenko.sdtfitness.data.account.AccountSessionManager
import com.stepandemianenko.sdtfitness.data.health.HealthConnectManager
import com.stepandemianenko.sdtfitness.data.local.AccountEntity
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies HomeViewModel can be built from plain (mocked) dependencies — i.e. the constructor-injection
 * refactor made it unit-testable without a real database — and that it maps account state from
 * AccountSessionManager into its UI state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `maps active account and account list from session manager into ui state`() {
        val repository = mockk<HomeRepository>(relaxed = true)
        every { repository.dashboardState } returns MutableStateFlow(HomeDashboardState())

        val account = AccountEntity(
            id = "acct-1",
            type = "guest",
            createdAt = 100L,
            isActive = true,
            updatedAt = 100L
        )
        val accountSessionManager = mockk<AccountSessionManager>(relaxed = true)
        every { accountSessionManager.observeAccounts() } returns flowOf(listOf(account))
        every { accountSessionManager.nonNullActiveAccountId } returns MutableStateFlow("acct-1")

        val healthConnectManager = mockk<HealthConnectManager>(relaxed = true)

        val viewModel = HomeViewModel(
            repository = repository,
            accountSessionManager = accountSessionManager,
            healthConnectManager = healthConnectManager
        )

        val state = viewModel.uiState.value
        assertEquals("acct-1", state.activeAccountId)
        assertEquals(1, state.accounts.size)
        assertEquals("acct-1", state.accounts.first().id)
        assertTrue(state.accounts.first().isActive)
    }
}
