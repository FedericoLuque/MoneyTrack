package com.federico.moneytrack.domain.model

data class Account(
    val id: Long = 0,
    val name: String,
    val currentBalance: Double,
    val type: String
)
