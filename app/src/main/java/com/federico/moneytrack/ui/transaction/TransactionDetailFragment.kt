package com.federico.moneytrack.ui.transaction

import android.content.res.ColorStateList
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
import com.federico.moneytrack.databinding.FragmentTransactionDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class TransactionDetailFragment : Fragment() {

    private var _binding: FragmentTransactionDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionDetailBinding.inflate(inflater, container, false)
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
                    viewModel.deleteTransaction()
                }
                .setNegativeButton(getString(R.string.settings_delete_all_cancel), null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        when (state) {
                            is TransactionDetailViewModel.TransactionDetailState.Loading -> { }
                            is TransactionDetailViewModel.TransactionDetailState.Loaded -> {
                                bindState(state)
                            }
                        }
                    }
                }
                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is TransactionDetailViewModel.UiEvent.Deleted -> {
                                Toast.makeText(requireContext(), getString(R.string.detail_deleted), Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
                            is TransactionDetailViewModel.UiEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindState(state: TransactionDetailViewModel.TransactionDetailState.Loaded) {
        val transaction = state.transaction
        val category = state.category
        val account = state.account

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        // Categoría
        if (category != null) {
            binding.tvCategoryIcon.text = category.name.firstOrNull()?.toString()?.uppercase() ?: "?"
            try {
                binding.tvCategoryIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor(category.colorHex))
            } catch (e: Exception) {
                binding.tvCategoryIcon.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
            }
            binding.tvCategoryName.text = category.name
        } else {
            binding.tvCategoryIcon.text = "₿"
            binding.tvCategoryIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F7931A"))
            binding.tvCategoryName.text = "Bitcoin"
        }

        // Monto
        val isIncome = if (category != null) category.transactionType == "INCOME" else transaction.amount >= 0
        val color = if (isIncome) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        binding.tvAmount.setTextColor(color)
        binding.tvAmount.text = currencyFormat.format(transaction.amount)

        // Cuenta
        binding.tvAccount.text = account?.name ?: "—"

        // Fecha
        binding.tvDate.text = dateFormat.format(Date(transaction.date))

        // Nota
        binding.tvNote.text = transaction.note ?: getString(R.string.detail_no_note)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
