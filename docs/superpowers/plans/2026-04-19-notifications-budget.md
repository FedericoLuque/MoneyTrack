# Notifications — Budget Alerts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enviar notificaciones al usuario cuando el gasto de una categoría alcanza el 80% o supera el 100% del presupuesto mensual, como job diario en background.

**Architecture:** `BudgetAlertChecker` contiene la lógica de negocio (testable sin framework). `BudgetAlertWorker` (HiltWorker + CoroutineWorker) delega en el checker y es schedulado con `PeriodicWorkRequest` cada 24hs. Este plan también instala la infraestructura compartida de notificaciones (NotificationHelper, WorkManagerModule, HiltWorkerFactory) y el dominio de Budget (modelo + repositorio), necesarios para las demás ramas de notificaciones.

**Tech Stack:** WorkManager 2.9.0, Hilt-Work 1.2.0, CoroutineWorker, `@HiltWorker`, `@AssistedInject`, NotificationCompat, Room, MockK

**IMPORTANTE:** Mergear esta rama a `main` antes de comenzar `feature/notifications-reminder` o `feature/notifications-bitcoin`, ya que esas ramas dependen de la infraestructura instalada aquí.

---

## File Map

| Acción | Ruta |
|--------|------|
| Modify | `gradle/libs.versions.toml` |
| Modify | `app/build.gradle.kts` |
| Modify | `app/src/main/AndroidManifest.xml` |
| Modify | `app/src/main/java/com/federico/moneytrack/MoneyTrackApp.kt` |
| Create | `app/src/main/java/com/federico/moneytrack/domain/model/Budget.kt` |
| Create | `app/src/main/java/com/federico/moneytrack/domain/repository/BudgetRepository.kt` |
| Create | `app/src/main/java/com/federico/moneytrack/data/repository/BudgetRepositoryImpl.kt` |
| Modify | `app/src/main/java/com/federico/moneytrack/di/RepositoryModule.kt` |
| Create | `app/src/main/java/com/federico/moneytrack/di/WorkManagerModule.kt` |
| Create | `app/src/main/java/com/federico/moneytrack/util/NotificationHelper.kt` |
| Create | `app/src/main/java/com/federico/moneytrack/worker/BudgetAlertChecker.kt` |
| Create | `app/src/main/java/com/federico/moneytrack/worker/BudgetAlertWorker.kt` |
| Modify | `app/src/main/java/com/federico/moneytrack/ui/MainActivity.kt` |
| Create | `app/src/test/java/com/federico/moneytrack/worker/BudgetAlertCheckerTest.kt` |

---

### Task 1: Crear rama + agregar dependencias

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Crear rama**
```bash
git checkout main
git pull
git checkout -b feature/notifications-budget
```

- [ ] **Step 2: Agregar versiones en libs.versions.toml**

En la sección `[versions]` agregar:
```toml
work = "2.9.0"
hiltWork = "1.2.0"
```

En la sección `[libraries]` agregar:
```toml
work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
work-testing = { group = "androidx.work", name = "work-testing", version.ref = "work" }
androidx-hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "hiltWork" }
androidx-hilt-work-compiler = { group = "androidx.hilt", name = "hilt-compiler", version.ref = "hiltWork" }
```

- [ ] **Step 3: Agregar dependencias en app/build.gradle.kts**

En el bloque `dependencies`:
```kotlin
// WorkManager + Hilt-Work
implementation(libs.work.runtime.ktx)
implementation(libs.androidx.hilt.work)
ksp(libs.androidx.hilt.work.compiler)

// WorkManager testing
testImplementation(libs.work.testing)
```

- [ ] **Step 4: Agregar proveedor de SharedPreferences en AppModule**

> **Nota:** Si la rama `feature/onboarding` ya fue mergeada, este provider ya existe — omitir este paso.

En `AppModule.kt`, agregar al final del objeto (antes del `}`):
```kotlin
@Provides
@Singleton
fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
    context.getSharedPreferences("moneytrack_prefs", Context.MODE_PRIVATE)
```

Agregar import:
```kotlin
import android.content.SharedPreferences
```

- [ ] **Step 5: Sincronizar y compilar**
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**
```bash
git add gradle/libs.versions.toml app/build.gradle.kts \
        app/src/main/java/com/federico/moneytrack/di/AppModule.kt
git commit -m "feat: add WorkManager, Hilt-Work deps and SharedPreferences provider"
```

---

