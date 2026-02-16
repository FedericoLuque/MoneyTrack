package com.federico.moneytrack.data.repository

import com.federico.moneytrack.data.local.dao.CategoryDao
import com.federico.moneytrack.domain.model.Category
import com.federico.moneytrack.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.federico.moneytrack.data.local.entity.Category as CategoryEntity

class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return dao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return dao.getCategoryById(id)?.toDomain()
    }

    override suspend fun insertCategory(category: Category) {
        dao.insertCategory(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        dao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        dao.deleteCategory(category.toEntity())
    }

    // Mappers
    private fun CategoryEntity.toDomain(): Category {
        return Category(
            id = id,
            name = name,
            iconName = iconName,
            colorHex = colorHex,
            transactionType = transactionType
        )
    }

    private fun Category.toEntity(): CategoryEntity {
        return CategoryEntity(
            id = id,
            name = name,
            iconName = iconName,
            colorHex = colorHex,
            transactionType = transactionType
        )
    }
}
