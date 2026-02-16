package com.federico.moneytrack.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.federico.moneytrack.R
import com.federico.moneytrack.databinding.ItemTransactionBinding
import com.federico.moneytrack.domain.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransactionViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            // TODO: Cuando integremos las categorías reales, usar el nombre/icono de la categoría
            binding.tvNote.text = transaction.note ?: "Sin nota"
            binding.tvDate.text = dateFormat.format(Date(transaction.date))
            binding.tvAmount.text = currencyFormat.format(transaction.amount)
            
            // Color por defecto (asumiremos gasto en rojo por ahora, esto se refinará con la lógica de categorías)
            // En una app real, deberíamos cruzar el categoryId para saber si es Gasto o Ingreso y colorear.
            // Por simplicidad visual, usaremos negro por ahora.
            binding.tvAmount.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.black))
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
