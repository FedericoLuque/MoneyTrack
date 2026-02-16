package com.federico.moneytrack.data.repository

import com.federico.moneytrack.data.local.dao.AccountDao
import com.federico.moneytrack.domain.model.Account
import com.federico.moneytrack.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.federico.moneytrack.data.local.entity.Account as AccountEntity

class AccountRepositoryImpl @Inject constructor(
    private val dao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<Account>> {
        return dao.getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAccountById(id: Long): Account? {
        return dao.getAccountById(id)?.toDomain()
    }

    override suspend fun insertAccount(account: Account) {
        dao.insertAccount(account.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        dao.updateAccount(account.toEntity())
    }

    override suspend fun deleteAccount(account: Account) {
        dao.deleteAccount(account.toEntity())
    }

    // Mappers
    private fun AccountEntity.toDomain(): Account {
        return Account(
            id = id,
            name = name,
            currentBalance = currentBalance,
            type = type
        )
    }

    private fun Account.toEntity(): AccountEntity {
        return AccountEntity(
            id = id,
            name = name,
            currentBalance = currentBalance,
            type = type
        )
    }
}
