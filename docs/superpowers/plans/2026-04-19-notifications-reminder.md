# Notifications — Daily Reminder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enviar un recordatorio diario al usuario a una hora configurable si no registró ninguna transacción en el día.

**Architecture:** `DailyReminderChecker` contiene la lógica (testable). `DailyReminderWorker` delega en el checker. La hora se configura en la pantalla de Settings y se guarda en SharedPreferences. Al cambiar la hora, `SettingsViewModel` cancela el WorkRequest anterior y schedula uno nuevo con `ExistingPeriodicWorkPolicy.REPLACE` y el `initialDelay` correcto para llegar a la hora elegida.

**Tech Stack:** WorkManager, Hilt-Work, CoroutineWorker, SharedPreferences, TimePickerDialog, MockK

**PREREQUISITO:** La rama `feature/notifications-budget` debe estar mergeada en `main` antes de comenzar esta rama. Esta rama depende de: `NotificationHelper`, `WorkManagerModule`, `HiltWorkerFactory` config en `MoneyTrackApp`, y el permiso `POST_NOTIFICATIONS` en el manifest — todos instalados por esa rama.

---

## File Map

| Acción | Ruta |
|--------|------|
| Create | `app/src/main/java/com/federico/moneytrack/worker/DailyReminderChecker.kt` |
| Create | `app/src/main/java/com/federico/moneytrack/worker/DailyReminderWorker.kt` |
| Create | `app/src/main/java/com/federico/moneytrack/worker/ReminderScheduler.kt` |
| Modify | `app/src/main/java/com/federico/moneytrack/ui/settings/SettingsViewModel.kt` |
| Modify | `app/src/main/java/com/federico/moneytrack/ui/settings/SettingsFragment.kt` |
| Modify | `app/src/main/res/layout/fragment_settings.xml` |
| Modify | `app/src/main/res/values/strings.xml` |
| Create | `app/src/test/java/com/federico/moneytrack/worker/DailyReminderCheckerTest.kt` |

---

### Task 1: Crear rama

- [ ] **Step 1: Crear rama desde main actualizado**
```bash
git checkout main
git pull
git checkout -b feature/notifications-reminder
```

---

### Task 2: DailyReminderChecker (TDD)

**Files:**
- Create: `app/src/test/java/com/federico/moneytrack/worker/DailyReminderCheckerTest.kt`
- Create: `app/src/main/java/com/federico/moneytrack/worker/DailyReminderChecker.kt`

- [ ] **Step 1: Escribir test que falla**

`app/src/test/java/com/federico/moneytrack/worker/DailyReminderCheckerTest.kt`:
```kotlin
package com.federico.moneytrack.worker

import com.federico.moneytrack.domain.model.Category
import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.model.TransactionWithCategory
import com.federico.moneytrack.domain.repository.TransactionRepository
import com.federico.moneytrack.util.NotificationHelper
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DailyReminderCheckerTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var checker: DailyReminderChecker

    @Before
    fun setup() {
        transactionRepository = mockk()
        notificationHelper = mockk(relaxed = true)
        checker = DailyReminderChecker(transactionRepository, notificationHelper)
    }

    @Test
    fun `envia notificacion cuando no hay transacciones en el dia`() = runTest {
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns flowOf(emptyList())

        checker.checkAndNotify()

        verify { notificationHelper.showDailyReminderNotification() }
    }

    @Test
    fun `no envia notificacion cuando ya hay transacciones en el dia`() = runTest {
        val category = Category(id = 1L, name = "Comida", iconName = "", colorHex = "", transactionType = "EXPENSE")
        val transaction = TransactionWithCategory(
            transaction = Transaction(id = 1L, accountId = 1L, categoryId = 1L, amount = 20.0, date = System.currentTimeMillis(), note = null),
            category = category
        )
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns flowOf(listOf(transaction))

        checker.checkAndNotify()

        verify(exactly = 0) { notificationHelper.showDailyReminderNotification() }
    }
}
```

- [ ] **Step 2: Ejecutar test para verificar que falla**
```bash
./gradlew test --tests "com.federico.moneytrack.worker.DailyReminderCheckerTest"
```
Expected: FAIL — `DailyReminderChecker` no existe todavía

- [ ] **Step 3: Implementar DailyReminderChecker**

`app/src/main/java/com/federico/moneytrack/worker/DailyReminderChecker.kt`:
```kotlin
package com.federico.moneytrack.worker

import com.federico.moneytrack.domain.repository.TransactionRepository
import com.federico.moneytrack.util.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class DailyReminderChecker @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val notificationHelper: NotificationHelper
) {
    suspend fun checkAndNotify() {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val transactions = transactionRepository.getTransactionsByDateRange(startOfDay, endOfDay).first()

        if (transactions.isEmpty()) {
            notificationHelper.showDailyReminderNotification()
        }
    }
}
```

- [ ] **Step 4: Ejecutar tests para verificar que pasan**
```bash
./gradlew test --tests "com.federico.moneytrack.worker.DailyReminderCheckerTest"
```
Expected: BUILD SUCCESSFUL — 2 tests passed

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/federico/moneytrack/worker/DailyReminderChecker.kt \
        app/src/test/java/com/federico/moneytrack/worker/DailyReminderCheckerTest.kt
