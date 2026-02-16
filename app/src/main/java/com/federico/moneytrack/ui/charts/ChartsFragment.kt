package com.federico.moneytrack.ui.charts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.federico.moneytrack.R
import com.federico.moneytrack.databinding.FragmentChartsBinding
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChartsFragment : Fragment() {

    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChartsViewModel by viewModels()

    private val cashFlowProducer = CartesianChartModelProducer()
    private val patrimonyProducer = CartesianChartModelProducer()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chartCashFlow.modelProducer = cashFlowProducer
        binding.chartPatrimony.modelProducer = patrimonyProducer

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ChartsUiState.Loading -> { /* Esperando datos */ }
                        is ChartsUiState.Success -> {
                            updateCashFlowChart(state)
                            updatePatrimonyChart(state)
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateCashFlowChart(state: ChartsUiState.Success) {
        if (state.weeklyCashFlow.isEmpty()) return

        val incomes = state.weeklyCashFlow.map { it.incomeAmount }
        val expenses = state.weeklyCashFlow.map { it.expenseAmount }

        cashFlowProducer.runTransaction {
            columnSeries {
                series(incomes)
                series(expenses)
            }
        }
    }

    private suspend fun updatePatrimonyChart(state: ChartsUiState.Success) {
        val fiat = state.fiatBalance.coerceAtLeast(0.0)
        val btc = state.bitcoinValue.coerceAtLeast(0.0)

        patrimonyProducer.runTransaction {
            columnSeries {
                series(fiat, btc)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
