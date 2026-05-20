package com.federico.moneytrack.ui.dashboard

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.federico.moneytrack.R
import com.federico.moneytrack.databinding.ItemTransactionBinding
import com.federico.moneytrack.domain.model.TransactionWithCategory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val onItemClick: ((TransactionWithCategory) -> Unit)? = null
) : ListAdapter<TransactionWithCategory, TransactionAdapter.TransactionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransactionViewHolder(
        private val binding: ItemTransactionBinding,
        private val onItemClick: ((TransactionWithCategory) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionWithCategory) {
            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }

            val transaction = item.transaction
            val category = item.category

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            binding.tvNote.text = transaction.note ?: "Sin nota"
            binding.tvDate.text = dateFormat.format(Date(transaction.date))

            val isIncome = if (category != null) category.transactionType == "INCOME" else transaction.amount >= 0
            val colorRes = if (isIncome) R.color.color_income else R.color.color_expense
            binding.tvAmount.setTextColor(ContextCompat.getColor(binding.root.context, colorRes))
            binding.tvAmount.text = currencyFormat.format(transaction.amount)

            if (category != null) {
                binding.tvCategoryIcon.text = category.name.firstOrNull()?.toString()?.uppercase() ?: "₿"
                try {
                    binding.tvCategoryIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor(category.colorHex))
                } catch (e: Exception) {
                    binding.tvCategoryIcon.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
                }
            } else {
                binding.tvCategoryIcon.text = "₿"
                binding.tvCategoryIcon.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(binding.root.context, R.color.color_bitcoin)
                )
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<TransactionWithCategory>() {
        override fun areItemsTheSame(oldItem: TransactionWithCategory, newItem: TransactionWithCategory): Boolean {
            return oldItem.transaction.id == newItem.transaction.id
        }

        override fun areContentsTheSame(oldItem: TransactionWithCategory, newItem: TransactionWithCategory): Boolean {
            return oldItem == newItem
        }
    }
}
