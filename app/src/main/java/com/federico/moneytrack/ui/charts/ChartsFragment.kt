package com.federico.moneytrack.ui.charts

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
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
import com.patrykandpatrick.vico.core.cartesian.CartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.common.component.TextComponent
import com.patrykandpatrick.vico.views.cartesian.ScrollHandler
import com.patrykandpatrick.vico.views.cartesian.ZoomHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChartsFragment : Fragment() {

    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChartsViewModel by viewModels()

    private val cashFlowProducer = CartesianChartModelProducer()
    private val patrimonyProducer = CartesianChartModelProducer()
    private var cashFlowDayLabels: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCashFlowChart()
        binding.chartCashFlow.modelProducer = cashFlowProducer
        setupPatrimonyChart()
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

    private fun getThemeTextColor(): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        return requireContext().getColorStateList(typedValue.resourceId).defaultColor
    }

    private fun setupCashFlowChart() {
        val incomeColor = Color.parseColor("#4CAF50")
        val expenseColor = Color.parseColor("#F44336")
        val bitcoinColor = Color.parseColor("#EF6C00")
        val axisColor = Color.GRAY
        val textColor = getThemeTextColor()

        val columnLayer = ColumnCartesianLayer(
            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                LineComponent(Fill(incomeColor), thicknessDp = 8f),
                LineComponent(Fill(expenseColor), thicknessDp = 8f),
                LineComponent(Fill(bitcoinColor), thicknessDp = 8f)
            ),
            mergeMode = { ColumnCartesianLayer.MergeMode.Grouped() }
        )

        val axisLabel = TextComponent(color = textColor, textSizeSp = 10f)
        val axisLine = LineComponent(Fill(axisColor), thicknessDp = 1f)
        val axisTick = LineComponent(Fill(axisColor), thicknessDp = 1f)

        val bottomAxis = HorizontalAxis.bottom(
            label = axisLabel,
            line = axisLine,
            tick = axisTick,
            tickLengthDp = 4f,
            valueFormatter = CartesianValueFormatter { _, value, _ ->
                cashFlowDayLabels.getOrElse(value.toInt()) { "" }
            }
        )

        val startAxis = VerticalAxis.start(
            label = axisLabel,
            line = axisLine,
            tick = axisTick,
            tickLengthDp = 4f
        )

        binding.chartCashFlow.scrollHandler = ScrollHandler(scrollEnabled = false)
        binding.chartCashFlow.zoomHandler = ZoomHandler(
            zoomEnabled = false,
            initialZoom = Zoom.Content
        )
        binding.chartCashFlow.chart = CartesianChart(
            columnLayer,
            startAxis = startAxis,
            bottomAxis = bottomAxis
        )
    }

    private suspend fun updateCashFlowChart(state: ChartsUiState.Success) {
        if (state.monthlyCashFlow.isEmpty()) return

        cashFlowDayLabels = state.monthlyCashFlow.map { it.dayLabel }
        val incomes = state.monthlyCashFlow.map { it.incomeAmount }
        val expenses = state.monthlyCashFlow.map { it.expenseAmount }
        val bitcoin = state.monthlyCashFlow.map { it.bitcoinAmount }

        cashFlowProducer.runTransaction {
            columnSeries {
                series(incomes)
                series(expenses)
                series(bitcoin)
            }
        }
    }

    private fun setupPatrimonyChart() {
        val fiatColor = Color.parseColor("#2196F3")
        val btcColor = Color.parseColor("#EF6C00")
        val axisColor = Color.GRAY
        val textColor = getThemeTextColor()

        val columnLayer = ColumnCartesianLayer(
            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                LineComponent(Fill(fiatColor), thicknessDp = 16f),
                LineComponent(Fill(btcColor), thicknessDp = 16f)
            ),
            mergeMode = { ColumnCartesianLayer.MergeMode.Stacked }
        )

        val axisLabel = TextComponent(color = textColor, textSizeSp = 12f)
        val axisLine = LineComponent(Fill(axisColor), thicknessDp = 1f)
        val axisTick = LineComponent(Fill(axisColor), thicknessDp = 1f)

        val labels = listOf("Fiat", "Bitcoin")
        val bottomAxis = HorizontalAxis.bottom(
            label = axisLabel,
            line = axisLine,
            tick = axisTick,
            tickLengthDp = 4f,
            valueFormatter = CartesianValueFormatter { _, value, _ ->
                labels.getOrElse(value.toInt()) { "" }
            }
        )

        val startAxis = VerticalAxis.start(
            label = axisLabel,
            line = axisLine,
            tick = axisTick,
            tickLengthDp = 4f
        )

        binding.chartPatrimony.chart = CartesianChart(
            columnLayer,
            startAxis = startAxis,
            bottomAxis = bottomAxis
        )
    }

    private suspend fun updatePatrimonyChart(state: ChartsUiState.Success) {
        val fiat = state.fiatBalance.coerceAtLeast(0.0)
        val btc = state.bitcoinValue.coerceAtLeast(0.0)

        patrimonyProducer.runTransaction {
            columnSeries {
                series(fiat, 0.0)
                series(0.0, btc)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
