package com.federico.moneytrack.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.federico.moneytrack.R
import com.federico.moneytrack.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_addTransactionFragment)
        }

        binding.btnCategories.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_categoriesFragment)
        }

        binding.btnConfiguracion.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_settingsFragment)
        }

        binding.btnViewAllTransactions.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_transactionsFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is DashboardUiState.Loading -> {
                                binding.tvTotalBalance.text = getString(R.string.dashboard_loading)
                            }
                            is DashboardUiState.Success -> {
                                val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
                                binding.tvTotalBalance.text = format.format(state.fiatBalance)
                                binding.tvBitcoinValue.text = format.format(state.bitcoinValue)
                                transactionAdapter.submitList(state.recentTransactions)
                            }
                        }
                    }
                }
                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is DashboardViewModel.UiEvent.NavigateToTransactionDetail -> {
                                findNavController().navigate(
                                    R.id.action_global_transactionDetailFragment,
                                    bundleOf("transactionId" to event.transactionId)
                                )
                            }
                            is DashboardViewModel.UiEvent.NavigateToBitcoinDetail -> {
                                findNavController().navigate(
                                    R.id.action_global_bitcoinHoldingDetailFragment,
                                    bundleOf("holdingId" to event.holdingId)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter { item ->
            viewModel.onTransactionClicked(item)
        }
        binding.rvTransactions.apply {
            adapter = transactionAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
