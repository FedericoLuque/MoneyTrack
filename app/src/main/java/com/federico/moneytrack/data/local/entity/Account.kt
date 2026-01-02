package com.federico.moneytrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "current_balance")
    val currentBalance: Double,
    val type: String // e.g., "CASH", "BANK", "SAVINGS"
)
