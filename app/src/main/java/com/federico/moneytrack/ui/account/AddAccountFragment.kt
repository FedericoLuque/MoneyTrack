package com.federico.moneytrack.ui.account

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
import com.federico.moneytrack.databinding.FragmentAddAccountBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddAccountFragment : Fragment() {

    private var _binding: FragmentAddAccountBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AccountsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTypeSpinner()

        binding.btnSaveAccount.setOnClickListener {
            val name = binding.etAccountName.text.toString()
            val balanceStr = binding.etInitialBalance.text.toString()
            val balance = balanceStr.toDoubleOrNull() ?: 0.0
            val type = binding.actvAccountType.text.toString()

            viewModel.addAccount(name, balance, type)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventFlow.collect { event ->
                    when (event) {
                        is AccountsViewModel.UiEvent.SaveSuccess -> {
                            Toast.makeText(requireContext(), "Cuenta creada", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                        is AccountsViewModel.UiEvent.Error -> {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupTypeSpinner() {
        val types = listOf(
            getString(R.string.account_type_cash),
            getString(R.string.account_type_bank),
            getString(R.string.account_type_savings),
            getString(R.string.account_type_investment)
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        binding.actvAccountType.setAdapter(adapter)
        binding.actvAccountType.setText(types[0], false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
