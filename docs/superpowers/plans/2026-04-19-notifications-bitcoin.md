# Notifications — Bitcoin Price Alert Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enviar una notificación cuando el precio de Bitcoin varía más de un umbral configurable (%) respecto al precio de referencia almacenado, chequeando cada hora.

**Architecture:** `BitcoinPriceAlertChecker` contiene la lógica (testable). Lee el precio actual via `BitcoinRepository.getBitcoinPrice()`, compara con un precio base guardado en SharedPreferences (`bitcoin_alert_baseline`). Si la variación absoluta supera el umbral, envía la notificación y actualiza el baseline. `BitcoinPriceAlertWorker` delega en el checker y se schedula con `PeriodicWorkRequest` cada 1hs. El umbral se configura en Settings.

**Tech Stack:** WorkManager, Hilt-Work, CoroutineWorker, SharedPreferences, MockK

**PREREQUISITO:** La rama `feature/notifications-budget` debe estar mergeada en `main` antes de comenzar esta rama. Esta rama depende de: `NotificationHelper`, `WorkManagerModule`, `HiltWorkerFactory` config, y permiso `POST_NOTIFICATIONS` — todos instalados por esa rama.

---

## File Map

| Acción | Ruta |
|--------|------|
| Create | `app/src/main/java/com/federico/moneytrack/worker/BitcoinPriceAlertChecker.kt` |
| Create | `app/src/main/java/com/federico/moneytrack/worker/BitcoinPriceAlertWorker.kt` |
| Modify | `app/src/main/java/com/federico/moneytrack/ui/settings/SettingsViewModel.kt` |
| Modify | `app/src/main/java/com/federico/moneytrack/ui/settings/SettingsFragment.kt` |
| Modify | `app/src/main/res/layout/fragment_settings.xml` |
| Modify | `app/src/main/res/values/strings.xml` |
| Modify | `app/src/main/java/com/federico/moneytrack/ui/MainActivity.kt` |
| Create | `app/src/test/java/com/federico/moneytrack/worker/BitcoinPriceAlertCheckerTest.kt` |

---

### Task 1: Crear rama

- [ ] **Step 1: Crear rama desde main actualizado**
```bash
git checkout main
git pull
git checkout -b feature/notifications-bitcoin
```

---

### Task 2: BitcoinPriceAlertChecker (TDD)

**Files:**
- Create: `app/src/test/java/com/federico/moneytrack/worker/BitcoinPriceAlertCheckerTest.kt`
- Create: `app/src/main/java/com/federico/moneytrack/worker/BitcoinPriceAlertChecker.kt`

- [ ] **Step 1: Escribir test que falla**

