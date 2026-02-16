package com.federico.moneytrack.domain.model

data class DailyCashFlow(
    val dayLabel: String,
    val incomeAmount: Double,
    val expenseAmount: Double
)
