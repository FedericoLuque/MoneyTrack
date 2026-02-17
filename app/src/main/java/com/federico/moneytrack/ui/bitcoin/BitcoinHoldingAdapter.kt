package com.federico.moneytrack.ui.bitcoin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.federico.moneytrack.databinding.ItemBitcoinHoldingBinding
import com.federico.moneytrack.domain.model.BitcoinHolding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BitcoinHoldingAdapter : ListAdapter<BitcoinHolding, BitcoinHoldingAdapter.HoldingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HoldingViewHolder {
        val binding = ItemBitcoinHoldingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HoldingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HoldingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HoldingViewHolder(
        private val binding: ItemBitcoinHoldingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BitcoinHolding) {
            val isBuy = item.satsAmount >= 0
            val satsFormat = NumberFormat.getIntegerInstance(Locale.getDefault())
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            binding.tvType.text = if (isBuy) "Compra" else "Venta"
            binding.tvDate.text = dateFormat.format(Date(item.lastUpdate))

            val satsText = if (isBuy) "+${satsFormat.format(item.satsAmount)} sats" else "${satsFormat.format(item.satsAmount)} sats"
            binding.tvSats.text = satsText
            binding.tvSats.setTextColor(if (isBuy) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<BitcoinHolding>() {
        override fun areItemsTheSame(oldItem: BitcoinHolding, newItem: BitcoinHolding): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BitcoinHolding, newItem: BitcoinHolding): Boolean {
            return oldItem == newItem
        }
    }
}
