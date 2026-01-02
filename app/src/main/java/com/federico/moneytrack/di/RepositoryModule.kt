package com.federico.moneytrack.di

import com.federico.moneytrack.data.repository.AccountRepositoryImpl
import com.federico.moneytrack.data.repository.BitcoinRepositoryImpl
import com.federico.moneytrack.data.repository.CategoryRepositoryImpl
import com.federico.moneytrack.data.repository.TransactionRepositoryImpl
import com.federico.moneytrack.domain.repository.AccountRepository
import com.federico.moneytrack.domain.repository.BitcoinRepository
import com.federico.moneytrack.domain.repository.CategoryRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        accountRepositoryImpl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindBitcoinRepository(
        bitcoinRepositoryImpl: BitcoinRepositoryImpl
    ): BitcoinRepository
}
