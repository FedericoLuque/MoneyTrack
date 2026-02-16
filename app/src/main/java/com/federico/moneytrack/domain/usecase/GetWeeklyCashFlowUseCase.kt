package com.federico.moneytrack.domain.usecase

import com.federico.moneytrack.domain.model.DailyCashFlow
import com.federico.moneytrack.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class GetWeeklyCashFlowUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    /**
     * Obtiene el flujo de caja diario de los últimos 7 días.
     * Agrupa las transacciones por día y separa ingresos de gastos.
     */
    operator fun invoke(): Flow<List<DailyCashFlow>> {
        val calendar = Calendar.getInstance()
        // Fin del día de hoy
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        // Inicio de hace 6 días (7 días en total incluyendo hoy)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        return transactionRepository.getTransactionsByDateRange(startDate, endDate).map { transactions ->
            // Crear mapa de los 7 días con valores iniciales en 0
            val dailyMap = linkedMapOf<String, Pair<Double, Double>>()
            val cal = Calendar.getInstance()
            cal.timeInMillis = startDate
            for (i in 0 until 7) {
                val label = dayFormat.format(cal.time).replaceFirstChar { it.uppercase() }
                dailyMap[label] = Pair(0.0, 0.0)
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Agrupar transacciones por día
            for (twc in transactions) {
                val txCal = Calendar.getInstance()
                txCal.timeInMillis = twc.transaction.date
                val label = dayFormat.format(txCal.time).replaceFirstChar { it.uppercase() }

                val current = dailyMap[label] ?: Pair(0.0, 0.0)
                val isIncome = twc.category?.transactionType == "INCOME"
                dailyMap[label] = if (isIncome) {
                    Pair(current.first + twc.transaction.amount, current.second)
                } else {
                    Pair(current.first, current.second + twc.transaction.amount)
                }
            }

            dailyMap.map { (label, amounts) ->
                DailyCashFlow(
                    dayLabel = label,
                    incomeAmount = amounts.first,
                    expenseAmount = amounts.second
                )
            }
        }
    }
}
