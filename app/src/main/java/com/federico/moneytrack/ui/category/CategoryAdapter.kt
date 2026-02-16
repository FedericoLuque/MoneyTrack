package com.federico.moneytrack.ui.category

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.federico.moneytrack.databinding.ItemCategoryBinding
import com.federico.moneytrack.domain.model.Category

class CategoryAdapter : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CategoryViewHolder(private val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category) {
            binding.tvCategoryName.text = category.name
            binding.tvCategoryType.text = if (category.transactionType == "INCOME") "INGRESO" else "GASTO"
            
            try {
                binding.vCategoryColor.backgroundTintList = ColorStateList.valueOf(Color.parseColor(category.colorHex))
            } catch (e: Exception) {
                // Color por defecto si falla el hex
                binding.vCategoryColor.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}
