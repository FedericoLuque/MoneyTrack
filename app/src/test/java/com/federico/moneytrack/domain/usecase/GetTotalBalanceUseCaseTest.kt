package com.federico.moneytrack.domain.usecase

import com.federico.moneytrack.domain.model.Account
import com.federico.moneytrack.domain.repository.AccountRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetTotalBalanceUseCaseTest {

    private lateinit var repository: AccountRepository
    private lateinit var useCase: GetTotalBalanceUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetTotalBalanceUseCase(repository)
    }

    @Test
    fun `suma correcta de saldos de multiples cuentas`() = runTest {
        val accounts = listOf(
            Account(id = 1, name = "Efectivo", currentBalance = 500.0, type = "cash"),
            Account(id = 2, name = "Banco", currentBalance = 1500.0, type = "bank"),
            Account(id = 3, name = "Ahorros", currentBalance = 3000.0, type = "savings")
        )
        every { repository.getAllAccounts() } returns flowOf(accounts)

        val result = useCase().first()

        assertEquals(5000.0, result, 0.01)
    }

    @Test
    fun `lista vacia retorna 0`() = runTest {
        every { repository.getAllAccounts() } returns flowOf(emptyList())

        val result = useCase().first()

        assertEquals(0.0, result, 0.01)
    }
}
