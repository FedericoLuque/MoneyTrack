package com.federico.moneytrack.domain.usecase

import com.federico.moneytrack.domain.model.DailyCashFlow
import com.federico.moneytrack.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

class GetMonthlyCashFlowUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    /**
     * Obtiene el flujo de caja diario del mes en curso.
     * Agrupa las transacciones por día y separa ingresos de gastos.
     */
    operator fun invoke(): Flow<List<DailyCashFlow>> {
        val calendar = Calendar.getInstance()

        // Inicio del mes actual (día 1, 00:00:00.000)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Fin del último día del mes (23:59:59.999)
        calendar.set(Calendar.DAY_OF_MONTH, daysInMonth)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        return transactionRepository.getTransactionsByDateRange(startDate, endDate).map { transactions ->
            // Crear mapa con todos los días del mes con valores iniciales en 0
            // Triple: (income, expense, bitcoin)
            val dailyMap = linkedMapOf<Int, Triple<Double, Double, Double>>()
            for (day in 1..daysInMonth) {
                dailyMap[day] = Triple(0.0, 0.0, 0.0)
            }

            // Agrupar transacciones por día del mes
            for (twc in transactions) {
                val txCal = Calendar.getInstance()
                txCal.timeInMillis = twc.transaction.date
                val dayOfMonth = txCal.get(Calendar.DAY_OF_MONTH)

                val current = dailyMap[dayOfMonth] ?: Triple(0.0, 0.0, 0.0)
                val type = twc.category?.transactionType
                dailyMap[dayOfMonth] = when (type) {
                    "INCOME" -> Triple(current.first + twc.transaction.amount, current.second, current.third)
                    "BITCOIN" -> Triple(current.first, current.second, current.third + Math.abs(twc.transaction.amount))
                    else -> Triple(current.first, current.second + twc.transaction.amount, current.third)
                }
            }

            dailyMap.map { (day, amounts) ->
                DailyCashFlow(
                    dayLabel = day.toString(),
                    incomeAmount = amounts.first,
                    expenseAmount = amounts.second,
                    bitcoinAmount = amounts.third
                )
            }
        }
    }
}