### Task 2: Dominio de Budget (modelo + repositorio)

**Files:**
- Create: `app/src/main/java/com/federico/moneytrack/domain/model/Budget.kt`
- Create: `app/src/main/java/com/federico/moneytrack/domain/repository/BudgetRepository.kt`
- Create: `app/src/main/java/com/federico/moneytrack/data/repository/BudgetRepositoryImpl.kt`
- Modify: `app/src/main/java/com/federico/moneytrack/di/RepositoryModule.kt`

- [ ] **Step 1: Crear domain model Budget**

`app/src/main/java/com/federico/moneytrack/domain/model/Budget.kt`:
```kotlin
package com.federico.moneytrack.domain.model

data class Budget(
    val id: Long = 0,
    val categoryId: Long,
    val limitAmount: Double,
    val periodMonth: Int,
    val periodYear: Int
)
```

- [ ] **Step 2: Crear BudgetRepository interface**

`app/src/main/java/com/federico/moneytrack/domain/repository/BudgetRepository.kt`:
```kotlin
package com.federico.moneytrack.domain.repository

import com.federico.moneytrack.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getAllBudgets(): Flow<List<Budget>>
    suspend fun insertBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)
}
```

- [ ] **Step 3: Crear BudgetRepositoryImpl**

`app/src/main/java/com/federico/moneytrack/data/repository/BudgetRepositoryImpl.kt`:
```kotlin
package com.federico.moneytrack.data.repository

import com.federico.moneytrack.data.local.dao.BudgetDao
import com.federico.moneytrack.data.local.entity.Budget as BudgetEntity
import com.federico.moneytrack.domain.model.Budget
import com.federico.moneytrack.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getAllBudgets(): Flow<List<Budget>> =
        budgetDao.getAllBudgets().map { list -> list.map { it.toDomain() } }

    override suspend fun insertBudget(budget: Budget) =
        budgetDao.insertBudget(budget.toEntity())

    override suspend fun deleteBudget(budget: Budget) =
        budgetDao.deleteBudget(budget.toEntity())

    private fun BudgetEntity.toDomain() = Budget(
        id = id,
        categoryId = categoryId,
        limitAmount = limitAmount,
        periodMonth = periodMonth,
        periodYear = periodYear
    )

    private fun Budget.toEntity() = BudgetEntity(
        id = id,
        categoryId = categoryId,
        limitAmount = limitAmount,
        periodMonth = periodMonth,
        periodYear = periodYear
    )
}
```

- [ ] **Step 4: Agregar binding en RepositoryModule**

En `RepositoryModule.kt`, agregar los imports:
```kotlin
import com.federico.moneytrack.data.repository.BudgetRepositoryImpl
import com.federico.moneytrack.domain.repository.BudgetRepository
```

Y agregar el método de binding:
```kotlin
@Binds
@Singleton
abstract fun bindBudgetRepository(
    budgetRepositoryImpl: BudgetRepositoryImpl
): BudgetRepository
```

- [ ] **Step 5: Compilar para verificar**
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/federico/moneytrack/domain/model/Budget.kt \
        app/src/main/java/com/federico/moneytrack/domain/repository/BudgetRepository.kt \
        app/src/main/java/com/federico/moneytrack/data/repository/BudgetRepositoryImpl.kt \
        app/src/main/java/com/federico/moneytrack/di/RepositoryModule.kt
git commit -m "feat: add Budget domain model, repository interface and implementation"
```

---

### Task 3: Configurar HiltWorkerFactory y permisos en el manifest

**Files:**
- Modify: `app/src/main/java/com/federico/moneytrack/MoneyTrackApp.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Actualizar MoneyTrackApp para implementar Configuration.Provider**

Reemplazar el contenido de `MoneyTrackApp.kt` con:
```kotlin
package com.federico.moneytrack

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.federico.moneytrack.util.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MoneyTrackApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        ThemeManager.applyTheme(ThemeManager.getThemePreference(this))
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

- [ ] **Step 2: Agregar permiso POST_NOTIFICATIONS en AndroidManifest.xml**

Antes de la etiqueta `<application>`, agregar:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```

- [ ] **Step 3: Deshabilitar el inicializador por defecto de WorkManager**

Dentro de `<application>` en `AndroidManifest.xml`, agregar:
```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="androidx.work.WorkManagerInitializer"
        android:value="androidx.startup"
        tools:node="remove" />
</provider>
```

