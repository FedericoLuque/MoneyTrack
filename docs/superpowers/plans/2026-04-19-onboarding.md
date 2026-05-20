# Onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mostrar una pantalla de bienvenida de 3 slides (ViewPager2) al primer inicio, guardando un flag en SharedPreferences para no volver a mostrarla.

**Architecture:** `OnboardingActivity` vive fuera del NavGraph principal. `MainActivity.onCreate()` lee el flag `onboarding_completed` de SharedPreferences antes de inflar el layout; si es `false`, redirige a `OnboardingActivity` y se cierra. Al completar el último slide, `OnboardingViewModel` escribe el flag y lanza `MainActivity`.

**Tech Stack:** Kotlin, Hilt (`@HiltViewModel`, `@AndroidEntryPoint`), ViewPager2, TabLayoutMediator, SharedPreferences, MockK

---

## File Map

| Acción | Ruta |
|--------|------|
| Modify | `gradle/libs.versions.toml` |
| Modify | `app/src/main/java/com/federico/moneytrack/di/AppModule.kt` |
| Create | `app/src/main/res/drawable/tab_selector.xml` |
| Create | `app/src/main/res/layout/activity_onboarding.xml` |
| Create | `app/src/main/res/layout/item_onboarding_slide.xml` |
| Modify | `app/src/main/res/values/strings.xml` |
| Create | `app/src/main/java/com/federico/moneytrack/ui/onboarding/OnboardingSlide.kt` |
| Create | `app/src/main/java/com/federico/moneytrack/ui/onboarding/OnboardingViewModel.kt` |
| Create | `app/src/main/java/com/federico/moneytrack/ui/onboarding/OnboardingPagerAdapter.kt` |
| Create | `app/src/main/java/com/federico/moneytrack/ui/onboarding/OnboardingActivity.kt` |
| Modify | `app/src/main/AndroidManifest.xml` |
| Modify | `app/src/main/java/com/federico/moneytrack/ui/MainActivity.kt` |
| Create | `app/src/test/java/com/federico/moneytrack/ui/onboarding/OnboardingViewModelTest.kt` |

---

### Task 1: Crear rama y agregar SharedPreferences al grafo de Hilt

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/src/main/java/com/federico/moneytrack/di/AppModule.kt`

- [ ] **Step 1: Crear rama**
```bash
git checkout -b feature/onboarding
```

- [ ] **Step 2: Agregar ViewPager2 a libs.versions.toml**

En `[versions]` agregar:
```toml
viewpager2 = "1.0.0"
```

En `[libraries]` agregar:
```toml
androidx-viewpager2 = { group = "androidx.viewpager2", name = "viewpager2", version.ref = "viewpager2" }
```

- [ ] **Step 3: Agregar dependencia en app/build.gradle.kts**

En el bloque `dependencies`:
```kotlin
implementation(libs.androidx.viewpager2)
```

- [ ] **Step 4: Agregar proveedor de SharedPreferences en AppModule**

> **Nota:** Si la rama `feature/notifications-budget` ya fue mergeada, este provider ya existe — omitir este paso.

En `AppModule.kt`, agregar al final del objeto (antes del `}`):
```kotlin
@Provides
@Singleton
fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
    context.getSharedPreferences("moneytrack_prefs", Context.MODE_PRIVATE)
```

Agregar import al inicio del archivo:
```kotlin
import android.content.SharedPreferences
```

- [ ] **Step 5: Compilar para verificar**
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**
```bash
git add gradle/libs.versions.toml app/build.gradle.kts \
        app/src/main/java/com/federico/moneytrack/di/AppModule.kt
git commit -m "feat: add ViewPager2 dep and SharedPreferences Hilt provider"
```

---

### Task 2: Layouts y strings del onboarding

**Files:**
- Create: `app/src/main/res/drawable/tab_selector.xml`
- Create: `app/src/main/res/layout/activity_onboarding.xml`
- Create: `app/src/main/res/layout/item_onboarding_slide.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Crear drawable para los puntos de progreso**

`app/src/main/res/drawable/tab_selector.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_selected="true">
        <shape android:shape="oval">
            <solid android:color="?attr/colorPrimary"/>
            <size android:width="10dp" android:height="10dp"/>
        </shape>
    </item>
    <item>
        <shape android:shape="oval">
            <solid android:color="#44000000"/>
            <size android:width="8dp" android:height="8dp"/>
        </shape>
    </item>
</selector>
```

- [ ] **Step 2: Crear activity_onboarding.xml**

