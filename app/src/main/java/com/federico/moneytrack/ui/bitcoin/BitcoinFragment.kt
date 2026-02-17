package com.federico.moneytrack.ui.bitcoin

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
import com.federico.moneytrack.databinding.FragmentBitcoinBinding
import com.federico.moneytrack.domain.model.BitcoinHolding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class BitcoinFragment : Fragment() {

    private var _binding: FragmentBitcoinBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BitcoinViewModel by viewModels()
    private lateinit var holdingAdapter: BitcoinHoldingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBitcoinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        holdingAdapter = BitcoinHoldingAdapter { holding ->
            showDeleteDialog(holding)
        }

        binding.rvHoldings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = holdingAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
                        val satsFormat = NumberFormat.getIntegerInstance(Locale.getDefault())

                        binding.tvBtcBalance.text = "${satsFormat.format(state.totalSats)} Sats"
                        binding.tvFiatValue.text = "≈ ${currencyFormat.format(state.fiatValue)}"

                        holdingAdapter.submitList(state.holdings)
                        binding.tvEmptyState.visibility = if (state.holdings.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvHoldings.visibility = if (state.holdings.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is BitcoinViewModel.UiEvent.DeleteSuccess -> {
                                Toast.makeText(requireContext(), getString(R.string.bitcoin_delete_success), Toast.LENGTH_SHORT).show()
                            }
                            is BitcoinViewModel.UiEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> { }
                        }
                    }
                }
            }
        }

        binding.btnBuy.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isBuy", true) }
            findNavController().navigate(R.id.action_bitcoinFragment_to_addBitcoinTransactionFragment, bundle)
        }

        binding.btnSell.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isBuy", false) }
            findNavController().navigate(R.id.action_bitcoinFragment_to_addBitcoinTransactionFragment, bundle)
        }
    }

    private fun showDeleteDialog(holding: BitcoinHolding) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.bitcoin_delete_dialog_title))
            .setMessage(getString(R.string.bitcoin_delete_dialog_message))
            .setPositiveButton(getString(R.string.bitcoin_delete_dialog_confirm)) { _, _ ->
                viewModel.deleteBitcoinHolding(holding)
            }
            .setNegativeButton(getString(R.string.settings_delete_all_cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