Verificar que el tag `<manifest>` incluya el namespace de tools:
```xml
xmlns:tools="http://schemas.android.com/tools"
```

- [ ] **Step 4: Compilar para verificar**
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/federico/moneytrack/MoneyTrackApp.kt \
        app/src/main/AndroidManifest.xml
git commit -m "feat: configure HiltWorkerFactory and disable default WorkManager init"
```

---

### Task 4: NotificationHelper

**Files:**
- Create: `app/src/main/java/com/federico/moneytrack/util/NotificationHelper.kt`

- [ ] **Step 1: Crear NotificationHelper**

`app/src/main/java/com/federico/moneytrack/util/NotificationHelper.kt`:
```kotlin
package com.federico.moneytrack.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.federico.moneytrack.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas MoneyTrack",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alertas de presupuesto, recordatorios y Bitcoin"
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showBudgetWarningNotification(categoryName: String, percentage: Int, notificationId: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Presupuesto al $percentage%")
            .setContentText("Estás al $percentage% de tu presupuesto de $categoryName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    fun showBudgetExceededNotification(categoryName: String, percentage: Int, notificationId: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Presupuesto superado")
            .setContentText("Superaste tu presupuesto de $categoryName ($percentage%)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    fun showDailyReminderNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("MoneyTrack")
            .setContentText("¿Ya registraste tus gastos de hoy?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_REMINDER, notification)
    }

    fun showBitcoinAlertNotification(currentPrice: Double, changePercent: Double) {
        val direction = if (changePercent > 0) "↑" else "↓"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alerta Bitcoin $direction")
            .setContentText("Bitcoin: €${"%.0f".format(currentPrice)} ($direction${"%.1f".format(Math.abs(changePercent))}%)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_BITCOIN, notification)
    }

    companion object {
        const val CHANNEL_ID = "moneytrack_alerts"
        const val NOTIFICATION_ID_REMINDER = 2001
        const val NOTIFICATION_ID_BITCOIN = 3001
        const val BUDGET_NOTIFICATION_ID_BASE = 1000
    }
}
```

- [ ] **Step 2: Compilar para verificar**
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/federico/moneytrack/util/NotificationHelper.kt
git commit -m "feat: add NotificationHelper with all notification types"
```

---

### Task 5: WorkManagerModule (Hilt)

**Files:**
- Create: `app/src/main/java/com/federico/moneytrack/di/WorkManagerModule.kt`

- [ ] **Step 1: Crear WorkManagerModule**

`app/src/main/java/com/federico/moneytrack/di/WorkManagerModule.kt`:
```kotlin
package com.federico.moneytrack.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
```

- [ ] **Step 2: Compilar para verificar**
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/federico/moneytrack/di/WorkManagerModule.kt
git commit -m "feat: add WorkManagerModule Hilt provider"
```

---

### Task 6: Solicitar permiso POST_NOTIFICATIONS en MainActivity

**Files:**
- Modify: `app/src/main/java/com/federico/moneytrack/ui/MainActivity.kt`

- [ ] **Step 1: Agregar request de permiso en MainActivity**

Agregar como propiedad de la clase (antes de `onCreate`):
```kotlin
private val requestNotificationPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { /* no-op — las notificaciones son opcionales */ }
```

Agregar al final de `onCreate()`:
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
```

Agregar imports:
```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
```

- [ ] **Step 2: Compilar para verificar**
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/federico/moneytrack/ui/MainActivity.kt
git commit -m "feat: request POST_NOTIFICATIONS permission on Android 13+"
```

---

### Task 7: BudgetAlertChecker (TDD)

**Files:**
- Create: `app/src/test/java/com/federico/moneytrack/worker/BudgetAlertCheckerTest.kt`
- Create: `app/src/main/java/com/federico/moneytrack/worker/BudgetAlertChecker.kt`

- [ ] **Step 1: Escribir test que falla**

`app/src/test/java/com/federico/moneytrack/worker/BudgetAlertCheckerTest.kt`:
```kotlin
package com.federico.moneytrack.worker

import com.federico.moneytrack.domain.model.Budget
import com.federico.moneytrack.domain.model.Category
import com.federico.moneytrack.domain.model.Transaction
import com.federico.moneytrack.domain.model.TransactionWithCategory
import com.federico.moneytrack.domain.repository.BudgetRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import com.federico.moneytrack.util.NotificationHelper
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class BudgetAlertCheckerTest {

    private lateinit var budgetRepository: BudgetRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var checker: BudgetAlertChecker

    @Before
    fun setup() {
        budgetRepository = mockk()
        transactionRepository = mockk()
        notificationHelper = mockk(relaxed = true)
        checker = BudgetAlertChecker(budgetRepository, transactionRepository, notificationHelper)
    }

    @Test
    fun `no notifica cuando no hay presupuestos`() = runTest {
        coEvery { budgetRepository.getAllBudgets() } returns flowOf(emptyList())
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns flowOf(emptyList())

        checker.checkAndAlert()

        verify(exactly = 0) { notificationHelper.showBudgetWarningNotification(any(), any(), any()) }
        verify(exactly = 0) { notificationHelper.showBudgetExceededNotification(any(), any(), any()) }
    }

    @Test
    fun `notifica advertencia cuando el gasto esta entre 80 y 100 por ciento`() = runTest {
        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH) + 1
        val year = now.get(Calendar.YEAR)

        val category = Category(id = 10L, name = "Comida", iconName = "", colorHex = "", transactionType = "EXPENSE")
        val budget = Budget(id = 1L, categoryId = 10L, limitAmount = 100.0, periodMonth = month, periodYear = year)
        val transaction = TransactionWithCategory(
            transaction = Transaction(id = 1L, accountId = 1L, categoryId = 10L, amount = 85.0, date = System.currentTimeMillis(), note = null),
            category = category
        )

        coEvery { budgetRepository.getAllBudgets() } returns flowOf(listOf(budget))
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns flowOf(listOf(transaction))

        checker.checkAndAlert()

        verify { notificationHelper.showBudgetWarningNotification("Comida", 85, any()) }
    }

    @Test
    fun `notifica superacion cuando el gasto es igual o mayor al 100 por ciento`() = runTest {
        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH) + 1
        val year = now.get(Calendar.YEAR)

        val category = Category(id = 20L, name = "Ocio", iconName = "", colorHex = "", transactionType = "EXPENSE")
        val budget = Budget(id = 2L, categoryId = 20L, limitAmount = 50.0, periodMonth = month, periodYear = year)
        val transaction = TransactionWithCategory(
            transaction = Transaction(id = 2L, accountId = 1L, categoryId = 20L, amount = 60.0, date = System.currentTimeMillis(), note = null),
            category = category
        )

        coEvery { budgetRepository.getAllBudgets() } returns flowOf(listOf(budget))
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns flowOf(listOf(transaction))

        checker.checkAndAlert()

        verify { notificationHelper.showBudgetExceededNotification("Ocio", 120, any()) }
    }

    @Test
    fun `no notifica cuando el gasto esta por debajo del 80 por ciento`() = runTest {
        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH) + 1
        val year = now.get(Calendar.YEAR)

        val category = Category(id = 30L, name = "Transporte", iconName = "", colorHex = "", transactionType = "EXPENSE")
        val budget = Budget(id = 3L, categoryId = 30L, limitAmount = 100.0, periodMonth = month, periodYear = year)
        val transaction = TransactionWithCategory(
            transaction = Transaction(id = 3L, accountId = 1L, categoryId = 30L, amount = 50.0, date = System.currentTimeMillis(), note = null),
            category = category
        )

        coEvery { budgetRepository.getAllBudgets() } returns flowOf(listOf(budget))
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns flowOf(listOf(transaction))

        checker.checkAndAlert()

        verify(exactly = 0) { notificationHelper.showBudgetWarningNotification(any(), any(), any()) }
        verify(exactly = 0) { notificationHelper.showBudgetExceededNotification(any(), any(), any()) }
    }
}
```

- [ ] **Step 2: Ejecutar test para verificar que falla**
```bash
./gradlew test --tests "com.federico.moneytrack.worker.BudgetAlertCheckerTest"
```
Expected: FAIL — `BudgetAlertChecker` no existe todavía

- [ ] **Step 3: Implementar BudgetAlertChecker**

`app/src/main/java/com/federico/moneytrack/worker/BudgetAlertChecker.kt`:
```kotlin
package com.federico.moneytrack.worker

