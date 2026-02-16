package com.federico.moneytrack.domain.usecase

import com.federico.moneytrack.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetTotalBalanceUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    operator fun invoke(): Flow<Double> {
        return repository.getAllAccounts().map { accounts ->
            accounts.sumOf { it.currentBalance }
        }
    }
}
