package com.federico.moneytrack.data.local.pojo

import androidx.room.Embedded
import androidx.room.Relation
import com.federico.moneytrack.data.local.entity.Category
import com.federico.moneytrack.data.local.entity.Transaction

data class TransactionWithCategory(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: Category?
)