`app/src/test/java/com/federico/moneytrack/worker/BitcoinPriceAlertCheckerTest.kt`:
```kotlin
package com.federico.moneytrack.worker

import android.content.SharedPreferences
import com.federico.moneytrack.domain.repository.BitcoinRepository
import com.federico.moneytrack.util.NotificationHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class BitcoinPriceAlertCheckerTest {

    private lateinit var bitcoinRepository: BitcoinRepository
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var checker: BitcoinPriceAlertChecker

    @Before
    fun setup() {
        bitcoinRepository = mockk()
        prefs = mockk()
        editor = mockk()
        notificationHelper = mockk(relaxed = true)
        every { prefs.edit() } returns editor
        every { editor.putFloat(any(), any()) } returns editor
        every { editor.apply() } just Runs
        checker = BitcoinPriceAlertChecker(bitcoinRepository, prefs, notificationHelper)
    }

    @Test
    fun `guarda baseline en primer uso sin notificar`() = runTest {
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 0f) } returns 0f
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_ALERT_THRESHOLD, 5f) } returns 5f
        coEvery { bitcoinRepository.getBitcoinPrice("eur") } returns 50000.0

        checker.checkAndAlert()

        verify { editor.putFloat(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 50000f) }
        verify(exactly = 0) { notificationHelper.showBitcoinAlertNotification(any(), any()) }
    }

    @Test
    fun `notifica cuando la variacion supera el umbral positivo`() = runTest {
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 0f) } returns 50000f
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_ALERT_THRESHOLD, 5f) } returns 5f
        coEvery { bitcoinRepository.getBitcoinPrice("eur") } returns 53000.0 // +6%

        checker.checkAndAlert()

        verify { notificationHelper.showBitcoinAlertNotification(53000.0, any()) }
        verify { editor.putFloat(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 53000f) }
    }

    @Test
    fun `notifica cuando la variacion supera el umbral negativo`() = runTest {
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 0f) } returns 50000f
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_ALERT_THRESHOLD, 5f) } returns 5f
        coEvery { bitcoinRepository.getBitcoinPrice("eur") } returns 46000.0 // -8%

        checker.checkAndAlert()

        verify { notificationHelper.showBitcoinAlertNotification(46000.0, any()) }
        verify { editor.putFloat(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 46000f) }
    }

    @Test
    fun `no notifica cuando la variacion esta por debajo del umbral`() = runTest {
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 0f) } returns 50000f
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_ALERT_THRESHOLD, 5f) } returns 5f
        coEvery { bitcoinRepository.getBitcoinPrice("eur") } returns 51000.0 // +2%

        checker.checkAndAlert()

        verify(exactly = 0) { notificationHelper.showBitcoinAlertNotification(any(), any()) }
    }

    @Test
    fun `no notifica si la API devuelve precio cero`() = runTest {
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_BASELINE_PRICE, 0f) } returns 50000f
        every { prefs.getFloat(BitcoinPriceAlertChecker.KEY_ALERT_THRESHOLD, 5f) } returns 5f
        coEvery { bitcoinRepository.getBitcoinPrice("eur") } returns 0.0

        checker.checkAndAlert()

        verify(exactly = 0) { notificationHelper.showBitcoinAlertNotification(any(), any()) }
    }
}
```

- [ ] **Step 2: Ejecutar test para verificar que falla**
```bash
./gradlew test --tests "com.federico.moneytrack.worker.BitcoinPriceAlertCheckerTest"
```
Expected: FAIL — `BitcoinPriceAlertChecker` no existe todavía

- [ ] **Step 3: Implementar BitcoinPriceAlertChecker**

`app/src/main/java/com/federico/moneytrack/worker/BitcoinPriceAlertChecker.kt`:
```kotlin
package com.federico.moneytrack.worker

import android.content.SharedPreferences
import com.federico.moneytrack.domain.repository.BitcoinRepository
import com.federico.moneytrack.util.NotificationHelper
import javax.inject.Inject
import kotlin.math.abs

class BitcoinPriceAlertChecker @Inject constructor(
    private val bitcoinRepository: BitcoinRepository,
    private val prefs: SharedPreferences,
    private val notificationHelper: NotificationHelper
) {
    suspend fun checkAndAlert() {
        val currentPrice = try {
            bitcoinRepository.getBitcoinPrice("eur")
        } catch (e: Exception) {
            return
        }

        if (currentPrice <= 0.0) return

        val baselinePrice = prefs.getFloat(KEY_BASELINE_PRICE, 0f).toDouble()
        val threshold = prefs.getFloat(KEY_ALERT_THRESHOLD, 5f).toDouble()

        if (baselinePrice <= 0.0) {
            prefs.edit().putFloat(KEY_BASELINE_PRICE, currentPrice.toFloat()).apply()
            return
        }

        val changePercent = (currentPrice - baselinePrice) / baselinePrice * 100

        if (abs(changePercent) >= threshold) {
            notificationHelper.showBitcoinAlertNotification(currentPrice, changePercent)
            prefs.edit().putFloat(KEY_BASELINE_PRICE, currentPrice.toFloat()).apply()
        }
    }

    companion object {
        const val KEY_BASELINE_PRICE = "bitcoin_alert_baseline"
        const val KEY_ALERT_THRESHOLD = "bitcoin_alert_threshold"
    }
}
```

