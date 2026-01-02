package com.federico.moneytrack.di

import com.federico.moneytrack.data.repository.BitcoinRepositoryImpl
import com.federico.moneytrack.data.repository.TransactionRepositoryImpl
import com.federico.moneytrack.domain.repository.BitcoinRepository
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
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindBitcoinRepository(
        bitcoinRepositoryImpl: BitcoinRepositoryImpl
    ): BitcoinRepository
}