git commit -m "feat: add DailyReminderChecker (TDD)"
```

---

### Task 3: DailyReminderWorker + ReminderScheduler

**Files:**
- Create: `app/src/main/java/com/federico/moneytrack/worker/DailyReminderWorker.kt`
- Create: `app/src/main/java/com/federico/moneytrack/worker/ReminderScheduler.kt`

- [ ] **Step 1: Crear DailyReminderWorker**

`app/src/main/java/com/federico/moneytrack/worker/DailyReminderWorker.kt`:
```kotlin
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
```

- [ ] **Step 2: Crear ReminderScheduler**

`app/src/main/java/com/federico/moneytrack/worker/ReminderScheduler.kt`:
```kotlin
package com.federico.moneytrack.worker

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReminderScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun schedule(hour: Int, minute: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelay = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "daily_reminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork("daily_reminder")
    }
}
```

- [ ] **Step 3: Compilar para verificar**
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/federico/moneytrack/worker/DailyReminderWorker.kt \
        app/src/main/java/com/federico/moneytrack/worker/ReminderScheduler.kt
git commit -m "feat: add DailyReminderWorker and ReminderScheduler"
```

---

### Task 4: Agregar hora de recordatorio en Settings

**Files:**
- Modify: `app/src/main/res/layout/fragment_settings.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/federico/moneytrack/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/federico/moneytrack/ui/settings/SettingsFragment.kt`

- [ ] **Step 1: Agregar strings**

En `app/src/main/res/values/strings.xml`, agregar antes de `</resources>`:
```xml
<!-- Recordatorio diario -->
<string name="settings_reminder_title">Recordatorio diario</string>
<string name="settings_reminder_description">Hora del recordatorio para registrar gastos</string>
<string name="settings_reminder_time_format">%1$02d:%2$02d</string>
```

- [ ] **Step 2: Agregar UI en fragment_settings.xml**

Localizar el archivo `app/src/main/res/layout/fragment_settings.xml` y agregar el siguiente bloque antes del cierre del layout principal (antes del último `</...Layout>`):

```xml
<com.google.android.material.divider.MaterialDivider
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:layout_marginBottom="8dp"/>

<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="@string/settings_reminder_title"
    android:textAppearance="?attr/textAppearanceTitleMedium"
    android:textColor="?attr/colorOnSurface"
    android:layout_marginBottom="4dp"/>

<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="@string/settings_reminder_description"
    android:textAppearance="?attr/textAppearanceBodyMedium"
    android:textColor="?attr/colorOnSurfaceVariant"/>

<com.google.android.material.button.MaterialButton
    android:id="@+id/btnReminderTime"
    style="@style/Widget.Material3.Button.OutlinedButton"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:text="21:00"/>
```

- [ ] **Step 3: Agregar lógica en SettingsViewModel**

En `SettingsViewModel.kt`, agregar inyección de `SharedPreferences` y `ReminderScheduler` al constructor:
```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val csvBackupManager: CsvBackupManager,
    private val prefs: SharedPreferences,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
```

Agregar los siguientes métodos al ViewModel:
```kotlin
fun getReminderTime(): Pair<Int, Int> {
    val hour = prefs.getInt(KEY_REMINDER_HOUR, 21)
    val minute = prefs.getInt(KEY_REMINDER_MINUTE, 0)
    return Pair(hour, minute)
}

fun saveReminderTime(hour: Int, minute: Int) {
    prefs.edit()
        .putInt(KEY_REMINDER_HOUR, hour)
        .putInt(KEY_REMINDER_MINUTE, minute)
        .apply()
    reminderScheduler.schedule(hour, minute)
}

companion object {
    private const val KEY_REMINDER_HOUR = "reminder_hour"
    private const val KEY_REMINDER_MINUTE = "reminder_minute"
}
```

- [ ] **Step 4: Agregar interacción en SettingsFragment**

En `SettingsFragment.kt`, agregar en `onViewCreated()`:
```kotlin
// Mostrar hora actual en el botón
val (hour, minute) = viewModel.getReminderTime()
binding.btnReminderTime.text = "%02d:%02d".format(hour, minute)

// Abrir TimePicker al hacer tap
binding.btnReminderTime.setOnClickListener {
    val (currentHour, currentMinute) = viewModel.getReminderTime()
    android.app.TimePickerDialog(
        requireContext(),
        { _, selectedHour, selectedMinute ->
            viewModel.saveReminderTime(selectedHour, selectedMinute)
            binding.btnReminderTime.text = "%02d:%02d".format(selectedHour, selectedMinute)
        },
        currentHour,
        currentMinute,
        true
    ).show()
}
```

- [ ] **Step 5: Compilar para verificar**
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Ejecutar todos los tests**
```bash
./gradlew test
```
Expected: BUILD SUCCESSFUL — todos los tests pasan

- [ ] **Step 7: Commit**
```bash
git add app/src/main/res/layout/fragment_settings.xml \
        app/src/main/res/values/strings.xml \
        app/src/main/java/com/federico/moneytrack/ui/settings/SettingsViewModel.kt \
        app/src/main/java/com/federico/moneytrack/ui/settings/SettingsFragment.kt
git commit -m "feat: add reminder time picker in Settings and wire DailyReminderWorker"
```

---

### Task 5: Abrir PR a main

- [ ] **Step 1: Push de la rama**
```bash
git push -u origin feature/notifications-reminder
```

- [ ] **Step 2: Crear PR**
```bash
gh pr create --title "feat: add daily reminder notification" --body "$(cat <<'EOF'
## Summary
- DailyReminderChecker: notifica si no hay transacciones en el día (2 tests)
- DailyReminderWorker: CoroutineWorker schedulado con initialDelay a la hora configurada
- ReminderScheduler: encapsula la lógica de schedule/cancel con REPLACE policy
- Settings: selector de hora con TimePickerDialog

## Prerequisito
Requiere que feature/notifications-budget esté mergeado en main.

## Test plan
- [ ] `./gradlew test` → todos los tests pasan
- [ ] Settings → cambiar hora de recordatorio → se muestra la nueva hora
- [ ] Sin registrar transacciones, esperar la hora configurada → llega notificación
- [ ] Registrar una transacción → notificación NO aparece esa noche
EOF
)"
```