import com.federico.moneytrack.domain.repository.BudgetRepository
import com.federico.moneytrack.domain.repository.TransactionRepository
import com.federico.moneytrack.util.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class BudgetAlertChecker @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationHelper: NotificationHelper
) {
    suspend fun checkAndAlert() {
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH) + 1
        val currentYear = now.get(Calendar.YEAR)

        val startOfMonth = Calendar.getInstance().apply {
            set(currentYear, currentMonth - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfMonth = Calendar.getInstance().apply {
            set(currentYear, currentMonth - 1, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val budgets = budgetRepository.getAllBudgets().first()
            .filter { it.periodMonth == currentMonth && it.periodYear == currentYear }

        if (budgets.isEmpty()) return

        val transactions = transactionRepository.getTransactionsByDateRange(startOfMonth, endOfMonth).first()

        budgets.forEach { budget ->
            val categoryTransactions = transactions.filter {
                it.category?.id == budget.categoryId && it.category?.transactionType == "EXPENSE"
            }

            val categoryName = transactions
                .firstOrNull { it.category?.id == budget.categoryId }
                ?.category?.name ?: return@forEach

            val spending = categoryTransactions.sumOf { it.transaction.amount }

            if (budget.limitAmount <= 0.0) return@forEach

            val percentage = (spending / budget.limitAmount * 100).toInt()
            val notificationId = NotificationHelper.BUDGET_NOTIFICATION_ID_BASE + budget.id.toInt()

            when {
                percentage >= 100 -> notificationHelper.showBudgetExceededNotification(categoryName, percentage, notificationId)
                percentage >= 80 -> notificationHelper.showBudgetWarningNotification(categoryName, percentage, notificationId)
            }
        }
    }
}
```

- [ ] **Step 4: Ejecutar tests para verificar que pasan**
```bash
./gradlew test --tests "com.federico.moneytrack.worker.BudgetAlertCheckerTest"
```
Expected: BUILD SUCCESSFUL — 4 tests passed

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/federico/moneytrack/worker/BudgetAlertChecker.kt \
        app/src/test/java/com/federico/moneytrack/worker/BudgetAlertCheckerTest.kt
git commit -m "feat: add BudgetAlertChecker with budget threshold logic (TDD)"
```

---

### Task 8: BudgetAlertWorker + schedule en MainActivity

**Files:**
- Create: `app/src/main/java/com/federico/moneytrack/worker/BudgetAlertWorker.kt`
- Modify: `app/src/main/java/com/federico/moneytrack/ui/MainActivity.kt`

- [ ] **Step 1: Crear BudgetAlertWorker**

`app/src/main/java/com/federico/moneytrack/worker/BudgetAlertWorker.kt`:
```kotlin
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
```

- [ ] **Step 2: Schedulear BudgetAlertWorker en MainActivity.onCreate()**

Agregar al final de `onCreate()` en `MainActivity.kt`:
```kotlin
WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "budget_alert",
    ExistingPeriodicWorkPolicy.KEEP,
    PeriodicWorkRequestBuilder<BudgetAlertWorker>(24, TimeUnit.HOURS).build()
)
```

Agregar imports:
```kotlin
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.federico.moneytrack.worker.BudgetAlertWorker
import java.util.concurrent.TimeUnit
```

- [ ] **Step 3: Ejecutar todos los tests**
```bash
./gradlew test
```
Expected: BUILD SUCCESSFUL — todos los tests pasan

- [ ] **Step 4: Compilar para verificar**
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/federico/moneytrack/worker/BudgetAlertWorker.kt \
        app/src/main/java/com/federico/moneytrack/ui/MainActivity.kt
git commit -m "feat: add BudgetAlertWorker scheduled every 24hs"
```

---

### Task 9: Abrir PR a main

- [ ] **Step 1: Push de la rama**
```bash
git push -u origin feature/notifications-budget
```

- [ ] **Step 2: Crear PR**
```bash
gh pr create --title "feat: add budget alert notifications" --body "$(cat <<'EOF'
## Summary
- Infraestructura compartida de notificaciones: NotificationHelper, WorkManagerModule, HiltWorkerFactory
- Dominio de Budget: modelo, BudgetRepository interface e implementación
- BudgetAlertChecker: lógica de negocio testable sin framework (4 tests)
- BudgetAlertWorker: CoroutineWorker con HiltWorker, schedulado cada 24hs
- Permiso POST_NOTIFICATIONS solicitado en Android 13+

## IMPORTANTE
Mergear esta rama antes de comenzar feature/notifications-reminder o feature/notifications-bitcoin.

## Test plan
- [ ] `./gradlew test` → todos los tests pasan
- [ ] Crear un presupuesto y gastar >80% → notificación aparece al día siguiente
- [ ] Gastar >100% → notificación de "Superado" con prioridad HIGH
EOF
)"
```
