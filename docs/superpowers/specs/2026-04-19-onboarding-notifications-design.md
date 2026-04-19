# MoneyTrack — Onboarding + Notificaciones

**Fecha:** 2026-04-19
**Objetivo:** Preparar la app para publicación en Play Store añadiendo onboarding y notificaciones.
**Alcance:** Opción A — sin nuevas funcionalidades financieras, sin cambios a Room schema.

---

## Estructura de ramas

```
main
├── feature/onboarding
├── feature/notifications-budget
├── feature/notifications-reminder
└── feature/notifications-bitcoin
```

Cada rama hace PR independiente a `main`. Orden recomendado: onboarding primero (sin dependencias), luego las tres de notificaciones (pueden ir en paralelo).

---

## feature/onboarding

### Flujo
1. Al iniciar la app, `MoneyTrackApp` o la actividad de entrada lee el flag `onboarding_completed` de `SharedPreferences`.
2. Si es `false` → lanza `OnboardingActivity`.
3. Al pulsar "Empezar" en el último slide → setea el flag a `true` y lanza `MainActivity`.

### Slides
| # | Título | Contenido |
|---|--------|-----------|
| 1 | Bienvenido a MoneyTrack | Logo + tagline de la app |
| 2 | Cuentas y transacciones | Ilustración de registro de gastos/ingresos con categorías |
| 3 | Bitcoin y gráficos | Muestra el panel Bitcoin y los charts de patrimonio |

### Componentes nuevos
- `OnboardingActivity` — Activity independiente, fuera del NavGraph principal
- `OnboardingFragment` — ViewPager2 con TabLayout (puntos de progreso)
- `OnboardingViewModel` — maneja página actual y escritura del flag
- Flag `onboarding_completed` en `SharedPreferences` (inyectado via Hilt)

### Restricciones
- No toca ningún código existente de Dashboard, Room ni use cases.
- No requiere permiso adicional.

---

## feature/notifications-budget

### Descripción
Alerta al usuario cuando el gasto de una categoría supera el 80% o el 100% de su presupuesto mensual.

### Componentes nuevos
- `BudgetAlertWorker` — `CoroutineWorker` con `PeriodicWorkRequest` cada 24hs
- `NotificationHelper` — crea `NotificationChannel` y construye notificaciones (compartido entre las 3 ramas, puede extraerse en un commit base)
- Hilt module `WorkManagerModule` para proveer `WorkManager`

### Lógica del Worker
1. Obtiene presupuestos activos del mes via `BudgetRepository`.
2. Obtiene gastos reales por categoría via `TransactionRepository` (mes actual).
3. Por cada categoría: calcula `gasto / límite * 100`.
4. Si ≥ 80% y < 100% → notificación "Estás al X% de tu presupuesto de [Categoría]".
5. Si ≥ 100% → notificación "Superaste tu presupuesto de [Categoría]".

### Permiso
`POST_NOTIFICATIONS` (Android 13+) solicitado en `MainActivity.onStart()` si no fue concedido.

---

## feature/notifications-reminder

### Descripción
Recordatorio diario configurable para que el usuario no olvide registrar sus gastos.

### Componentes nuevos
- `DailyReminderWorker` — `CoroutineWorker` con `PeriodicWorkRequest` con flexWindow de 15 min alrededor de la hora configurada
- Campo de hora en la pantalla de Settings existente (hora por defecto: 21:00)

### Lógica del Worker
1. Consulta via `TransactionRepository` si existe alguna transacción registrada en el día actual.
2. Si no hay ninguna → envía notificación "¿Ya registraste tus gastos de hoy?".
3. Si ya hay transacciones → no hace nada.

### Configuración
- La hora se guarda en `SharedPreferences` como `HH:mm`.
- Al cambiar la hora en Settings, se cancela el WorkRequest anterior y se reprograma.

---

## feature/notifications-bitcoin

### Descripción
Alerta cuando el precio de Bitcoin varía más de un umbral configurable respecto al último precio conocido.

### Componentes nuevos
- `BitcoinPriceAlertWorker` — `CoroutineWorker` con `PeriodicWorkRequest` cada 1hs
- Campo de umbral (%) en Settings (por defecto: 5%)

### Lógica del Worker
1. Llama a `GetBitcoinValueUseCase` (ya existe, usa CoinGecko API).
2. Lee `BitcoinHolding.lastFiatPrice` de Room como precio de referencia.
3. Calcula variación: `(precioActual - referencia) / referencia * 100`.
4. Si `|variación| >= umbral` → notificación con precio actual y % de cambio (↑ o ↓).
5. Actualiza `lastFiatPrice` en Room.

### Configuración
- Umbral guardado en `SharedPreferences` como `bitcoin_alert_threshold` (Float).

---

## Infraestructura compartida de notificaciones

| Elemento | Detalle |
|----------|---------|
| `NotificationHelper` | Crea canal `CHANNEL_ID = "moneytrack_alerts"`, importancia HIGH. Construye `NotificationCompat.Builder`. |
| `WorkManagerModule` | Hilt `@Module` que provee `WorkManager.getInstance(context)` |
| Permiso | `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` en `AndroidManifest.xml` |
| Solicitud de permiso | `ActivityResultLauncher` en `MainActivity` para Android 13+ |

---

## Testing

- `OnboardingViewModel`: test unitario verificando que el flag se setea al completar.
- `BudgetAlertWorker`: test con `WorkManagerTestInitHelper` y repositorios mockeados (MockK).
- `DailyReminderWorker`: test que verifica que no envía notificación si ya hay transacciones del día.
- `BitcoinPriceAlertWorker`: test que verifica umbral y actualización de precio de referencia.

---

## Dependencias nuevas (app/build.gradle)

```kotlin
// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.0")
implementation("androidx.hilt:hilt-work:1.2.0")
ksp("androidx.hilt:hilt-compiler:1.2.0")

// Testing WorkManager
androidTestImplementation("androidx.work:work-testing:2.9.0")
```
