package com.federico.moneytrack.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val transactionType: String // "INCOME" or "EXPENSE"
)