- [ ] **Step 4: Ejecutar tests para verificar que pasan**
```bash
./gradlew test --tests "com.federico.moneytrack.worker.BitcoinPriceAlertCheckerTest"
```
Expected: BUILD SUCCESSFUL — 5 tests passed

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/federico/moneytrack/worker/BitcoinPriceAlertChecker.kt \
        app/src/test/java/com/federico/moneytrack/worker/BitcoinPriceAlertCheckerTest.kt
git commit -m "feat: add BitcoinPriceAlertChecker with threshold logic (TDD)"
```

---

### Task 3: BitcoinPriceAlertWorker + schedule en MainActivity

**Files:**
- Create: `app/src/main/java/com/federico/moneytrack/worker/BitcoinPriceAlertWorker.kt`
- Modify: `app/src/main/java/com/federico/moneytrack/ui/MainActivity.kt`

- [ ] **Step 1: Crear BitcoinPriceAlertWorker**

`app/src/main/java/com/federico/moneytrack/worker/BitcoinPriceAlertWorker.kt`:
```kotlin
package com.federico.moneytrack.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BitcoinPriceAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val checker: BitcoinPriceAlertChecker
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        checker.checkAndAlert()
        return Result.success()
    }
}
```

- [ ] **Step 2: Schedulear BitcoinPriceAlertWorker en MainActivity.onCreate()**

Agregar al final de `onCreate()` en `MainActivity.kt`:
```kotlin
WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "bitcoin_price_alert",
    ExistingPeriodicWorkPolicy.KEEP,
    PeriodicWorkRequestBuilder<BitcoinPriceAlertWorker>(1, TimeUnit.HOURS).build()
)
```

Agregar import:
```kotlin
import com.federico.moneytrack.worker.BitcoinPriceAlertWorker
```

(Los demás imports de WorkManager ya deben estar de la rama de budget.)

- [ ] **Step 3: Compilar para verificar**
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/federico/moneytrack/worker/BitcoinPriceAlertWorker.kt \
        app/src/main/java/com/federico/moneytrack/ui/MainActivity.kt
git commit -m "feat: add BitcoinPriceAlertWorker scheduled every hour"
```

---

### Task 4: Agregar umbral de alerta en Settings

**Files:**
- Modify: `app/src/main/res/layout/fragment_settings.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/federico/moneytrack/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/federico/moneytrack/ui/settings/SettingsFragment.kt`

- [ ] **Step 1: Agregar strings**

En `app/src/main/res/values/strings.xml`, agregar antes de `</resources>`:
```xml
<!-- Alerta Bitcoin -->
<string name="settings_bitcoin_alert_title">Alerta de precio Bitcoin</string>
<string name="settings_bitcoin_alert_description">Notificar cuando el precio varíe más de:</string>
<string name="settings_bitcoin_alert_suffix">%</string>
```

- [ ] **Step 2: Agregar UI en fragment_settings.xml**

Localizar `app/src/main/res/layout/fragment_settings.xml` y agregar antes del cierre del layout principal:
```xml
<com.google.android.material.divider.MaterialDivider
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:layout_marginBottom="8dp"/>

<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="@string/settings_bitcoin_alert_title"
    android:textAppearance="?attr/textAppearanceTitleMedium"
    android:textColor="?attr/colorOnSurface"
    android:layout_marginBottom="4dp"/>

<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="@string/settings_bitcoin_alert_description"
    android:textAppearance="?attr/textAppearanceBodyMedium"
    android:textColor="?attr/colorOnSurfaceVariant"/>

<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:layout_marginTop="8dp">

    <com.google.android.material.slider.Slider
        android:id="@+id/sliderBitcoinThreshold"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:valueFrom="1"
        android:valueTo="20"
        android:stepSize="1"
        android:value="5"/>

    <TextView
        android:id="@+id/tvBitcoinThresholdValue"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:minWidth="48dp"
        android:text="5%"
        android:textAppearance="?attr/textAppearanceBodyLarge"/>

</LinearLayout>
```

