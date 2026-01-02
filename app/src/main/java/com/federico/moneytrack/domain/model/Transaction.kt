package com.federico.moneytrack.domain.model

data class Transaction(
    val id: Long = 0,
    val accountId: Long,
    val categoryId: Long?,
    val amount: Double,
    val date: Long,
    val note: String?
)
