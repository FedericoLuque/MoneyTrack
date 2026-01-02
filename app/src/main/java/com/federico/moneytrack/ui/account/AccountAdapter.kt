package com.federico.moneytrack.ui.account

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.federico.moneytrack.databinding.ItemAccountBinding
import com.federico.moneytrack.domain.model.Account
import java.text.NumberFormat
import java.util.Locale

class AccountAdapter : ListAdapter<Account, AccountAdapter.AccountViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AccountViewHolder(private val binding: ItemAccountBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(account: Account) {
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            
            binding.tvAccountName.text = account.name
            binding.tvAccountType.text = account.type
            binding.tvAccountBalance.text = currencyFormat.format(account.currentBalance)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Account>() {
        override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem == newItem
        }
    }
}