`app/src/main/res/layout/activity_onboarding.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?attr/colorSurface">

    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/viewPager"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toTopOf="@id/tabLayout"/>

    <com.google.android.material.tabs.TabLayout
        android:id="@+id/tabLayout"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp"
        app:tabBackground="@drawable/tab_selector"
        app:tabGravity="center"
        app:tabIndicatorHeight="0dp"
        app:tabMinWidth="0dp"
        app:tabPaddingStart="6dp"
        app:tabPaddingEnd="6dp"
        app:layout_constraintBottom_toTopOf="@id/btnNext"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnNext"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="32dp"
        android:layout_marginEnd="32dp"
        android:layout_marginBottom="48dp"
        android:text="@string/onboarding_next"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"/>

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 3: Crear item_onboarding_slide.xml**

`app/src/main/res/layout/item_onboarding_slide.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="32dp">

    <ImageView
        android:id="@+id/ivSlideIcon"
        android:layout_width="128dp"
        android:layout_height="128dp"
        android:layout_marginTop="80dp"
        android:contentDescription="@null"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <TextView
        android:id="@+id/tvSlideTitle"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginTop="32dp"
        android:gravity="center"
        android:textAppearance="?attr/textAppearanceHeadlineMedium"
        android:textColor="?attr/colorOnSurface"
        app:layout_constraintTop_toBottomOf="@id/ivSlideIcon"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <TextView
        android:id="@+id/tvSlideDescription"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:gravity="center"
        android:textAppearance="?attr/textAppearanceBodyLarge"
        android:textColor="?attr/colorOnSurfaceVariant"
        app:layout_constraintTop_toBottomOf="@id/tvSlideTitle"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 4: Agregar strings al archivo de strings existente**

En `app/src/main/res/values/strings.xml`, agregar antes de `</resources>`:
```xml
<!-- Onboarding -->
<string name="onboarding_next">Siguiente</string>
<string name="onboarding_start">Empezar</string>
<string name="onboarding_title_1">Bienvenido a MoneyTrack</string>
<string name="onboarding_desc_1">Tu gestor de finanzas personales. Registrá ingresos, gastos y llevá el control de tus cuentas en un solo lugar.</string>
<string name="onboarding_title_2">Cuentas y transacciones</string>
<string name="onboarding_desc_2">Creá múltiples cuentas (efectivo, banco, ahorro) y registrá cada movimiento con categorías personalizadas.</string>
<string name="onboarding_title_3">Bitcoin y gráficos</string>
<string name="onboarding_desc_3">Seguí tus tenencias en Bitcoin con precio en tiempo real y visualizá tu patrimonio con gráficos claros.</string>
```

- [ ] **Step 5: Commit**
```bash
git add app/src/main/res/
git commit -m "feat: add onboarding layouts, drawable and strings"
```

---

### Task 3: OnboardingViewModel (TDD)

**Files:**
- Create: `app/src/test/java/com/federico/moneytrack/ui/onboarding/OnboardingViewModelTest.kt`
- Create: `app/src/main/java/com/federico/moneytrack/ui/onboarding/OnboardingViewModel.kt`

- [ ] **Step 1: Escribir test que falla**

`app/src/test/java/com/federico/moneytrack/ui/onboarding/OnboardingViewModelTest.kt`:
```kotlin
package com.federico.moneytrack.ui.onboarding

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingViewModelTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        prefs = mockk()
        editor = mockk()
        every { prefs.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } just Runs
    }

    @Test
    fun `isCompleted returns false when flag is not set`() {
        every { prefs.getBoolean(OnboardingViewModel.KEY_ONBOARDING_COMPLETED, false) } returns false
        val vm = OnboardingViewModel(prefs)
        assertFalse(vm.isCompleted)
    }

    @Test
    fun `isCompleted returns true when flag is set`() {
        every { prefs.getBoolean(OnboardingViewModel.KEY_ONBOARDING_COMPLETED, false) } returns true
        val vm = OnboardingViewModel(prefs)
        assertTrue(vm.isCompleted)
    }

    @Test
    fun `markCompleted writes true to SharedPreferences`() {
        every { prefs.getBoolean(any(), any()) } returns false
        val vm = OnboardingViewModel(prefs)
        vm.markCompleted()
        verify { editor.putBoolean(OnboardingViewModel.KEY_ONBOARDING_COMPLETED, true) }
        verify { editor.apply() }
    }
}
```

- [ ] **Step 2: Ejecutar test para verificar que falla**
```bash
./gradlew test --tests "com.federico.moneytrack.ui.onboarding.OnboardingViewModelTest"
```
Expected: FAIL — `OnboardingViewModel` no existe todavía

- [ ] **Step 3: Implementar OnboardingViewModel**

`app/src/main/java/com/federico/moneytrack/ui/onboarding/OnboardingViewModel.kt`:
```kotlin
package com.federico.moneytrack.ui.onboarding

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: SharedPreferences
) : ViewModel() {

    val isCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun markCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }

    companion object {
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
```

- [ ] **Step 4: Ejecutar test para verificar que pasa**
```bash
./gradlew test --tests "com.federico.moneytrack.ui.onboarding.OnboardingViewModelTest"
```
Expected: BUILD SUCCESSFUL — 3 tests passed

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/federico/moneytrack/ui/onboarding/OnboardingViewModel.kt \
        app/src/test/java/com/federico/moneytrack/ui/onboarding/OnboardingViewModelTest.kt
