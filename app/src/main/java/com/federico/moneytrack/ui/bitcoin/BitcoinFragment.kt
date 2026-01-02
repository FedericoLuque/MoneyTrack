package com.federico.moneytrack.ui.bitcoin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.federico.moneytrack.R
import com.federico.moneytrack.databinding.FragmentBitcoinBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class BitcoinFragment : Fragment() {

    private var _binding: FragmentBitcoinBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BitcoinViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBitcoinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
                    val satsFormat = NumberFormat.getIntegerInstance(Locale.getDefault())
                    
                    binding.tvBtcBalance.text = "${satsFormat.format(state.totalSats)} Sats"
                    binding.tvFiatValue.text = "≈ ${currencyFormat.format(state.fiatValue)}"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
