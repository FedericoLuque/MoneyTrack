package com.federico.moneytrack.domain.model

data class TransactionWithCategory(
    val transaction: Transaction,
    val category: Category?
)