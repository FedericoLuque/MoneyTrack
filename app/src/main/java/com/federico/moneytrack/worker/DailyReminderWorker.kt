package com.federico.moneytrack.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val checker: DailyReminderChecker
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        checker.checkAndNotify()
        return Result.success()
    }
}
