package com.federico.moneytrack.ui.bitcoin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.federico.moneytrack.R
import com.federico.moneytrack.databinding.FragmentAddBitcoinTransactionBinding
import com.federico.moneytrack.domain.model.Account
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddBitcoinTransactionFragment : Fragment() {

    private var _binding: FragmentAddBitcoinTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BitcoinViewModel by viewModels()

    private var isBuy: Boolean = true
    private var selectedAccountId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBitcoinTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isBuy = arguments?.getBoolean("isBuy", true) ?: true
        binding.toolbar.title = if (isBuy) getString(R.string.action_buy_btc) else getString(R.string.action_sell_btc)

        setupObservers()
        
        binding.btnSave.setOnClickListener {
            saveTransaction()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.accounts.collect { accounts ->
                        setupAccountSpinner(accounts)
                    }
                }
                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is BitcoinViewModel.UiEvent.TransactionSuccess -> {
                                Toast.makeText(requireContext(), getString(R.string.msg_btc_transaction_saved), Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
                            is BitcoinViewModel.UiEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupAccountSpinner(accounts: List<Account>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, accounts.map { it.name })
        binding.actvAccount.setAdapter(adapter)
        binding.actvAccount.setOnItemClickListener { _, _, position, _ ->
            selectedAccountId = accounts[position].id
        }
    }

    private fun saveTransaction() {
        val satsStr = binding.etSats.text.toString()
        val fiatStr = binding.etFiat.text.toString()
        val note = binding.etNote.text.toString()
        
        val sats = satsStr.toLongOrNull() ?: 0L
        val fiat = fiatStr.toDoubleOrNull() ?: 0.0

        if (sats <= 0 || fiat <= 0) {
            Toast.makeText(requireContext(), "Cantidades inválidas", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedAccountId == null) {
            Toast.makeText(requireContext(), "Selecciona una cuenta", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.executeTransaction(
            sats = sats,
            fiatAmount = fiat,
            accountId = selectedAccountId!!,
            isBuy = isBuy,
            note = note
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
