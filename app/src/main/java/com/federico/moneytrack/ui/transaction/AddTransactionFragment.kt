package com.federico.moneytrack.ui.transaction

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
import com.federico.moneytrack.databinding.FragmentAddTransactionBinding
import com.federico.moneytrack.domain.model.Account
import com.federico.moneytrack.domain.model.Category
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddTransactionViewModel by viewModels()

    private var selectedAccountId: Long? = null
    private var selectedCategoryId: Long? = null
    private var selectedCategoryType: String = "EXPENSE"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupListeners()
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
                    viewModel.categories.collect { categories ->
                        setupCategorySpinner(categories)
                    }
                }
                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is AddTransactionViewModel.UiEvent.SaveSuccess -> {
                                Toast.makeText(requireContext(), "Transacción guardada", Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
                            is AddTransactionViewModel.UiEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            val amountStr = binding.etAmount.text.toString()
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            val note = binding.etNote.text.toString()

            if (selectedAccountId == null) {
                Toast.makeText(requireContext(), "Selecciona una cuenta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveTransaction(
                amount = amount,
                note = note,
                accountId = selectedAccountId!!,
                categoryId = selectedCategoryId,
                isExpense = selectedCategoryType == "EXPENSE"
            )
        }
    }

    private fun setupAccountSpinner(accounts: List<Account>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, accounts.map { it.name })
        binding.actvAccount.setAdapter(adapter)
        binding.actvAccount.setOnItemClickListener { _, _, position, _ ->
            selectedAccountId = accounts[position].id
        }
    }

    private fun setupCategorySpinner(categories: List<Category>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories.map { it.name })
        binding.actvCategory.setAdapter(adapter)
        binding.actvCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = categories[position].id
            selectedCategoryType = categories[position].transactionType
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