git commit -m "feat: add OnboardingViewModel with SharedPreferences flag (TDD)"
```

---

### Task 4: OnboardingSlide + PagerAdapter + OnboardingActivity

**Files:**
- Create: `app/src/main/java/com/federico/moneytrack/ui/onboarding/OnboardingSlide.kt`
- Create: `app/src/main/java/com/federico/moneytrack/ui/onboarding/OnboardingPagerAdapter.kt`
- Create: `app/src/main/java/com/federico/moneytrack/ui/onboarding/OnboardingActivity.kt`

- [ ] **Step 1: Crear data class OnboardingSlide**

`app/src/main/java/com/federico/moneytrack/ui/onboarding/OnboardingSlide.kt`:
```kotlin
package com.federico.moneytrack.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class OnboardingSlide(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int
)
```

- [ ] **Step 2: Crear OnboardingPagerAdapter**

`app/src/main/java/com/federico/moneytrack/ui/onboarding/OnboardingPagerAdapter.kt`:
```kotlin
package com.federico.moneytrack.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.federico.moneytrack.databinding.ItemOnboardingSlideBinding

class OnboardingPagerAdapter(
    private val slides: List<OnboardingSlide>
) : RecyclerView.Adapter<OnboardingPagerAdapter.SlideViewHolder>() {

    inner class SlideViewHolder(private val binding: ItemOnboardingSlideBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(slide: OnboardingSlide) {
            binding.tvSlideTitle.setText(slide.titleRes)
            binding.tvSlideDescription.setText(slide.descriptionRes)
            binding.ivSlideIcon.setImageResource(slide.iconRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val binding = ItemOnboardingSlideBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SlideViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount(): Int = slides.size
}
```

- [ ] **Step 3: Crear OnboardingActivity**

`app/src/main/java/com/federico/moneytrack/ui/onboarding/OnboardingActivity.kt`:
```kotlin
package com.federico.moneytrack.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.federico.moneytrack.R
import com.federico.moneytrack.databinding.ActivityOnboardingBinding
import com.federico.moneytrack.ui.MainActivity
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel: OnboardingViewModel by viewModels()

    private val slides = listOf(
        OnboardingSlide(R.string.onboarding_title_1, R.string.onboarding_desc_1, R.drawable.ic_launcher_foreground),
        OnboardingSlide(R.string.onboarding_title_2, R.string.onboarding_desc_2, R.drawable.ic_launcher_foreground),
        OnboardingSlide(R.string.onboarding_title_3, R.string.onboarding_desc_3, R.drawable.ic_launcher_foreground)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = OnboardingPagerAdapter(slides)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.btnNext.setText(
                    if (position == slides.lastIndex) R.string.onboarding_start
                    else R.string.onboarding_next
                )
            }
        })

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < slides.lastIndex) {
                binding.viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }
    }

    private fun finishOnboarding() {
        viewModel.markCompleted()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
```

- [ ] **Step 4: Compilar para verificar**
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/federico/moneytrack/ui/onboarding/
git commit -m "feat: add OnboardingActivity with ViewPager2 slides"
```

---

### Task 5: Registro en manifest + check en MainActivity

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/federico/moneytrack/ui/MainActivity.kt`

- [ ] **Step 1: Registrar OnboardingActivity en el manifest**

En `AndroidManifest.xml`, dentro de `<application>`, agregar después de la declaración de `MainActivity`:
```xml
<activity
    android:name=".ui.onboarding.OnboardingActivity"
    android:exported="false" />
```

- [ ] **Step 2: Agregar check de onboarding en MainActivity.onCreate()**

En `MainActivity.kt`, agregar inmediatamente después de `super.onCreate(savedInstanceState)` (antes de `binding = ...`):
```kotlin
val prefs = getSharedPreferences("moneytrack_prefs", MODE_PRIVATE)
if (!prefs.getBoolean(OnboardingViewModel.KEY_ONBOARDING_COMPLETED, false)) {
    startActivity(Intent(this, OnboardingActivity::class.java))
    finish()
    return
}
```

Agregar imports:
```kotlin
import android.content.Intent
import com.federico.moneytrack.ui.onboarding.OnboardingActivity
import com.federico.moneytrack.ui.onboarding.OnboardingViewModel
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
git add app/src/main/AndroidManifest.xml \
        app/src/main/java/com/federico/moneytrack/ui/MainActivity.kt
git commit -m "feat: redirect to onboarding on first launch"
```

---

### Task 6: Abrir PR a main

- [ ] **Step 1: Push de la rama**
```bash
git push -u origin feature/onboarding
```

- [ ] **Step 2: Crear PR**
```bash
gh pr create --title "feat: add onboarding flow (3 slides)" --body "$(cat <<'EOF'
## Summary
- Pantalla de bienvenida de 3 slides usando ViewPager2 + TabLayout (dots)
- Flag `onboarding_completed` en SharedPreferences para mostrar solo al primer inicio
- `OnboardingViewModel` inyectado con Hilt, testado con MockK

## Test plan
- [ ] Fresh install → onboarding aparece automáticamente
- [ ] Tap Siguiente → avanza slides, dot indicador se actualiza
- [ ] Último slide → botón dice "Empezar"
- [ ] Tap Empezar → main app abre, onboarding nunca vuelve a aparecer
- [ ] `./gradlew test` → todos los tests pasan
EOF
)"
```
