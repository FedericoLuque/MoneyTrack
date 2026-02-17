package com.federico.moneytrack.ui.bitcoin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.federico.moneytrack.R
import com.federico.moneytrack.databinding.FragmentBitcoinHoldingDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class BitcoinHoldingDetailFragment : Fragment() {

    private var _binding: FragmentBitcoinHoldingDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BitcoinHoldingDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBitcoinHoldingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.detail_delete_confirm_title))
                .setMessage(getString(R.string.detail_delete_confirm_message))
                .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                    viewModel.deleteHolding()
                }
                .setNegativeButton(getString(R.string.settings_delete_all_cancel), null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        when (state) {
                            is BitcoinHoldingDetailViewModel.BitcoinHoldingDetailState.Loading -> { }
                            is BitcoinHoldingDetailViewModel.BitcoinHoldingDetailState.Loaded -> {
                                bindState(state)
                            }
                        }
                    }
                }
                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is BitcoinHoldingDetailViewModel.UiEvent.Deleted -> {
                                Toast.makeText(requireContext(), getString(R.string.detail_deleted), Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
                            is BitcoinHoldingDetailViewModel.UiEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindState(state: BitcoinHoldingDetailViewModel.BitcoinHoldingDetailState.Loaded) {
        val holding = state.holding
        val satsFormat = NumberFormat.getIntegerInstance(Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        val isBuy = holding.satsAmount > 0
        binding.tvType.text = if (isBuy) getString(R.string.detail_type_buy) else getString(R.string.detail_type_sell)

        // Sats
        val satsColor = if (isBuy) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        val satsText = if (isBuy) "+${satsFormat.format(holding.satsAmount)} sats" else "${satsFormat.format(holding.satsAmount)} sats"
        binding.tvSats.text = satsText
        binding.tvSats.setTextColor(satsColor)

        // Precio fiat
        binding.tvFiatPrice.text = currencyFormat.format(holding.lastFiatPrice)

        // Plataforma
        if (!holding.platform.isNullOrBlank()) {
            binding.dividerPlatform.visibility = View.VISIBLE
            binding.labelPlatform.visibility = View.VISIBLE
            binding.tvPlatform.visibility = View.VISIBLE
            binding.tvPlatform.text = holding.platform
        } else {
            binding.dividerPlatform.visibility = View.GONE
            binding.labelPlatform.visibility = View.GONE
            binding.tvPlatform.visibility = View.GONE
        }

        // Comisión
        if (holding.commission > 0) {
            binding.dividerCommission.visibility = View.VISIBLE
            binding.labelCommission.visibility = View.VISIBLE
            binding.tvCommission.visibility = View.VISIBLE
            binding.tvCommission.text = "${holding.commission}%"
        } else {
            binding.dividerCommission.visibility = View.GONE
            binding.labelCommission.visibility = View.GONE
            binding.tvCommission.visibility = View.GONE
        }

        // Fecha
        binding.tvDate.text = dateFormat.format(Date(holding.lastUpdate))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