- [ ] **Step 3: Agregar prefs y métodos en SettingsViewModel**

Si `feature/notifications-reminder` ya fue mergeado, `prefs: SharedPreferences` ya está en el constructor — omitir agregar el parámetro, solo agregar los métodos.

Si NO fue mergeado, agregar `prefs: SharedPreferences` al constructor del ViewModel:
```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val csvBackupManager: CsvBackupManager,
    private val prefs: SharedPreferences
) : ViewModel() {
```

Con import:
```kotlin
import android.content.SharedPreferences
```

En ambos casos, agregar los siguientes métodos al ViewModel:
```kotlin
fun getBitcoinAlertThreshold(): Int =
    prefs.getFloat(BitcoinPriceAlertChecker.KEY_ALERT_THRESHOLD, 5f).toInt()

fun saveBitcoinAlertThreshold(percent: Int) {
    prefs.edit()
        .putFloat(BitcoinPriceAlertChecker.KEY_ALERT_THRESHOLD, percent.toFloat())
        .apply()
}
```

Agregar import:
```kotlin
import com.federico.moneytrack.worker.BitcoinPriceAlertChecker
```

- [ ] **Step 4: Agregar interacción en SettingsFragment**

En `SettingsFragment.kt`, agregar en `onViewCreated()`:
```kotlin
// Umbral de alerta Bitcoin
val currentThreshold = viewModel.getBitcoinAlertThreshold()
binding.sliderBitcoinThreshold.value = currentThreshold.toFloat()
binding.tvBitcoinThresholdValue.text = "$currentThreshold%"

binding.sliderBitcoinThreshold.addOnChangeListener { _, value, fromUser ->
    if (fromUser) {
        val threshold = value.toInt()
        binding.tvBitcoinThresholdValue.text = "$threshold%"
        viewModel.saveBitcoinAlertThreshold(threshold)
    }
}
```

- [ ] **Step 5: Ejecutar todos los tests**
```bash
./gradlew test
```
Expected: BUILD SUCCESSFUL — todos los tests pasan

- [ ] **Step 6: Compilar para verificar**
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**
```bash
git add app/src/main/res/layout/fragment_settings.xml \
        app/src/main/res/values/strings.xml \
        app/src/main/java/com/federico/moneytrack/ui/settings/SettingsViewModel.kt \
        app/src/main/java/com/federico/moneytrack/ui/settings/SettingsFragment.kt
git commit -m "feat: add Bitcoin alert threshold slider in Settings"
```

---

### Task 5: Abrir PR a main

- [ ] **Step 1: Push de la rama**
```bash
git push -u origin feature/notifications-bitcoin
```

- [ ] **Step 2: Crear PR**
```bash
gh pr create --title "feat: add Bitcoin price alert notification" --body "$(cat <<'EOF'
## Summary
- BitcoinPriceAlertChecker: compara precio actual vs baseline, notifica si variación >= umbral (5 tests)
- BitcoinPriceAlertWorker: CoroutineWorker schedulado cada 1 hora
- Settings: slider para configurar umbral de alerta (1-20%)
- Baseline se actualiza automáticamente al enviar una alerta

## Prerequisito
Requiere que feature/notifications-budget esté mergeado en main.

## Test plan
- [ ] `./gradlew test` → todos los tests pasan
- [ ] Settings → mover slider → valor se actualiza
- [ ] Primer uso → baseline se guarda sin notificar
- [ ] Variación > umbral configurado → llega notificación con precio y porcentaje
- [ ] Variación < umbral → sin notificación
EOF
)"
```
