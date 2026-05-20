package com.federico.moneytrack.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BudgetAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val checker: BudgetAlertChecker
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        checker.checkAndAlert()
        return Result.success()
    }
}
