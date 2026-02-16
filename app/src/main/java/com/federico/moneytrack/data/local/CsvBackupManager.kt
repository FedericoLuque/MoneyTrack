package com.federico.moneytrack.data.local

import androidx.room.withTransaction
import com.federico.moneytrack.data.local.dao.*
import com.federico.moneytrack.data.local.entity.*
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvBackupManager @Inject constructor(
    private val db: AppDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val bitcoinHoldingDao: BitcoinHoldingDao
) {

    suspend fun exportToFile(outputStream: OutputStream): Result<Unit> = runCatching {
        val writer = outputStream.bufferedWriter()

        // Accounts
        val accounts = accountDao.getAllAccounts().first()
        writer.write("[accounts]\n")
        writer.write("id,name,current_balance,type\n")
        accounts.forEach { a ->
            writer.write("${a.id},${escapeCsv(a.name)},${a.currentBalance},${escapeCsv(a.type)}\n")
        }

        // Categories
        val categories = categoryDao.getAllCategories().first()
        writer.write("[categories]\n")
        writer.write("id,name,icon_name,color_hex,transaction_type\n")
        categories.forEach { c ->
            writer.write("${c.id},${escapeCsv(c.name)},${escapeCsv(c.iconName)},${escapeCsv(c.colorHex)},${escapeCsv(c.transactionType)}\n")
        }

        // Budgets
        val budgets = budgetDao.getAllBudgets().first()
        writer.write("[budgets]\n")
        writer.write("id,category_id,limit_amount,period_month,period_year\n")
        budgets.forEach { b ->
            writer.write("${b.id},${b.categoryId},${b.limitAmount},${b.periodMonth},${b.periodYear}\n")
        }

        // Transactions
        val transactions = transactionDao.getAllTransactions().first()
        writer.write("[transactions]\n")
        writer.write("id,account_id,category_id,amount,date,note\n")
        transactions.forEach { t ->
            writer.write("${t.id},${t.accountId},${t.categoryId ?: ""},${t.amount},${t.date},${escapeCsv(t.note ?: "")}\n")
        }

        // Bitcoin holdings
        val holdings = bitcoinHoldingDao.getBitcoinHoldings().first()
        writer.write("[bitcoin_holdings]\n")
        writer.write("id,sats_amount,last_fiat_price,last_update\n")
        holdings.forEach { h ->
            writer.write("${h.id},${h.satsAmount},${h.lastFiatPrice},${h.lastUpdate}\n")
        }

        writer.flush()
    }

    suspend fun importFromFile(inputStream: InputStream): Result<Int> = runCatching {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val sections = mutableMapOf<String, List<List<String>>>()
        var currentSection = ""
        var headerSkipped = false
        var currentRows = mutableListOf<List<String>>()

        reader.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                if (currentSection.isNotEmpty()) {
                    sections[currentSection] = currentRows.toList()
                }
                currentSection = trimmed.substring(1, trimmed.length - 1)
                currentRows = mutableListOf()
                headerSkipped = false
            } else if (trimmed.isNotEmpty()) {
                if (!headerSkipped) {
                    headerSkipped = true
                } else {
                    currentRows.add(parseCsvLine(trimmed))
                }
            }
        }
        if (currentSection.isNotEmpty()) {
            sections[currentSection] = currentRows.toList()
        }

        var totalRecords = 0

        val accounts = sections["accounts"]?.map { cols ->
            require(cols.size >= 4) { "Formato inválido en accounts" }
            Account(
                id = cols[0].toLong(),
                name = cols[1],
                currentBalance = cols[2].toDouble(),
                type = cols[3]
            )
        } ?: emptyList()

        val categories = sections["categories"]?.map { cols ->
            require(cols.size >= 5) { "Formato inválido en categories" }
            Category(
                id = cols[0].toLong(),
                name = cols[1],
                iconName = cols[2],
                colorHex = cols[3],
                transactionType = cols[4]
            )
        } ?: emptyList()

        val budgets = sections["budgets"]?.map { cols ->
            require(cols.size >= 5) { "Formato inválido en budgets" }
            Budget(
                id = cols[0].toLong(),
                categoryId = cols[1].toLong(),
                limitAmount = cols[2].toDouble(),
                periodMonth = cols[3].toInt(),
                periodYear = cols[4].toInt()
            )
        } ?: emptyList()

        val transactions = sections["transactions"]?.map { cols ->
            require(cols.size >= 5) { "Formato inválido en transactions" }
            Transaction(
                id = cols[0].toLong(),
                accountId = cols[1].toLong(),
                categoryId = cols[2].takeIf { it.isNotEmpty() }?.toLong(),
                amount = cols[3].toDouble(),
                date = cols[4].toLong(),
                note = cols.getOrNull(5)?.takeIf { it.isNotEmpty() }
            )
        } ?: emptyList()

        val holdings = sections["bitcoin_holdings"]?.map { cols ->
            require(cols.size >= 4) { "Formato inválido en bitcoin_holdings" }
            BitcoinHolding(
                id = cols[0].toLong(),
                satsAmount = cols[1].toLong(),
                lastFiatPrice = cols[2].toDouble(),
                lastUpdate = cols[3].toLong()
            )
        } ?: emptyList()

        db.withTransaction {
            // Borrar en orden inverso de FK
            transactionDao.deleteAll()
            budgetDao.deleteAll()
            bitcoinHoldingDao.deleteAll()
            categoryDao.deleteAll()
            accountDao.deleteAll()

            // Insertar en orden correcto
            accountDao.insertAll(accounts)
            categoryDao.insertAll(categories)
            budgetDao.insertAll(budgets)
            transactionDao.insertAll(transactions)
            bitcoinHoldingDao.insertAll(holdings)
        }

        totalRecords = accounts.size + categories.size + budgets.size + transactions.size + holdings.size
        totalRecords
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
