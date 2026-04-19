package com.federico.moneytrack.domain.model

data class Budget(
    val id: Long = 0,
    val categoryId: Long,
    val limitAmount: Double,
    val periodMonth: Int,
    val periodYear: Int
)
